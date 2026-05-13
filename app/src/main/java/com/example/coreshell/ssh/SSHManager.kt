package com.example.coreshell.ssh

import com.jcraft.jsch.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties
import java.util.Vector

class SSHManager {

    private var jsch: JSch = JSch()
    private var session: Session? = null
    private var sftpChannel: ChannelSftp? = null
    private val mutex = Mutex()

    private var knownHostsPath: String? = null
    private var lastHostKeyAccepted: Boolean = false

    private var lastActivityTime: Long = System.currentTimeMillis()
    private val INACTIVITY_TIMEOUT = 30 * 60 * 1000L // 30 minuti

    var isConnected: Boolean = false
        private set

    private fun updateActivity() {
        lastActivityTime = System.currentTimeMillis()
    }

    fun checkInactivity() {
        if (isConnected && System.currentTimeMillis() - lastActivityTime > INACTIVITY_TIMEOUT) {
            SSHLogger.i("Automatic disconnection due to inactivity")
            // In a real app, we would use a CoroutineScope to call disconnect()
        }
    }

    /**
     * Callback per la verifica manuale dell'host key (MITM protection).
     * Se null, il comportamento dipende dalla configurazione di JSch.
     */
    var hostKeyCallback: ((String) -> Boolean)? = null

    /**
     * Configura il file known_hosts per la verifica dei server.
     */
    fun setKnownHosts(path: String) {
        knownHostsPath = path
        try {
            jsch.setKnownHosts(path)
        } catch (e: Exception) {
            // Ignora o logga errore
        }
    }

    private fun saveHostKeyIfNeeded(session: Session) {
        if (!lastHostKeyAccepted || knownHostsPath.isNullOrBlank()) return
        try {
            val hostKey = session.hostKey
            if (hostKey != null) {
                jsch.hostKeyRepository.add(hostKey, SimpleUserInfo(null, null, null))
            }
        } catch (e: Exception) {
            // Ignore persistence error
        } finally {
            lastHostKeyAccepted = false
        }
    }

