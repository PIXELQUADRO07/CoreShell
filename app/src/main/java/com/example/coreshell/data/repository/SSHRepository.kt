package com.example.coreshell.data.repository

import com.example.coreshell.data.model.ServerEntity
import com.example.coreshell.ssh.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.InputStream
import java.io.OutputStream

class SSHRepository {

    val sshManager = SSHManager()

    val isConnected get() = sshManager.isConnected

    fun setKnownHostsFile(path: String) {
        sshManager.setKnownHosts(path)
    }

    var hostKeyCallback: ((String) -> Boolean)?
        get() = sshManager.hostKeyCallback
        set(value) { sshManager.hostKeyCallback = value }

    suspend fun connect(server: ServerEntity) {
        when (server.authType) {
            "key" -> sshManager.connectWithKey(
                host = server.host,
                port = server.port,
                username = server.username,
                privateKeyPath = server.privateKeyPath!!,
                passphrase = server.password.ifBlank { null }
            )
            else -> sshManager.connect(
                host = server.host,
                port = server.port,
                username = server.username,
                password = server.password
            )
        }
    }

    suspend fun disconnect() = sshManager.disconnect()

    suspend fun executeCommand(command: String): CommandResult =
        sshManager.executeCommand(command)

    suspend fun openShell(): ShellSession = sshManager.openShell()

    suspend fun listDirectory(path: String): List<RemoteFile> =
        sshManager.listDirectory(path)

    suspend fun downloadFile(
        remotePath: String,
        localOutputStream: OutputStream,
        onProgress: ((Long, Long) -> Unit)? = null
    ) = sshManager.downloadFile(remotePath, localOutputStream, onProgress)

    suspend fun uploadFile(
        localInputStream: InputStream,
        remotePath: String,
        fileSize: Long = -1L,
        onProgress: ((Long, Long) -> Unit)? = null
    ) = sshManager.uploadFile(localInputStream, remotePath, fileSize, onProgress)

    suspend fun createDirectory(path: String) = sshManager.createDirectory(path)

    suspend fun deleteFile(path: String) = sshManager.deleteFile(path)

    suspend fun rename(oldPath: String, newPath: String) = sshManager.rename(oldPath, newPath)

    suspend fun getServerInfo(): ServerInfo = sshManager.getServerInfo()

    fun uploadFileAsFlow(
        localInputStream: InputStream,
        remotePath: String,
        fileSize: Long
    ): Flow<TransferProgress> = flow {
        emit(TransferProgress(0, fileSize, TransferState.STARTED))
        // Note: Real progress would need a more complex flow/channel setup
        sshManager.uploadFile(localInputStream, remotePath, fileSize) { uploaded, total ->
        }
        emit(TransferProgress(fileSize, fileSize, TransferState.COMPLETED))
    }
}

data class TransferProgress(
    val transferred: Long,
    val total: Long,
    val state: TransferState
) {
    val percentage: Int get() = if (total > 0) ((transferred.toFloat() / total) * 100).toInt() else 0
}

enum class TransferState { STARTED, IN_PROGRESS, COMPLETED, ERROR }
