package com.example.ui.ssh

import com.example.data.model.ServerProfile
import com.example.data.model.SimulatedFileSystem

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
    var connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    var failureReason: String = "",
    val terminalLines: MutableList<TerminalLine> = mutableListOf(),
    var currentWorkingDirectory: String = "/home/deckard",
    var sftpDirectory: String = "/home/deckard",
    val fileSystem: SimulatedFileSystem = SimulatedFileSystem(),
    val commandHistory: MutableList<String> = mutableListOf(),
    var historyIndex: Int = -1,
    
    // Telemetry stats updated dynamically
    var cpuUsage: Float = 0.12f,
    var ramUsage: Float = 0.34f,
    var diskUsage: Float = 0.55f,
    var netTx: Float = 42.0f, // kB/s
    var netRx: Float = 120.0f, // kB/s
    var cpuTemp: Float = 42.0f // Celsius
) {
    fun addOutput(text: String, type: TerminalLineType = TerminalLineType.REGULAR) {
        val lines = text.split("\n")
        for (line in lines) {
            terminalLines.add(TerminalLine(line, type))
        }
        // Limit history size to 1000 lines for performance
        while (terminalLines.size > 1000) {
            terminalLines.removeAt(0)
        }
    }
}