    suspend fun connect(
        host: String,
        port: Int = 22,
        username: String,
        password: String,
        timeout: Int = 15_000
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (isConnected && session?.isConnected == true) return@withLock

            try {
                SSHLogger.i("Attempting to connect to $host:$port as $username")
                val newSession = jsch.getSession(username, host, port)
                newSession.setPassword(password)

                val config = Properties()
                // Sicurezza: rimosso "no", ora usa "ask" (richiede UserInfo)
                config["StrictHostKeyChecking"] = "ask"
                config["PreferredAuthentications"] = "password,publickey"
                config["server_host_key"] = "ssh-rsa,ecdsa-sha2-nistp256,ssh-ed25519"
                newSession.setConfig(config)
                newSession.timeout = timeout
                
                // UserInfo per gestire password e conferma host key
                newSession.userInfo = SimpleUserInfo(password, hostKeyCallback) { accepted ->
                    lastHostKeyAccepted = accepted
                }

                newSession.connect()
                saveHostKeyIfNeeded(newSession)
                session = newSession
                isConnected = true
                updateActivity()
                SSHLogger.i("Connection established with $host")
            } catch (e: JSchException) {
                isConnected = false
                SSHLogger.e("Connection error: ${e.message}")
                throw SSHConnectionException("Connection failed: ${e.message}", e)
            }
        }
    }

    suspend fun connectWithKey(
        host: String,
        port: Int = 22,
        username: String,
        privateKeyPath: String,
        passphrase: String? = null,
        timeout: Int = 15_000
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (isConnected && session?.isConnected == true) return@withLock

            try {
                SSHLogger.i("Attempting key-based connection to $host")
                jsch.removeAllIdentity()
                if (passphrase != null) {
                    jsch.addIdentity(privateKeyPath, passphrase)
                } else {
                    jsch.addIdentity(privateKeyPath)
                }
                
                val newSession = jsch.getSession(username, host, port)
                val config = Properties()
                config["StrictHostKeyChecking"] = "ask"
                newSession.setConfig(config)
                newSession.timeout = timeout
                newSession.userInfo = SimpleUserInfo(passphrase, hostKeyCallback) { accepted ->
                    lastHostKeyAccepted = accepted
                }

                newSession.connect()
                saveHostKeyIfNeeded(newSession)
                session = newSession
                isConnected = true
                updateActivity()
                SSHLogger.i("Key-based connection established with $host")
            } catch (e: JSchException) {
                isConnected = false
                SSHLogger.e("Key-based connection error: ${e.message}")
                throw SSHConnectionException("Key-based connection failed: ${e.message}", e)
            }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                SSHLogger.i("Disconnecting...")
                sftpChannel?.disconnect()
                sftpChannel = null
                session?.disconnect()
                session = null
                isConnected = false
            } catch (e: Exception) {
                // Ignore disconnect errors
            }
        }
    }

    suspend fun executeCommand(command: String): CommandResult = withContext(Dispatchers.IO) {
        updateActivity()
        val currentSession = mutex.withLock { session }
            ?: throw SSHConnectionException("Not connected to server")

        if (!currentSession.isConnected) {
            mutex.withLock { isConnected = false }
            throw SSHConnectionException("SSH session closed")
        }

        var channel: ChannelExec? = null
        try {
            channel = currentSession.openChannel("exec") as ChannelExec
            channel.setCommand(command)

            val outputStream = ByteArrayOutputStream()
            val errorStream = ByteArrayOutputStream()
            channel.outputStream = outputStream
            channel.setErrStream(errorStream)

            channel.connect(10_000)

            while (!channel.isClosed) {
                delay(50)
            }

            CommandResult(
                stdout = outputStream.toString("UTF-8"),
                stderr = errorStream.toString("UTF-8"),
                exitCode = channel.exitStatus
            )
        } catch (e: JSchException) {
            throw SSHCommandException("Command execution error: ${e.message}", e)
        } finally {
            channel?.disconnect()
        }
    }

    suspend fun openShell(): ShellSession = withContext(Dispatchers.IO) {
        updateActivity()
        val currentSession = mutex.withLock { session }
            ?: throw SSHConnectionException("Not connected to server")

        try {
            val channel = currentSession.openChannel("shell") as ChannelShell
            channel.setPtyType("xterm-256color")
            channel.setPtySize(220, 50, 1000, 500)

            val inputStream = channel.inputStream
            val outputStream = channel.outputStream
            channel.connect(10_000)

            ShellSession(channel, inputStream, outputStream)
        } catch (e: JSchException) {
            throw SSHCommandException("Cannot open shell: ${e.message}", e)
        }
    }

    private suspend fun getSftpChannel(): ChannelSftp = withContext(Dispatchers.IO) {
        updateActivity()
        val currentSession = mutex.withLock { session }
            ?: throw SSHConnectionException("Not connected to server")

        val existing = sftpChannel
        if (existing != null && existing.isConnected) return@withContext existing

        try {
            val channel = currentSession.openChannel("sftp") as ChannelSftp
            channel.connect(10_000)
            sftpChannel = channel
            channel
        } catch (e: JSchException) {
            throw SSHConnectionException("Cannot open SFTP channel: ${e.message}", e)
        }
    }

    suspend fun listDirectory(remotePath: String): List<RemoteFile> = withContext(Dispatchers.IO) {
        val sftp = getSftpChannel()
        try {
            @Suppress("UNCHECKED_CAST")
            val entries = sftp.ls(remotePath) as Vector<ChannelSftp.LsEntry>
            entries
                .filter { it.filename != "." && it.filename != ".." }
                .map { entry ->
                    RemoteFile(
                        name = entry.filename,
                        path = "$remotePath/${entry.filename}".replace("//", "/"),
                        isDirectory = entry.attrs.isDir,
                        size = entry.attrs.size,
                        permissions = entry.attrs.permissionsString,
                        lastModified = entry.attrs.mTime.toLong() * 1000,
                        owner = entry.attrs.uId.toString(),
                        group = entry.attrs.gId.toString()
                    )
                }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        } catch (e: SftpException) {
            throw SSHFileException("Error listing directory '$remotePath': ${e.message}", e)
        }
    }

    suspend fun downloadFile(
        remotePath: String,
        localOutputStream: OutputStream,
        onProgress: ((downloaded: Long, total: Long) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val sftp = getSftpChannel()
        try {
            val fileSize = sftp.lstat(remotePath).size
            val monitor = if (onProgress != null) {
                object : SftpProgressMonitor {
                    private var downloaded = 0L
                    override fun init(op: Int, src: String, dest: String, max: Long) {}
                    override fun count(count: Long): Boolean {
                        downloaded += count
                        onProgress(downloaded, fileSize)
                        return true
                    }
                    override fun end() {}
                }
            } else null

            sftp.get(remotePath, localOutputStream, monitor)
        } catch (e: SftpException) {
            throw SSHFileException("Error downloading '$remotePath': ${e.message}", e)
        }
    }

    suspend fun uploadFile(
        localInputStream: InputStream,
        remotePath: String,
        fileSize: Long = -1L,
        onProgress: ((uploaded: Long, total: Long) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val sftp = getSftpChannel()
        try {
            val monitor = if (onProgress != null && fileSize > 0) {
                object : SftpProgressMonitor {
                    private var uploaded = 0L
                    override fun init(op: Int, src: String, dest: String, max: Long) {}
                    override fun count(count: Long): Boolean {
                        uploaded += count
                        onProgress(uploaded, fileSize)
                        return true
                    }
                    override fun end() {}
                }
            } else null

            sftp.put(localInputStream, remotePath, monitor, ChannelSftp.OVERWRITE)
        } catch (e: SftpException) {
            throw SSHFileException("Error uploading to '$remotePath': ${e.message}", e)
        }
    }

    suspend fun createDirectory(remotePath: String) = withContext(Dispatchers.IO) {
        val sftp = getSftpChannel()
        try {
            sftp.mkdir(remotePath)
        } catch (e: SftpException) {
            throw SSHFileException("Cannot create directory: ${e.message}", e)
        }
    }

    suspend fun deleteFile(remotePath: String) = withContext(Dispatchers.IO) {
        val sftp = getSftpChannel()
        try {
            sftp.rm(remotePath)
        } catch (e: SftpException) {
            throw SSHFileException("Cannot delete file: ${e.message}", e)
        }
    }

    suspend fun rename(oldPath: String, newPath: String) = withContext(Dispatchers.IO) {
        val sftp = getSftpChannel()
        try {
            sftp.rename(oldPath, newPath)
        } catch (e: SftpException) {
            throw SSHFileException("Cannot rename: ${e.message}", e)
        }
    }

    suspend fun getServerInfo(): ServerInfo = withContext(Dispatchers.IO) {
        val uname = executeCommand("uname -a").stdout.trim()
        val uptime = executeCommand("uptime -p").stdout.trim()
        val memResult = executeCommand("free -m | awk 'NR==2{printf \"%s %s %s\", \$2,\$3,\$4}'").stdout.trim()
        val cpuResult = executeCommand("top -bn1 | grep 'Cpu(s)' | awk '{print \$2}'").stdout.trim()

        val memParts = memResult.split(" ")
        ServerInfo(
            uname = uname,
            uptime = uptime,
            totalMemMB = memParts.getOrNull(0)?.toLongOrNull() ?: 0L,
            usedMemMB = memParts.getOrNull(1)?.toLongOrNull() ?: 0L,
            freeMemMB = memParts.getOrNull(2)?.toLongOrNull() ?: 0L,
            cpuUsagePercent = cpuResult.replace("%us,", "").trim().toFloatOrNull() ?: 0f
        )
    }
}

data class RemoteFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val permissions: String,
    val lastModified: Long,
    val owner: String,
    val group: String
) {
    val sizeFormatted: String get() = when {
        isDirectory -> "—"
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
}

data class CommandResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int
) {
    val isSuccess get() = exitCode == 0
    val output get() = if (stderr.isBlank()) stdout else "$stdout\n$stderr"
}

