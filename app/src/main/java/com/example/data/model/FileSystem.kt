package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileEntry(
    val name: String,
    val isDirectory: Boolean,
    var size: Long,
    val permissions: String,
    val modifiedTime: String,
    var content: String = ""
)

class SimulatedFileSystem {
    // Path -> List of file entries inside
    private val folders = mutableMapOf<String, MutableList<FileEntry>>()

    init {
        val dateStr = SimpleDateFormat("MMM dd HH:mm", Locale.US).format(Date())

        // Setup base layout directories
        folders["/"] = mutableListOf(
            FileEntry("bin", true, 0, "drwxr-xr-x", dateStr),
            FileEntry("etc", true, 0, "drwxr-xr-x", dateStr),
            FileEntry("home", true, 0, "drwxr-xr-x", dateStr),
            FileEntry("var", true, 0, "drwxr-xr-x", dateStr),
            FileEntry("tmp", true, 0, "drwxrwxrwt", dateStr),
            FileEntry("root", true, 0, "drwx------", dateStr)
        )

        // Setup /bin
        folders["/bin"] = mutableListOf(
            FileEntry("sh", false, 128400, "-rwxr-xr-x", dateStr),
            FileEntry("ls", false, 98200, "-rwxr-xr-x", dateStr),
            FileEntry("cat", false, 65100, "-rwxr-xr-x", dateStr),
            FileEntry("neofetch", false, 4010, "-rwxr-xr-x", dateStr),
            FileEntry("cipher", false, 95120, "-rwxr-xr-x", dateStr)
        )

        // Setup /etc
        folders["/etc"] = mutableListOf(
            FileEntry("motd", false, 480, "-rw-r--r--", dateStr, """
============================================================
              __   _  _  ___   ___  ___ 
             / _` | || || _ \ / _` | _ \
            | (_| | || ||   /| (_| |   /
             \__, | \_,_||_|_\\__,_||_|_|
             |___/ NEW_TOKYO DECK LINK STABLE-4
============================================================
* SYSTEM STATUS: OPERATIONAL
* SECURITY NODE: ACTIVE
* COGNITIVE SHIELDING: ENGAGED
* DIRECT INTERCONNECT ESTABLISHED. WELCOME TO CHIPSIDE.
============================================================
"""),
            FileEntry("resolv.conf", false, 142, "-rw-r--r--", dateStr, "nameserver 127.0.0.1\nnameserver 8.8.8.8\nnameserver 0.0.0.0")
        )

        // Setup /home
        folders["/home"] = mutableListOf(
            FileEntry("deckard", true, 0, "drwxr-xr-x", dateStr)
        )

        // Setup /home/deckard
        folders["/home/deckard"] = mutableListOf(
            FileEntry("cyber_node.cfg", false, 280, "-rw-r--r--", dateStr, """
# CYBERNETIC PROTOCOL CONFIG
NEXUS_LINK=149.201.2.99
ENCRYPTION_MODE=AES_GCM_256
OVERCLOCK_LEVEL=9
QUANTUM_SYNC=on
AI_CORES_ALLOCATED=16
"""),
            FileEntry("corporate_bounty.db", false, 40120, "-rw-r--r--", dateStr, "BINARY_DATA_ENCRYPTED_SIGN_OFF_NEEDED_FOR_READ"),
            FileEntry("intrusion_logs.txt", false, 1140, "-rw-r--r--", dateStr, """
[2026-05-21 04:12:05] ACCESS ATTEMPT FROM 10.0.2.15 BLOCKED
[2026-05-21 05:22:11] FIREWALL TRIGGERED ON IP: 192.168.10.22 - COMPROMISED
[2026-05-21 11:43:00] DECK SIGNATURE OVERRIDE DETECTED - ACCEPTED BY USER
[2026-05-21 15:02:12] TELEMETRY EXPORT INITIATED... SUCCESS
"""),
            FileEntry("netrunner.sh", false, 310, "-rwxr-xr-x", dateStr, """
#!/bin/bash
echo "Initializing uplink..."
cipher --decrypt /home/deckard/corporate_bounty.db || echo "ERR: DECRYPTION REJECTED"
echo "Done."
""")
        )

        // Setup /var
        folders["/var"] = mutableListOf(
            FileEntry("log", true, 0, "drwxr-xr-x", dateStr),
            FileEntry("run", true, 0, "drwxr-xr-x", dateStr)
        )

        folders["/var/log"] = mutableListOf(
            FileEntry("syslog", false, 5602, "-rw-r--r--", dateStr, "systemd v249-14-cyber initialized\nkernel: ACPI: CyberDeck Platform v1.2 detected\nkernel: CPU0: 32 cores online\nkernel: GPU0: Holographic Synthesizer active\n"),
            FileEntry("auth.log", false, 2030, "-rw-r--r--", dateStr, "May 21 12:00:22 sshd[1093]: Server listening on port 22\nMay 21 14:02:00 sshd[22045]: Connection closed by authenticating user root\n")
        )

        // Empty directories
        folders["/var/run"] = mutableListOf()
        folders["/tmp"] = mutableListOf()
        folders["/root"] = mutableListOf()
    }

