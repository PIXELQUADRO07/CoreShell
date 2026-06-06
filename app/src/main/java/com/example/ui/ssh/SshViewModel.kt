package com.example.ui.ssh

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.RSAKeyPair
import com.example.data.model.ServerProfile
import com.example.data.repository.SshRepository
import com.example.util.CyberUtils
import com.jcraft.jsch.Session
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class SshViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = SshRepository(db.serverProfileDao(), db.rsaKeyPairDao())
    private val sshHelper = SshClientHelper()

    val profiles: StateFlow<List<ServerProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val keyPairs: StateFlow<List<RSAKeyPair>> = repository.allKeyPairs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active tabbed sessions
    private val _activeSessions = MutableStateFlow<List<SshSessionState>>(emptyList())
    val activeSessions: StateFlow<List<SshSessionState>> = _activeSessions.asStateFlow()

    // Currently selected session ID
    private val _selectedSessionId = MutableStateFlow<String?>(null)
    val selectedSessionId: StateFlow<String?> = _selectedSessionId.asStateFlow()

    // Real JSch sessions storage
    private val realSessions = mutableMapOf<String, Session>()
    private val shellChannels = mutableMapOf<String, com.jcraft.jsch.ChannelShell>()

    // Telemetry updates loop job
    private var telemetryJob: Job? = null

    init {
        startTelemetryLoop()
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                _activeSessions.value.forEach { sessionState ->
                    if (sessionState.connectionState == ConnectionState.CONNECTED) {
                        val session = realSessions[sessionState.sessionId]
                        if (session != null && session.isConnected) {
                            try {
                                val telemetry = sshHelper.fetchTelemetry(session)
                                updateSessionState(sessionState.sessionId) { s ->
                                    s.copy(
                                        cpuUsage = telemetry.cpuUsage,
                                        ramUsage = telemetry.ramUsage,
                                        diskUsage = telemetry.diskUsage,
                                        netTx = telemetry.netTx,
                                        netRx = telemetry.netRx,
                                        cpuTemp = telemetry.cpuTemp
                                    )
                                }
                            } catch (e: Exception) {
                                // Silent fail for telemetry
                            }
                        }
                    }
                }
            }
        }
    }

    fun connectToServer(profile: ServerProfile) {
        viewModelScope.launch {
            val sessionId = UUID.randomUUID().toString()
            val newSessionState = SshSessionState(
                sessionId = sessionId,
                profile = profile,
                connectionState = ConnectionState.CONNECTING,
                currentWorkingDirectory = "/",
                sftpDirectory = "/"
            )

            _activeSessions.value += newSessionState
            _selectedSessionId.value = sessionId

            updateSessionState(sessionId) { s ->
                s.addOutput("CYBERSHELL TERMINAL - ESTABLISHING REAL UPLINK", TerminalLineType.TITLE)
                    .addOutput("Connecting to ${profile.host}:${profile.port} as [${profile.username}]...", TerminalLineType.SYSTEM)
            }

            try {
                var privateKey: String? = null
                if (profile.authType == "RSA_KEY" && profile.rsaKeyId != null) {
                    val keyPair = repository.getKeyPairById(profile.rsaKeyId)
                    privateKey = keyPair?.privateKey
                }

                updateSessionState(sessionId) { it.copy(connectionState = ConnectionState.HANDSHAKE) }
                val session = sshHelper.connect(profile, privateKey)
                realSessions[sessionId] = session

                // Start Shell Channel
                val shellChannel = sshHelper.openShell(session) { data ->
                    updateSessionState(sessionId) { s ->
                        s.addOutput(data, TerminalLineType.REGULAR)
                    }
                }
                shellChannels[sessionId] = shellChannel

                val files = try { sshHelper.listSftpFiles(session, "/") } catch(e: Exception) { emptyList() }
                updateSessionState(sessionId) { s ->
                    s.copy(connectionState = ConnectionState.CONNECTED, sftpFiles = files)
                        .addOutput("Handshake successful. Link established.", TerminalLineType.SUCCESS)
                        .addOutput("Allocating virtual terminal environment...", TerminalLineType.SYSTEM)
                }

                // Fetch MOTD
                val motd = try { sshHelper.executeCommand(session, "cat /etc/motd") } catch(e: Exception) { "Welcome to ${profile.name}" }
                updateSessionState(sessionId) { s ->
                    s.addOutput(motd, TerminalLineType.HIGHLIGHT)
                        .addOutput("\nREAL SSH SESSION ACTIVE. TYPE COMMANDS BELOW.", TerminalLineType.SUCCESS)
                }

            } catch (e: Exception) {
                updateSessionState(sessionId) { s ->
                    s.copy(connectionState = ConnectionState.FAILED, failureReason = e.message ?: "Unknown Error")
                        .addOutput("CONNECTION FAILED: ${e.message}", TerminalLineType.ERROR)
                }
            }
        }
    }

    fun closeSession(sessionId: String) {
        shellChannels[sessionId]?.disconnect()
        shellChannels.remove(sessionId)
        realSessions[sessionId]?.disconnect()
        realSessions.remove(sessionId)
        val currentList = _activeSessions.value.filter { it.sessionId != sessionId }
        _activeSessions.value = currentList
        if (_selectedSessionId.value == sessionId) {
            _selectedSessionId.value = currentList.firstOrNull()?.sessionId
        }
    }

    fun switchSession(sessionId: String) {
        _selectedSessionId.value = sessionId
    }

    private fun updateSessionState(sessionId: String, block: (SshSessionState) -> SshSessionState) {
        _activeSessions.value = _activeSessions.value.map { session ->
            if (session.sessionId == sessionId) {
                block(session)
            } else {
                session
            }
        }
    }

    fun runTerminalCommand(sessionId: String, rawCmd: String) {
        val cmd = rawCmd.trim()
        if (cmd.isEmpty()) return

        val shellChannel = shellChannels[sessionId]
        if (shellChannel == null || !shellChannel.isConnected) {
            updateSessionState(sessionId) { it.addOutput("Error: Shell disconnected", TerminalLineType.ERROR) }
            return
        }

        viewModelScope.launch {
            try {
                val os = shellChannel.outputStream
                os.write("$cmd\n".toByteArray())
                os.flush()
            } catch (e: Exception) {
                updateSessionState(sessionId) { it.addOutput("Send Error: ${e.message}", TerminalLineType.ERROR) }
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}K"
            else -> "${bytes / (1024 * 1024)}M"
        }
    }

    fun addProfile(name: String, host: String, port: Int, username: String, authType: String, password: String, rsaKeyId: String?, neonColorHex: String, isTailscale: Boolean = false) {
        viewModelScope.launch {
            val autoDetectedTailscale = isTailscale || host.trim().startsWith("100.") || host.trim().endsWith(".ts.net") || host.trim().endsWith(".tailscale")
            val prof = ServerProfile(
                name = name,
                host = host,
                port = port,
                username = username,
                authType = authType,
                password = password,
                rsaKeyId = rsaKeyId,
                neonColorHex = neonColorHex,
                isTailscale = autoDetectedTailscale
            )
            repository.insertProfile(prof)
        }
    }

    fun updateProfile(profile: ServerProfile) {
        viewModelScope.launch {
            repository.updateProfile(profile)
        }
    }

    fun deleteProfile(profile: ServerProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
        }
    }

    fun generateNewRSAKeyPair(alias: String) {
        viewModelScope.launch {
            val keys = CyberUtils.generateRsa2048KeyPair()
            val rsaModel = RSAKeyPair(
                alias = alias,
                publicKey = keys.first,
                privateKey = keys.second
            )
            repository.insertKeyPair(rsaModel)
        }
    }

    fun deleteKeyPair(keyPair: RSAKeyPair) {
        viewModelScope.launch {
            repository.deleteKeyPair(keyPair)
        }
    }

    fun sftpNavigateTo(sessionId: String, targetDir: String) {
        val session = realSessions[sessionId] ?: return
        viewModelScope.launch {
            val currentState = _activeSessions.value.find { it.sessionId == sessionId } ?: return@launch
            val target = if (targetDir == "..") {
                val current = currentState.sftpDirectory
                if (current == "/") "/" else current.substringBeforeLast("/", "")
                    .let { if (it.isEmpty()) "/" else it }
            } else if (targetDir.startsWith("/")) {
                targetDir
            } else {
                (if (currentState.sftpDirectory.endsWith("/")) currentState.sftpDirectory else "${currentState.sftpDirectory}/") + targetDir
            }
            
            try {
                val files = sshHelper.listSftpFiles(session, target)
                updateSessionState(sessionId) { s ->
                    s.copy(sftpDirectory = target, sftpFiles = files)
                }
            } catch (e: Exception) {
                updateSessionState(sessionId) { s ->
                    s.addOutput("SFTP Error: ${e.message}", TerminalLineType.ERROR)
                }
            }
        }
    }

    fun sftpCreateFile(sessionId: String, filename: String, content: String) {
        val session = realSessions[sessionId] ?: return
        viewModelScope.launch {
            val currentState = _activeSessions.value.find { it.sessionId == sessionId } ?: return@launch
            val target = (if (currentState.sftpDirectory.endsWith("/")) currentState.sftpDirectory else "${currentState.sftpDirectory}/") + filename
            try {
                sshHelper.sftpUpload(session, target, content)
                val files = sshHelper.listSftpFiles(session, currentState.sftpDirectory)
                updateSessionState(sessionId) { it.copy(sftpFiles = files).addOutput("SFTP: Uploaded $filename", TerminalLineType.SUCCESS) }
            } catch (e: Exception) {
                updateSessionState(sessionId) { it.addOutput("SFTP Upload Error: ${e.message}", TerminalLineType.ERROR) }
            }
        }
    }

    fun sftpDeleteNode(sessionId: String, name: String) {
        val session = realSessions[sessionId] ?: return
        viewModelScope.launch {
            val currentState = _activeSessions.value.find { it.sessionId == sessionId } ?: return@launch
            val target = (if (currentState.sftpDirectory.endsWith("/")) currentState.sftpDirectory else "${currentState.sftpDirectory}/") + name
            try {
                sshHelper.sftpDelete(session, target)
                val files = sshHelper.listSftpFiles(session, currentState.sftpDirectory)
                updateSessionState(sessionId) { it.copy(sftpFiles = files).addOutput("SFTP: Deleted $name", TerminalLineType.WARNING) }
            } catch (e: Exception) {
                updateSessionState(sessionId) { it.addOutput("SFTP Delete Error: ${e.message}", TerminalLineType.ERROR) }
            }
        }
    }

    fun sftpCreateDirectory(sessionId: String, dirname: String) {
        val session = realSessions[sessionId] ?: return
        viewModelScope.launch {
            val currentState = _activeSessions.value.find { it.sessionId == sessionId } ?: return@launch
            val target = (if (currentState.sftpDirectory.endsWith("/")) currentState.sftpDirectory else "${currentState.sftpDirectory}/") + dirname
            try {
                sshHelper.sftpMkdir(session, target)
                val files = sshHelper.listSftpFiles(session, currentState.sftpDirectory)
                updateSessionState(sessionId) { it.copy(sftpFiles = files).addOutput("SFTP: Created directory $dirname", TerminalLineType.SUCCESS) }
            } catch (e: Exception) {
                updateSessionState(sessionId) { it.addOutput("SFTP Mkdir Error: ${e.message}", TerminalLineType.ERROR) }
            }
        }
    }

    suspend fun sftpGetFileContent(sessionId: String, filename: String): String {
        val session = realSessions[sessionId] ?: return ""
        val currentState = _activeSessions.value.find { it.sessionId == sessionId } ?: return ""
        val target = (if (currentState.sftpDirectory.endsWith("/")) currentState.sftpDirectory else "${currentState.sftpDirectory}/") + filename
        return try {
            sshHelper.sftpDownload(session, target)
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }

    fun sftpUploadFile(sessionId: String, uri: android.net.Uri, filename: String) {
        val session = realSessions[sessionId] ?: return
        viewModelScope.launch {
            val currentState = _activeSessions.value.find { it.sessionId == sessionId } ?: return@launch
            val target = (if (currentState.sftpDirectory.endsWith("/")) currentState.sftpDirectory else "${currentState.sftpDirectory}/") + filename
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    sshHelper.sftpUploadStream(session, target, inputStream)
                    val files = sshHelper.listSftpFiles(session, currentState.sftpDirectory)
                    updateSessionState(sessionId) { it.copy(sftpFiles = files).addOutput("SFTP: Uploaded $filename", TerminalLineType.SUCCESS) }
                }
            } catch (e: Exception) {
                updateSessionState(sessionId) { it.addOutput("SFTP Upload Error: ${e.message}", TerminalLineType.ERROR) }
            }
        }
    }

    fun sftpDownloadFile(sessionId: String, filename: String, uri: android.net.Uri) {
        val session = realSessions[sessionId] ?: return
        viewModelScope.launch {
            val currentState = _activeSessions.value.find { it.sessionId == sessionId } ?: return@launch
            val target = (if (currentState.sftpDirectory.endsWith("/")) currentState.sftpDirectory else "${currentState.sftpDirectory}/") + filename
            try {
                val outputStream = getApplication<Application>().contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    sshHelper.sftpDownloadStream(session, target, outputStream)
                    updateSessionState(sessionId) { it.addOutput("SFTP: Downloaded $filename", TerminalLineType.SUCCESS) }
                }
            } catch (e: Exception) {
                updateSessionState(sessionId) { it.addOutput("SFTP Download Error: ${e.message}", TerminalLineType.ERROR) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        shellChannels.values.forEach { it.disconnect() }
        shellChannels.clear()
        realSessions.values.forEach { it.disconnect() }
        realSessions.clear()
    }
}
