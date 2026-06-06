package com.example.ui.ssh

import com.example.data.model.FileEntry
import com.example.data.model.ServerProfile

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    HANDSHAKE,
    AUTHENTICATING,
    CONNECTED,
    FAILED
}

enum class TerminalLineType {
    INPUT_COMMAND,
    REGULAR,
    SUCCESS,
    WARNING,
    ERROR,
    SYSTEM,
    HIGHLIGHT,
    TITLE
}

data class TerminalLine(
    val text: String,
    val type: TerminalLineType = TerminalLineType.REGULAR
)

data class SshSessionState(
    val sessionId: String,
    val profile: ServerProfile,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val failureReason: String = "",
    val terminalLines: List<TerminalLine> = emptyList(),
    val currentWorkingDirectory: String = "/",
    val sftpDirectory: String = "/",
    val sftpFiles: List<FileEntry> = emptyList(),
    val commandHistory: List<String> = emptyList(),
    val historyIndex: Int = -1,
    
    // Telemetry stats updated dynamically from real system
    val cpuUsage: Float = 0.0f,
    val ramUsage: Float = 0.0f,
    val diskUsage: Float = 0.0f,
    val netTx: Float = 0.0f, // kB/s
    val netRx: Float = 0.0f, // kB/s
    val cpuTemp: Float = 0.0f // Celsius
) {
    fun addOutput(text: String, type: TerminalLineType = TerminalLineType.REGULAR): SshSessionState {
        // Strip ANSI codes before splitting into lines
        val strippedText = text.replace(Regex("\u001B\\[[;\\d]*[A-Za-z]"), "")
        val newLines = strippedText.split("\n").map { TerminalLine(it, type) }
        val updatedLines = (terminalLines + newLines).takeLast(1000)
        return copy(terminalLines = updatedLines)
    }

    fun withCommand(cmd: String): SshSessionState {
        val updatedHistory = commandHistory + cmd
        return copy(
            commandHistory = updatedHistory,
            historyIndex = updatedHistory.size
        )
    }
}