    fun cleanPath(path: String): String {
        val parts = path.split("/").filter { it.isNotEmpty() && it != "." }
        val resolved = mutableListOf<String>()
        for (part in parts) {
            if (part == "..") {
                if (resolved.isNotEmpty()) resolved.removeAt(resolved.size - 1)
            } else {
                resolved.add(part)
            }
        }
        return "/" + resolved.joinToString("/")
    }

    fun resolvePath(currentDir: String, relativePath: String): String {
        return if (relativePath.startsWith("/")) {
            cleanPath(relativePath)
        } else {
            cleanPath((if (currentDir.endsWith("/")) currentDir else "$currentDir/") + relativePath)
        }
    }

    fun listDirectory(absolutePath: String): List<FileEntry>? {
        val path = cleanPath(absolutePath)
        return folders[path]
    }

    fun readFile(absolutePath: String): String? {
        val parentPath = getParentDirectory(absolutePath)
        val files = folders[parentPath] ?: return null
        val fileName = getFileName(absolutePath)
        val file = files.find { it.name == fileName && !it.isDirectory }
        return file?.content
    }

    fun writeFile(absolutePath: String, content: String): Boolean {
        val parentPath = getParentDirectory(absolutePath)
        val files = folders[parentPath] ?: return false
        val fileName = getFileName(absolutePath)
        val file = files.find { it.name == fileName && !it.isDirectory }

        val dateStr = SimpleDateFormat("MMM dd HH:mm", Locale.US).format(Date())
        if (file != null) {
            file.content = content
            file.size = content.length.toLong()
        } else {
            files.add(FileEntry(fileName, false, content.length.toLong(), "-rw-r--r--", dateStr, content))
        }
        return true
    }

    fun deleteFile(absolutePath: String): Boolean {
        val parentPath = getParentDirectory(absolutePath)
        val files = folders[parentPath] ?: return false
        val fileName = getFileName(absolutePath)
        val toRemove = files.find { it.name == fileName } ?: return false
        
        if (toRemove.isDirectory) {
            val childPath = cleanPath((if (parentPath.endsWith("/")) parentPath else "$parentPath/") + fileName)
            folders.remove(childPath)
        }
        files.remove(toRemove)
        return true
    }

    fun createDirectory(absolutePath: String): Boolean {
        val parentPath = getParentDirectory(absolutePath)
        val files = folders[parentPath] ?: return false
        val dirName = getFileName(absolutePath)

        if (files.any { it.name == dirName }) return false // Already exists

        val dateStr = SimpleDateFormat("MMM dd HH:mm", Locale.US).format(Date())
        files.add(FileEntry(dirName, true, 0, "drwxr-xr-x", dateStr))

        val fullDirPath = cleanPath((if (parentPath.endsWith("/")) parentPath else "$parentPath/") + dirName)
        folders[fullDirPath] = mutableListOf()
        return true
    }

    private fun getParentDirectory(absolutePath: String): String {
        val target = cleanPath(absolutePath)
        if (target == "/") return "/"
        val lastSlash = target.lastIndexOf('/')
        return if (lastSlash == 0) "/" else target.substring(0, lastSlash)
    }

    private fun getFileName(absolutePath: String): String {
        val target = cleanPath(absolutePath)
        if (target == "/") return ""
        val lastSlash = target.lastIndexOf('/')
        return target.substring(lastSlash + 1)
    }
}
