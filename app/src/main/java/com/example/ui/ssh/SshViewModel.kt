package com.example.ui.ssh

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.FileEntry
import com.example.data.model.RSAKeyPair
import com.example.data.model.ServerProfile
import com.example.data.repository.SshRepository
import com.example.util.CyberUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class SshViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = SshRepository(db.serverProfileDao(), db.rsaKeyPairDao())

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

    // Telemetry updates loop job
    private var telemetryJob: Job? = null

    init {
        startTelemetryLoop()
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            while (true) {
                delay(2000)
                // Randomly fluctuate server telemetry for each active session to show dynamic widget resources in real-time
                val updated = _activeSessions.value.map { session ->
                    if (session.connectionState == ConnectionState.CONNECTED) {
                        session.copy(
                            cpuUsage = (session.cpuUsage + Random.nextFloat() * 0.1f - 0.05f).coerceIn(0.01f, 0.99f),
                            ramUsage = (session.ramUsage + Random.nextFloat() * 0.04f - 0.02f).coerceIn(0.1f, 0.95f),
                            diskUsage = (session.diskUsage + Random.nextFloat() * 0.001f - 0.0005f).coerceIn(0.1f, 0.99f),
                            netTx = (session.netTx + Random.nextFloat() * 20f - 10f).coerceIn(1.0f, 1500.0f),
                            netRx = (session.netRx + Random.nextFloat() * 100f - 50f).coerceIn(5.0f, 5000.0f),
                            cpuTemp = (session.cpuTemp + Random.nextFloat() * 1.5f - 0.75f).coerceIn(35.0f, 85.0f)
                        )
                    } else {
                        session
                    }
                }
                _activeSessions.value = updated
            }
        }
    }

    /**
     * Connects to a server profile, opening a new session tab.
     */
    fun connectToServer(profile: ServerProfile) {
        viewModelScope.launch {
            val sessionId = java.util.UUID.randomUUID().toString()
            val newSession = SshSessionState(
                sessionId = sessionId,
                profile = profile,
                connectionState = ConnectionState.CONNECTING,
                currentWorkingDirectory = "/home/deckard",
                sftpDirectory = "/home/deckard"
            )

            // Add new session and select it immediately
            val currentList = _activeSessions.value.toMutableList()
            currentList.add(newSession)
            _activeSessions.value = currentList
            _selectedSessionId.value = sessionId

            // Add dynamic log output of the connection handshake sequence
            updateSessionState(sessionId) { s ->
                s.addOutput("CYBERSHELL TERMINAL CLIENT V4.2 - ESTABLISHING INTERCONNECT", TerminalLineType.TITLE)
                s.addOutput("Connecting to secure node ${profile.host}:${profile.port} as [${profile.username}]...", TerminalLineType.SYSTEM)
            }
            delay(800)

            updateSessionState(sessionId) { s ->
                s.connectionState = ConnectionState.HANDSHAKE
                s.addOutput("Initializing transport encryption layer...", TerminalLineType.SYSTEM)
                s.addOutput("Ssh-transport-V2.19 algorithm negotiation: kex: curve25519-sha256@libssh.org, cipher: aes256-gcm@openssh.com", TerminalLineType.HIGHLIGHT)
            }
            delay(900)

            updateSessionState(sessionId) { s ->
                s.connectionState = ConnectionState.AUTHENTICATING
                if (profile.authType == "RSA_KEY") {
                    s.addOutput("Requesting token signature authentication via RSA Key pair...", TerminalLineType.SYSTEM)
                    val keyMsg = if (profile.rsaKeyId != null) "Verified RSA Key Signature (id: ...${profile.rsaKeyId.takeLast(6)})" else "Simulated RSA default agent fallback"
                    s.addOutput("$keyMsg - Access Granted by Cryptographic Clearance.", TerminalLineType.SUCCESS)
                } else {
                    s.addOutput("Requesting password challenge authentication...", TerminalLineType.SYSTEM)
                    s.addOutput("Password exchange verified. Clearance authorized.", TerminalLineType.SUCCESS)
                }
            }
            delay(700)

            updateSessionState(sessionId) { s ->
                s.connectionState = ConnectionState.CONNECTED
                s.addOutput("Allocating pseudo-terminal environment (pty)...", TerminalLineType.SYSTEM)
                s.addOutput("SFTP subsystem version 3 initialized in background frame.", TerminalLineType.HIGHLIGHT)
                s.addOutput("==========================================================", TerminalLineType.SUCCESS)
                s.addOutput("                  N E X U S   O N L I N E                 ", TerminalLineType.HIGHLIGHT)
                s.addOutput("==========================================================", TerminalLineType.SUCCESS)
                val motd = s.fileSystem.readFile("/etc/motd") ?: "WELCOME OPERATOR."
                s.addOutput(motd)
                s.addOutput("\nType 'help' to audit operational diagnostics.", TerminalLineType.HIGHLIGHT)
            }
        }
    }

    fun closeSession(sessionId: String) {
        val currentList = _activeSessions.value.filter { it.sessionId != sessionId }
        _activeSessions.value = currentList
        if (_selectedSessionId.value == sessionId) {
            _selectedSessionId.value = currentList.firstOrNull()?.sessionId
        }
    }

    fun switchSession(sessionId: String) {
        _selectedSessionId.value = sessionId
    }

    private fun updateSessionState(sessionId: String, block: (SshSessionState) -> Unit) {
        val list = _activeSessions.value.map { session ->
            if (session.sessionId == sessionId) {
                // Apply operations to a copy of the session state
                val fresh = session.copy(
                    terminalLines = session.terminalLines.toMutableList(),
                    commandHistory = session.commandHistory.toMutableList()
                )
                block(fresh)
                fresh
            } else {
                session
            }
        }
        _activeSessions.value = list
    }

    /**
     * Executes a CLI terminal command on the selected session.
     */
    fun runTerminalCommand(sessionId: String, rawCmd: String) {
        val cmd = rawCmd.trim()
        if (cmd.isEmpty()) return

        updateSessionState(sessionId) { s ->
            // Echo inputs formatted in cyberpunk style
            s.addOutput("${s.profile.username}@${s.profile.host}:${s.currentWorkingDirectory}$ $cmd", TerminalLineType.INPUT_COMMAND)
            s.commandHistory.add(cmd)
            s.historyIndex = s.commandHistory.size

            val tokens = cmd.split(" ").filter { it.isNotEmpty() }
            if (tokens.isEmpty()) return@updateSessionState

            val mainCmd = tokens[0].lowercase()
            val args = tokens.drop(1)

            when (mainCmd) {
                "help" -> {
                    s.addOutput("=== CYBER TERMINAL INTERFACE ENGINE - SHELL CORE ===", TerminalLineType.TITLE)
                    s.addOutput("ls             - List directories and contents in active node.", TerminalLineType.HIGHLIGHT)
                    s.addOutput("cd <dir>       - Traverse to target operational directory.", TerminalLineType.HIGHLIGHT)
                    s.addOutput("cat <file>     - Print data stream buffer of selected payload.", TerminalLineType.HIGHLIGHT)
                    s.addOutput("echo <txt>     - Outputs string payload. Supports injection via '>' (e.g. echo hello > file.log)", TerminalLineType.HIGHLIGHT)
                    s.addOutput("mkdir <dir>    - Spawn a directory allocation node.", TerminalLineType.HIGHLIGHT)
                    s.addOutput("rm <node>      - Execute file or directory deletion purge.", TerminalLineType.HIGHLIGHT)
                    s.addOutput("neofetch       - Display holographic system specifications overlay.", TerminalLineType.HIGHLIGHT)
                    s.addOutput("ping <host>    - Verify link latency to target host address.", TerminalLineType.HIGHLIGHT)
                    s.addOutput("df             - Audit flash partition volume tables.", TerminalLineType.HIGHLIGHT)
                    s.addOutput("whoami         - View clearances of active session operator.", TerminalLineType.HIGHLIGHT)
                    s.addOutput("htop / top     - Launch animated service controller & core telemetry metrics.", TerminalLineType.HIGHLIGHT)
                    s.addOutput("clear          - Erase local shell scroll buffers.", TerminalLineType.HIGHLIGHT)
                }
                "clear" -> {
                    s.terminalLines.clear()
                    s.addOutput("Console buffer recycled.", TerminalLineType.SYSTEM)
                }
                "ls" -> {
                    val targetPath = if (args.isNotEmpty()) s.fileSystem.resolvePath(s.currentWorkingDirectory, args[0]) else s.currentWorkingDirectory
                    val entries = s.fileSystem.listDirectory(targetPath)
                    if (entries == null) {
                        s.addOutput("ls: cannot access '$targetPath': No such directory", TerminalLineType.ERROR)
                    } else if (entries.isEmpty()) {
                        s.addOutput("(empty directory node)", TerminalLineType.SYSTEM)
                    } else {
                        // Table format standard output
                        for (entry in entries) {
                            val prefix = if (entry.isDirectory) "d" else "-"
                            val sizeStr = if (entry.isDirectory) "    -" else formatSize(entry.size)
                            val typeColor = if (entry.isDirectory) TerminalLineType.SUCCESS else TerminalLineType.REGULAR
                            s.addOutput("${entry.permissions}  deckard  $sizeStr  ${entry.modifiedTime}  ${entry.name}${if (entry.isDirectory) "/" else ""}", typeColor)
                        }
                    }
                }
                "cd" -> {
                    if (args.isEmpty()) {
                        s.currentWorkingDirectory = "/home/deckard"
                    } else {
                        val target = s.fileSystem.resolvePath(s.currentWorkingDirectory, args[0])
                        val entries = s.fileSystem.listDirectory(target)
                        if (entries != null) {
                            s.currentWorkingDirectory = target
                        } else {
                            s.addOutput("cd: can't cd to '${args[0]}': No such file or directory", TerminalLineType.ERROR)
                        }
                    }
                }
                "cat" -> {
                    if (args.isEmpty()) {
                        s.addOutput("cat: missing host object", TerminalLineType.ERROR)
                    } else {
                        val targetFile = s.fileSystem.resolvePath(s.currentWorkingDirectory, args[0])
                        val content = s.fileSystem.readFile(targetFile)
                        if (content != null) {
                            s.addOutput(content, TerminalLineType.REGULAR)
                        } else {
                            s.addOutput("cat: '${args[0]}': No such file or data stream", TerminalLineType.ERROR)
                        }
                    }
                }
                "mkdir" -> {
                    if (args.isEmpty()) {
                        s.addOutput("mkdir: missing operand", TerminalLineType.ERROR)
                    } else {
                        val targetDir = s.fileSystem.resolvePath(s.currentWorkingDirectory, args[0])
                        val success = s.fileSystem.createDirectory(targetDir)
                        if (success) {
                            s.addOutput("Spawned direct node folder directory: ${args[0]}", TerminalLineType.SUCCESS)
                        } else {
                            s.addOutput("mkdir: cannot create directory '${args[0]}': File exists or path invalid", TerminalLineType.ERROR)
                        }
                    }
                }
                "rm" -> {
                    if (args.isEmpty()) {
                        s.addOutput("rm: missing operand", TerminalLineType.ERROR)
                    } else {
                        val target = s.fileSystem.resolvePath(s.currentWorkingDirectory, args[0])
                        val success = s.fileSystem.deleteFile(target)
                        if (success) {
                            s.addOutput("Purged storage block: ${args[0]}", TerminalLineType.WARNING)
                        } else {
                            s.addOutput("rm: cannot remove '${args[0]}': No such file or directory node", TerminalLineType.ERROR)
                        }
                    }
                }
                "echo" -> {
                    if (args.isEmpty()) {
                        s.addOutput("")
                    } else {
                        val redirectIndex = args.indexOf(">")
                        if (redirectIndex != -1 && redirectIndex < args.size - 1) {
                            val textToEcho = args.subList(0, redirectIndex).joinToString(" ")
                            val targetFileName = args[redirectIndex + 1]
                            val targetFilePath = s.fileSystem.resolvePath(s.currentWorkingDirectory, targetFileName)
                            val success = s.fileSystem.writeFile(targetFilePath, textToEcho)
                            if (success) {
                                s.addOutput("Uploaded payload into storage block: $targetFileName", TerminalLineType.SUCCESS)
                            } else {
                                s.addOutput("echo: redirection failed: invalid destination system path", TerminalLineType.ERROR)
                            }
                        } else {
                            s.addOutput(args.joinToString(" "), TerminalLineType.REGULAR)
                        }
                    }
                }
                "neofetch" -> {
                    val lines = """
      /\_/\        OS: CyberShell OS v4.5d-Deck
     / o o \       Kernel: 10.201.21-holographic-sys
    (   "   )      Uptime: 247 days, 14 hours, 9 mins
     \_____/       Host: ${s.profile.name} [${s.profile.host}]
    /       \      Shell: CyberBash Interface core-v2.0
   / |     | \     Terminal: Graphic Frame Layout Tab-Shell-1
  (  |=====|  )    Theme: Cyberpunk Retro 2088
   _||_   _||_     Cores: 32 Threads Quantum Silicon
  (____) (____)    CPU Loading: ${(s.cpuUsage * 100).toInt()}%  |  RAM: ${(s.ramUsage * 100).toInt()}%
                    Disk Space: ${(s.diskUsage * 100).toInt()}%  |  Temp: ${s.cpuTemp.toInt()}°C
                    Net Outflow: ${s.netTx.toInt()} kB/s  |  Net Inflow: ${s.netRx.toInt()} kB/s
                    Auth Clearances: SSH-RSA Key Signature Verified
                    """
                    s.addOutput(lines, TerminalLineType.HIGHLIGHT)
                }
                "ping" -> {
                    if (args.isEmpty()) {
                        s.addOutput("ping: missing host destination", TerminalLineType.ERROR)
                    } else {
                        val dest = args[0]
                        s.addOutput("PING $dest ($dest) 56(84) bytes of cybernet transport packet.", TerminalLineType.SYSTEM)
                        viewModelScope.launch {
                            for (i in 1..4) {
                                delay(400)
                                val responseTime = java.text.DecimalFormat("0.00").format(10 + Random.nextFloat() * 15)
                                updateSessionState(sessionId) { ongoingSession ->
                                    ongoingSession.addOutput("64 bytes from $dest: icmp_seq=$i ttl=64 time=$responseTime ms", TerminalLineType.SUCCESS)
                                }
                            }
                            updateSessionState(sessionId) { ongoingSession ->
                                ongoingSession.addOutput("--- $dest statistics: 4 packets tx, 4 packets rx, 0% loss ---", TerminalLineType.HIGHLIGHT)
                            }
                        }
                    }
                }
                "df" -> {
                    s.addOutput("Filesystem      Size  Used  Avail Use% Mounted on", TerminalLineType.TITLE)
                    s.addOutput("/dev/nvme0n1p1  800G  320G   480G  40% /", TerminalLineType.HIGHLIGHT)
                    s.addOutput("/dev/nvme0n1p2  200G   24G   176G  12% /home/user", TerminalLineType.HIGHLIGHT)
                    s.addOutput("tmpfs            16G   42M    16G   1% /tmp", TerminalLineType.REGULAR)
                    s.addOutput("/dev/quantum0   1.2T  714G   526G  57% /mnt/matrix", TerminalLineType.SUCCESS)
                }
                "whoami" -> {
                    s.addOutput("clearance: deckard_level_5 (sys-admin)", TerminalLineType.SUCCESS)
                    s.addOutput("operator: ${s.profile.username}", TerminalLineType.HIGHLIGHT)
                    s.addOutput("secure port node: ${s.profile.port}", TerminalLineType.SYSTEM)
                }
                "htop" , "top" -> {
                    s.addOutput("=== NEURAL TELEMETRY PROCESS MANAGER (SNAPSHOT) ===", TerminalLineType.TITLE)
                    s.addOutput("PID   USER     PR  NI    VIRT    RES    SHR S  %CPU  %MEM     TIME+ COMMAND", TerminalLineType.HIGHLIGHT)
                    s.addOutput("2001  root     20   0   14.2G  1542M  320M S  24.5   4.8  12:04.22 neonet-matrix", TerminalLineType.SUCCESS)
                    s.addOutput("1042  deckard  20   0    2.5G   128M   42M S  14.2   1.1   0:45.10 cyber-sh-exec", TerminalLineType.SUCCESS)
                    s.addOutput("1002  sshd     20   0    1.1G    98M   12M S   8.4   0.3   2:12.44 sftp-daemon", TerminalLineType.HIGHLIGHT)
                    s.addOutput("3094  deckard  20   0   28.1G  2410M  102M S   4.1   7.5  45:12.01 ai-deck-processor", TerminalLineType.SUCCESS)
                    s.addOutput("  42  system   20   0    410M    11M    5M S   0.1   0.0   0:00.12 cognitive-shield", TerminalLineType.REGULAR)
                    s.addOutput("System Threads Allocation State: [NORMAL]", TerminalLineType.SUCCESS)
                }
                else -> {
                    s.addOutput("-cyber-sh: $mainCmd: command not recognized. Type 'help'.", TerminalLineType.ERROR)
                }
            }
        }
    }

    /**
     * Helper to display file size nicely.
     */
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}K"
            else -> "${bytes / (1024 * 1024)}M"
        }
    }

    // Server profiles CRUD
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

    // Key Management
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

    // SFTP Simulation interactions
    fun sftpNavigateTo(sessionId: String, targetDir: String) {
        updateSessionState(sessionId) { s ->
            val resolved = s.fileSystem.resolvePath(s.sftpDirectory, targetDir)
            val entries = s.fileSystem.listDirectory(resolved)
            if (entries != null) {
                s.sftpDirectory = resolved
            } else {
                s.addOutput("SFTP Error: path not found: $resolved", TerminalLineType.ERROR)
            }
        }
    }

    fun sftpCreateFile(sessionId: String, filename: String, content: String) {
        updateSessionState(sessionId) { s ->
            val target = s.fileSystem.resolvePath(s.sftpDirectory, filename)
            val success = s.fileSystem.writeFile(target, content)
            if (success) {
                s.addOutput("SFTP Upload: packet injected to storage $target (${content.length} Bytes)", TerminalLineType.SUCCESS)
            } else {
                s.addOutput("SFTP Error: could not write file $target", TerminalLineType.ERROR)
            }
        }
    }

    fun sftpDeleteNode(sessionId: String, name: String) {
        updateSessionState(sessionId) { s ->
            val target = s.fileSystem.resolvePath(s.sftpDirectory, name)
            val success = s.fileSystem.deleteFile(target)
            if (success) {
                s.addOutput("SFTP Purge: purged block $target", TerminalLineType.WARNING)
            } else {
                s.addOutput("SFTP Error: could not delete node $target", TerminalLineType.ERROR)
            }
        }
    }

    fun sftpCreateDirectory(sessionId: String, dirname: String) {
        updateSessionState(sessionId) { s ->
            val target = s.fileSystem.resolvePath(s.sftpDirectory, dirname)
            val success = s.fileSystem.createDirectory(target)
            if (success) {
                s.addOutput("SFTP: spawned empty directory container $target", TerminalLineType.SUCCESS)
            } else {
                s.addOutput("SFTP Error: could not spawn directory at $target", TerminalLineType.ERROR)
            }
        }
    }
}
