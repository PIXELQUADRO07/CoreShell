package com.example.coreshell.ui.connection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.coreshell.R
import com.example.coreshell.data.model.ServerEntity
import com.example.coreshell.data.repository.ServerRepository
import com.example.coreshell.data.repository.SSHRepository
import com.example.coreshell.ssh.SSHException
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    private val serverRepo = ServerRepository(application)
    val sshRepo = SSHRepository()

    private val _connectionState = MutableLiveData<ConnectionState>(ConnectionState.Idle)
    val connectionState: LiveData<ConnectionState> = _connectionState

    private val _connectedServer = MutableLiveData<ServerEntity?>()
    val connectedServer: LiveData<ServerEntity?> = _connectedServer

    private val _hostKeyPrompt = MutableLiveData<HostKeyPrompt?>()
    val hostKeyPrompt: LiveData<HostKeyPrompt?> = _hostKeyPrompt

    private var pendingHostKeyPrompt: HostKeyPrompt? = null

    init {
        sshRepo.hostKeyCallback = ::requestHostKeyAcceptance
        val knownHostsFile = File(application.filesDir, "known_hosts")
        if (!knownHostsFile.exists()) {
            knownHostsFile.createNewFile()
        }
        sshRepo.setKnownHostsFile(knownHostsFile.absolutePath)
    }

    fun connect(host: String, port: Int, username: String, password: String, nickname: String = "") {
        val server = ServerEntity(
            nickname = nickname.ifBlank { host },
            host = host,
            port = port,
            username = username,
            password = password
        )
        connect(server)
    }

    fun connect(server: ServerEntity) {
        _connectionState.value = ConnectionState.Connecting
        viewModelScope.launch {
            try {
                sshRepo.connect(server)
                _connectedServer.value = server

                if (server.id > 0) {
                    serverRepo.updateLastConnected(server.id)
                }

                _connectionState.value = ConnectionState.Connected
            } catch (e: SSHException) {
                _connectionState.value = ConnectionState.Error(e.message ?: getApplication<Application>().getString(R.string.error_unknown))
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error(getApplication<Application>().getString(R.string.error_generic, e.message))
            }
        }
    }

    private fun requestHostKeyAcceptance(message: String): Boolean {
        val response = AtomicBoolean(false)
        val latch = CountDownLatch(1)
        val prompt = HostKeyPrompt(message, response, latch)

        pendingHostKeyPrompt = prompt
        _hostKeyPrompt.postValue(prompt)

        return try {
            val accepted = latch.await(2, TimeUnit.MINUTES)
            accepted && response.get()
        } finally {
            pendingHostKeyPrompt = null
            _hostKeyPrompt.postValue(null)
        }
    }

    fun respondToHostKeyPrompt(accepted: Boolean) {
        pendingHostKeyPrompt?.let {
            it.response.set(accepted)
            it.latch.countDown()
        }
    }

    data class HostKeyPrompt(
        val message: String,
        val response: AtomicBoolean,
        val latch: CountDownLatch
    )

    fun disconnect() {
        viewModelScope.launch {
            sshRepo.disconnect()
            _connectionState.value = ConnectionState.Idle
            _connectedServer.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { sshRepo.disconnect() }
    }
}

sealed class ConnectionState {
    object Idle : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