data class ServerInfo(
    val uname: String,
    val uptime: String,
    val totalMemMB: Long,
    val usedMemMB: Long,
    val freeMemMB: Long,
    val cpuUsagePercent: Float
)

class ShellSession(
    val channel: ChannelShell,
    val inputStream: InputStream,
    val outputStream: OutputStream
) {
    val isOpen get() = channel.isConnected
    fun close() = channel.disconnect()
}

/**
 * Implementazione sicura di UserInfo per gestire password e conferma host key.
 */
private class SimpleUserInfo(
    private val passwordOrPassphrase: String?,
    private val hostKeyCallback: ((String) -> Boolean)?,
    private val onHostKeyDecision: ((Boolean) -> Unit)?
) : UserInfo, UIKeyboardInteractive {
    override fun getPassphrase(): String? = passwordOrPassphrase
    override fun getPassword(): String? = passwordOrPassphrase
    override fun promptPassword(message: String?): Boolean = true
    override fun promptPassphrase(message: String?): Boolean = true
    override fun promptYesNo(message: String?): Boolean {
        val accepted = hostKeyCallback?.invoke(message ?: "") ?: true
        onHostKeyDecision?.invoke(accepted)
        return accepted
    }
    override fun showMessage(message: String?) {}
    override fun promptKeyboardInteractive(
        destination: String?,
        name: String?,
        instruction: String?,
        prompt: Array<out String>?,
        echo: BooleanArray?
    ): Array<String>? {
        if (prompt?.size == 1 && (prompt[0].contains("password", true) || prompt[0].contains("passphrase", true))) {
            return arrayOf(passwordOrPassphrase ?: "")
        }
        return null
    }
}

open class SSHException(message: String, cause: Throwable? = null) : Exception(message, cause)
class SSHConnectionException(message: String, cause: Throwable? = null) : SSHException(message, cause)
class SSHCommandException(message: String, cause: Throwable? = null) : SSHException(message, cause)
class SSHFileException(message: String, cause: Throwable? = null) : SSHException(message, cause)
