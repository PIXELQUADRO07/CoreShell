package com.example.coreshell.ui.terminal

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.coreshell.R
import com.example.coreshell.data.repository.SSHRepository
import com.example.coreshell.ssh.CommandResult
import com.example.coreshell.ssh.ShellSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class TerminalViewModel(application: Application, private val sshRepo: SSHRepository) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("coreshell_terminal", Context.MODE_PRIVATE)
    private val historyKey = "terminal_command_history"
    private val maxHistorySize = 200

    private val _terminalOutput = MutableLiveData<String>("")
    val terminalOutput: LiveData<String> = _terminalOutput

    private val _commandState = MutableLiveData<CommandState>(CommandState.Idle)
    val commandState: LiveData<CommandState> = _commandState

    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1
    private val _currentHistoryCommand = MutableLiveData<String>()
    val currentHistoryCommand: LiveData<String> = _currentHistoryCommand

    private var currentDir = "~"
    private val _promptText = MutableLiveData<String>("$currentDir $ ")
    val promptText: LiveData<String> = _promptText

    private val outputBuffer = StringBuilder()
    private var shellSession: ShellSession? = null
    private var shellReaderJob: Job? = null

    init {
        loadHistory()
    }

    fun sendCommand(command: String) {
        if (command.isBlank()) return

        commandHistory.add(command)
        if (commandHistory.size > maxHistorySize) {
            commandHistory.removeFirst()
        }
        persistHistory()
        historyIndex = commandHistory.size
        _commandState.value = CommandState.Running

        appendOutput("$currentDir $ $command\n")

        viewModelScope.launch {
            try {
                if (shellSession?.isOpen == true) {
                    withContext(Dispatchers.IO) {
                        shellSession?.outputStream?.write((command + "\n").toByteArray(StandardCharsets.UTF_8))
                        shellSession?.outputStream?.flush()
                    }
                    _commandState.value = CommandState.Success(CommandResult("", "", 0))
                } else {
                    val result = sshRepo.executeCommand(command)
                    appendOutput(result.output)
                    if (!result.output.endsWith("\n")) appendOutput("\n")

                    if (command.trim().startsWith("cd ")) {
                        updateCurrentDir()
                    }

                    _commandState.value = CommandState.Success(result)
                }
            } catch (e: Exception) {
                appendOutput("[${getApplication<Application>().getString(R.string.error_generic, e.message)}]\n")
                _commandState.value = CommandState.Error(e.message ?: getApplication<Application>().getString(R.string.error_simple))
            }
        }
    }

    private suspend fun updateCurrentDir() {
        try {
            val result = sshRepo.executeCommand("pwd")
            currentDir = result.stdout.trim()
            _promptText.postValue("${currentDir.substringAfterLast("/")} $ ")
        } catch (e: Exception) { /* ignora */ }
    }

    fun openPersistentShell() {
        if (shellSession?.isOpen == true) return

        viewModelScope.launch {
            try {
                val shell = sshRepo.openShell()
                shellSession = shell
                appendOutput("[Shell persistente aperta]\n")

                shellReaderJob?.cancel()
                shellReaderJob = viewModelScope.launch(Dispatchers.IO) {
                    readShellOutput(shell)
                }
            } catch (e: Exception) {
                appendOutput("[Impossibile aprire shell persistente: ${e.message}]\n")
            }
        }
    }

    fun closeShellSession() {
        shellReaderJob?.cancel()
        shellReaderJob = null
        shellSession?.close()
        shellSession = null
    }

    private suspend fun readShellOutput(session: ShellSession) {
        val reader = BufferedReader(InputStreamReader(session.inputStream, StandardCharsets.UTF_8))
        val buffer = CharArray(2048)

        try {
            while (session.isOpen) {
                coroutineContext.ensureActive()
                val count = reader.read(buffer)
                if (count < 0) break
                if (count > 0) {
                    appendOutput(String(buffer, 0, count))
                } else {
                    delay(50)
                }
            }
        } catch (e: Exception) {
            appendOutput("[Shell persistente chiusa: ${e.message}]\n")
        }
    }

    private fun appendOutput(text: String) {
        outputBuffer.append(text)
        if (outputBuffer.length > 50_000) {
            val excess = outputBuffer.length - 50_000
            outputBuffer.delete(0, excess)
        }
        _terminalOutput.postValue(outputBuffer.toString())
    }

    fun clearTerminal() {
        outputBuffer.clear()
        _terminalOutput.value = ""
    }

    private fun persistHistory() {
        val json = JSONArray()
        commandHistory.forEach { json.put(it) }
        prefs.edit().putString(historyKey, json.toString()).apply()
    }

    private fun loadHistory() {
        val historyJson = prefs.getString(historyKey, null) ?: return
        try {
            val json = JSONArray(historyJson)
            for (index in 0 until json.length()) {
                commandHistory.add(json.optString(index, ""))
            }
            historyIndex = commandHistory.size
        } catch (e: Exception) {
            // ignore invalid history data
        }
    }

    fun historyUp() {
        if (commandHistory.isEmpty()) return
        historyIndex = (historyIndex - 1).coerceAtLeast(0)
        _currentHistoryCommand.value = commandHistory[historyIndex]
    }

    fun historyDown() {
        if (historyIndex >= commandHistory.size - 1) {
            historyIndex = commandHistory.size
            _currentHistoryCommand.value = ""
            return
        }
        historyIndex = (historyIndex + 1).coerceAtMost(commandHistory.size - 1)
        _currentHistoryCommand.value = commandHistory[historyIndex]
    }

    override fun onCleared() {
        super.onCleared()
        closeShellSession()
    }
}

sealed class CommandState {
    object Idle : CommandState()
    object Running : CommandState()
    data class Success(val result: CommandResult) : CommandState()
    data class Error(val message: String) : CommandState()
}
