package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileEntry
import com.example.data.model.ServerProfile
import com.example.ui.components.*
import com.example.ui.ssh.ConnectionState
import com.example.ui.ssh.SshSessionState
import com.example.ui.ssh.TerminalLine
import com.example.ui.ssh.TerminalLineType
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshTerminalScreen(
    activeSessions: List<SshSessionState>,
    selectedSessionId: String?,
    onSwitchSession: (String) -> Unit,
    onCloseSession: (String) -> Unit,
    onExecuteCommand: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    // SFTP callbacks
    onSftpNavigate: (String, String) -> Unit,
    onSftpCreateFile: (String, String, String) -> Unit,
    onSftpDeleteNode: (String, String) -> Unit,
    onSftpCreateDir: (String, String) -> Unit,
    onSftpReadFile: suspend (String, String) -> String,
    onSftpUploadFile: (String, android.net.Uri, String) -> Unit,
    onSftpDownloadFile: (String, String, android.net.Uri) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Local selected sub-tab: "TERMINAL" vs "SFTP_FILES"
    var activeSubTab by remember { mutableStateOf("TERMINAL") }

    // Command text inputs
    var commandInputText by remember { mutableStateOf("") }

    // Active connection state target
    val activeSession = activeSessions.find { it.sessionId == selectedSessionId }

    // Telemetry graphs history cache: map of sessionId -> List of values
    val cpuHistoryMap = remember { mutableStateMapOf<String, MutableList<Float>>() }
    val ramHistoryMap = remember { mutableStateMapOf<String, MutableList<Float>>() }

    // SFTP modal dialog controls
    var showSftpCreateDialog by remember { mutableStateOf(false) }
    var sftpFilenameInput by remember { mutableStateOf("") }
    var sftpContentInput by remember { mutableStateOf("") }
    var isNewDirectoryCreation by remember { mutableStateOf(false) }

    // SFTP editor controls
    var editingFileEntry by remember { mutableStateOf<FileEntry?>(null) }
    var editingFileContent by remember { mutableStateOf("") }

    // Simulated transfer loading state (progress bar)
    var isSimulatingTransfer by remember { mutableStateOf(false) }
    var transferProgress by remember { mutableStateOf(0f) }
    var transferTitle by remember { mutableStateOf("SFTP UPLINK") }

    // File Pickers
    val uploadPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast("/") ?: "uploaded_file"
            activeSession?.let { s -> onSftpUploadFile(s.sessionId, it, fileName) }
        }
    }

    val downloadPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("*/*")
    ) { uri: android.net.Uri? ->
        uri?.let {
            editingFileEntry?.let { entry ->
                activeSession?.let { s -> onSftpDownloadFile(s.sessionId, entry.name, it) }
            }
        }
    }

    // Update oscilloscope graph histories periodically
    LaunchedEffect(activeSession?.cpuUsage, activeSession?.ramUsage) {
        activeSession?.let { s ->
            if (s.connectionState == ConnectionState.CONNECTED) {
                val cpuList = cpuHistoryMap.getOrPut(s.sessionId) { mutableListOf() }
                cpuList.add(s.cpuUsage)
                if (cpuList.size > 15) cpuList.removeAt(0)

                val ramList = ramHistoryMap.getOrPut(s.sessionId) { mutableListOf() }
                ramList.add(s.ramUsage)
                if (ramList.size > 15) ramList.removeAt(0)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // TOP NAVIGATION ROW: Active Sessions Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberDark)
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exit button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Exit to profiles",
                        tint = CyberPink
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Scrollable Session tabs roster
                LazyRow(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(activeSessions) { session ->
                        val isSelected = session.sessionId == selectedSessionId
                        val accentColorHex = session.profile.neonColorHex
                        val sAccentColor = Color(android.graphics.Color.parseColor(accentColorHex))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) CyberGray else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.dp else 0.5.dp,
                                    color = if (isSelected) sAccentColor else CyberGrayLight,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { onSwitchSession(session.sessionId) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status icon indicator
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when (session.connectionState) {
                                            ConnectionState.CONNECTED -> CyberGreen
                                            ConnectionState.FAILED -> CyberPink
                                            else -> CyberCyan
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = session.profile.name.uppercase(),
                                color = if (isSelected) sAccentColor else TermWhite,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close session Tab",
                                tint = TermMuted,
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { onCloseSession(session.sessionId) }
                            )
                        }
                    }

                    // Plus tab to select another server profile
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, CyberCyan, RoundedCornerShape(4.dp))
                                .clickable { onNavigateBack() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add node",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "CONNECT NEW",
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // PRIMARY DASHBOARD VIEW AREA
            if (activeSession == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO SSH SESSIONS DETECTED IN DECK BUFFER",
                        color = TermMuted,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                // Determine accent color
                val currentAccentColor = Color(android.graphics.Color.parseColor(activeSession.profile.neonColorHex))

                // Connection is in progress or failed
                if (activeSession.connectionState != ConnectionState.CONNECTED) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .cyberScanlines()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CyberBorderCard(
                            accentColor = currentAccentColor,
                            modifier = Modifier.fillMaxWidth().wrapContentHeight()
                        ) {
                            Text(
                                text = "ESTABLISHING SSH SECURE UPLINK...",
                                color = CyberCyan,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Visual linear logging matrix outputs
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(CyberBlack)
                                    .border(0.5.dp, CyberCyan.copy(alpha = 0.2f))
                                    .padding(8.dp)
                            ) {
                                items(activeSession.terminalLines) { log ->
                                    val col = when (log.type) {
                                        TerminalLineType.SUCCESS -> CyberGreen
                                        TerminalLineType.ERROR -> CyberPink
                                        TerminalLineType.HIGHLIGHT -> CyberCyan
                                        TerminalLineType.SYSTEM -> TermMuted
                                        else -> TermWhite
                                    }
                                    Text(
                                        text = log.text,
                                        color = col,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (activeSession.connectionState == ConnectionState.FAILED) {
                                Text(
                                    text = "UPLINK DEGRADED: " + activeSession.failureReason,
                                    color = CyberPink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                CyberButton(
                                    text = "RETRY INTERCONNECT LINK",
                                    onClick = { /* ViewModel automatically retries on select/connect screen */ },
                                    color = CyberPink,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        color = CyberCyan,
                                        trackColor = CyberGray,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                    )
                                    Text(
                                        text = activeSession.connectionState.name,
                                        color = CyberCyan,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // STATUS BAR BAR CHART / SEGMENTS: Terminal vs SFTP
                    TabRow(
                        selectedTabIndex = if (activeSubTab == "TERMINAL") 0 else 1,
                        containerColor = CyberDark,
                        contentColor = currentAccentColor,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                color = currentAccentColor,
                                modifier = Modifier.tabIndicatorOffset(tabPositions[if (activeSubTab == "TERMINAL") 0 else 1])
                            )
                        }
                    ) {
                        Tab(
                            selected = activeSubTab == "TERMINAL",
                            onClick = { activeSubTab = "TERMINAL" },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Terminal, contentDescription = "Terminal", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SHELL TERMINAL", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        )
                        Tab(
                            selected = activeSubTab == "SFTP_FILES",
                            onClick = { activeSubTab = "SFTP_FILES" },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FolderShared, contentDescription = "Files", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SFTP FILE EXPLORER", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        )
                    }

                    // MAIN CORE FRAMEWORK CORES
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // SHELL / SFTP WORKSPACE PANEL
                        Column(
                            modifier = Modifier
                                .weight(4f)
                                .fillMaxHeight()
                        ) {
                            if (activeSubTab == "TERMINAL") {
                                // SHELL COMPONENT VIEW
                                val listState = rememberLazyListState()

                                // Auto Scroll to bottom when output arrives
                                LaunchedEffect(activeSession.terminalLines.size) {
                                    if (activeSession.terminalLines.isNotEmpty()) {
                                        listState.animateScrollToItem(activeSession.terminalLines.size - 1)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .cyberScanlines()
                                        .background(CyberBlack.copy(alpha = 0.9f))
                                        .padding(8.dp)
                                ) {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(activeSession.terminalLines) { terminalLine ->
                                            val tColor = when (terminalLine.type) {
                                                TerminalLineType.INPUT_COMMAND -> currentAccentColor
                                                TerminalLineType.HIGHLIGHT -> CyberCyan
                                                TerminalLineType.SUCCESS -> CyberGreen
                                                TerminalLineType.WARNING -> CyberYellow
                                                TerminalLineType.ERROR -> CyberPink
                                                TerminalLineType.TITLE -> CyberPink
                                                TerminalLineType.SYSTEM -> TermMuted
                                                else -> TermWhite
                                            }
                                            Text(
                                                text = terminalLine.text,
                                                color = tColor,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp,
                                                fontWeight = if (terminalLine.type == TerminalLineType.INPUT_COMMAND) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                // Interactive CLI Input panel
                                Column {
                                    // Row with custom shell shortcut helpers to easily write commands
                                    LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(CyberDark)
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val commonSnippets = listOf("ls -la", "htop", "df -h", "free -m", "docker ps", "neofetch")
                                        items(listOf("TAB", "ESC", "UP", "DOWN") + commonSnippets) { shortcut ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(CyberGray)
                                                    .border(0.5.dp, currentAccentColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                                                    .clickable {
                                                        when (shortcut) {
                                                            "TAB" -> { commandInputText += " " }
                                                            "ESC" -> { commandInputText = "" }
                                                            "UP" -> {
                                                                if (activeSession.commandHistory.isNotEmpty()) {
                                                                    commandInputText = activeSession.commandHistory.last()
                                                                }
                                                            }
                                                            "DOWN" -> { commandInputText = "" }
                                                            else -> onExecuteCommand(activeSession.sessionId, shortcut)
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = shortcut,
                                                    color = currentAccentColor,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Real terminal command prompt input field
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(CyberBlack)
                                            .border(1.dp, CyberGrayLight)
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "> ",
                                            color = currentAccentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 14.sp
                                        )

                                        BasicTextField(
                                            value = commandInputText,
                                            onValueChange = { commandInputText = it },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("terminal_command_input"),
                                            textStyle = LocalTextStyle.current.copy(
                                                color = TermWhite,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 14.sp
                                            ),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                            keyboardActions = KeyboardActions(
                                                onSend = {
                                                    if (commandInputText.isNotBlank()) {
                                                        onExecuteCommand(activeSession.sessionId, commandInputText)
                                                        commandInputText = ""
                                                    }
                                                }
                                            )
                                        )

                                        IconButton(
                                            onClick = {
                                                if (commandInputText.isNotBlank()) {
                                                    onExecuteCommand(activeSession.sessionId, commandInputText)
                                                    commandInputText = ""
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = "Send Command",
                                                tint = currentAccentColor
                                            )
                                        }
                                    }
                                }
                            } else {
                                // SFTP PORT ARCHIVAL MANAGER VIEW
                                val folderContents = activeSession.sftpFiles

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                ) {
                                    // Active SFTP Path segment bar selector
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CyberDark)
                                            .border(0.5.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Folder, contentDescription = "Active Folder", tint = CyberCyan, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = activeSession.sftpDirectory,
                                                color = TermWhite,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Move directory upwards
                                        if (activeSession.sftpDirectory != "/") {
                                            Icon(
                                                imageVector = Icons.Default.DriveFolderUpload,
                                                contentDescription = "Go up directory depth",
                                                tint = CyberCyan,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable {
                                                        onSftpNavigate(activeSession.sessionId, "..")
                                                    }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action bar: Upload a file / Spawn interactive shell dir
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CyberButton(
                                            text = "UPLOAD FILE",
                                            onClick = {
                                                uploadPicker.launch("*/*")
                                            },
                                            color = CyberCyan,
                                            modifier = Modifier.weight(1f)
                                        )
                                        CyberButton(
                                            text = "CREATE DIR",
                                            onClick = {
                                                isNewDirectoryCreation = true
                                                sftpFilenameInput = "node-" + (folderContents.size + 1)
                                                showSftpCreateDialog = true
                                            },
                                            color = CyberGreen,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Vertical grids of SFTP data directory items
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        items(folderContents) { entry ->
                                            SFTPNodeCard(
                                                entry = entry,
                                                onOpenNode = {
                                                    if (entry.isDirectory) {
                                                        onSftpNavigate(activeSession.sessionId, entry.name)
                                                    } else {
                                                        // It's a file, fetch contents and simulate downloads details
                                                        coroutineScope.launch {
                                                            isSimulatingTransfer = true
                                                            transferTitle = "SFTP DOWNLOADING..."
                                                            transferProgress = 0f
                                                            while (transferProgress < 1f) {
                                                                delay(120)
                                                                transferProgress += 0.2f
                                                            }
                                                            isSimulatingTransfer = false
                                                            val content = onSftpReadFile(activeSession.sessionId, entry.name)
                                                            editingFileEntry = entry
                                                            editingFileContent = content
                                                        }
                                                    }
                                                },
                                                onDeleteNode = {
                                                    coroutineScope.launch {
                                                        isSimulatingTransfer = true
                                                        transferTitle = "SFTP PURGING PACKET..."
                                                        transferProgress = 0f
                                                        while (transferProgress < 1f) {
                                                            delay(100)
                                                            transferProgress += 0.25f
                                                        }
                                                        isSimulatingTransfer = false
                                                        onSftpDeleteNode(activeSession.sessionId, entry.name)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // SIDE VERTICAL DASHBOARD: REAL-TIME MONITORS GAUGE
                        var isGaugeExpanded by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier
                                .width(if (isGaugeExpanded) 200.dp else 120.dp)
                                .fillMaxHeight()
                                .background(CyberDark)
                                .border(0.5.dp, CyberGrayLight)
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "MONITOR",
                                color = currentAccentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable { isGaugeExpanded = !isGaugeExpanded }
                            )

                            // Animated Oscilloscope Metrics graphs
                            val cpuList = cpuHistoryMap[activeSession.sessionId] ?: emptyList()
                            val ramList = ramHistoryMap[activeSession.sessionId] ?: emptyList()

                            OscilloscopeGraph(
                                metricsHistory = cpuList,
                                color = CyberCyan,
                                title = if (isGaugeExpanded) "CPU COMPUTE LOAD" else "CPU",
                                valueText = "${(activeSession.cpuUsage * 100).toInt()}%"
                            )

                            OscilloscopeGraph(
                                metricsHistory = ramList,
                                color = CyberPink,
                                title = if (isGaugeExpanded) "MEM ALLOCATION" else "RAM",
                                valueText = "${(activeSession.ramUsage * 100).toInt()}%"
                            )

                            // Text numerical secondary metrics
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("SYSTEMS STATS", color = TermMuted, fontSize = 8.sp)
                                MetricValueRow(
                                    label = "CORE TEMP",
                                    value = "${activeSession.cpuTemp.toInt()}°C",
                                    color = if (activeSession.cpuTemp > 65f) CyberPink else CyberGreen
                                )
                                MetricValueRow(
                                    label = "STORAGE",
                                    value = "${(activeSession.diskUsage * 100).toInt()}%",
                                    color = CyberYellow
                                )
                                MetricValueRow(
                                    label = "UPLINK RX",
                                    value = "${activeSession.netRx.toInt()} k/s",
                                    color = CyberCyan
                                )
                                MetricValueRow(
                                    label = "UPLINK TX",
                                    value = "${activeSession.netTx.toInt()} k/s",
                                    color = CyberGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // SFTP File creation/Upload popup Dialog overlay
        if (showSftpCreateDialog) {
            AlertDialog(
                onDismissRequest = { showSftpCreateDialog = false },
                title = {
                    Text(
                        text = if (isNewDirectoryCreation) "SPAWN DIRECTORY NODE" else "UPLOAD FILE NODE",
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = sftpFilenameInput,
                            onValueChange = { sftpFilenameInput = it },
                            label = { Text("Directory / File Name") },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, focusedLabelColor = CyberCyan),
                            modifier = Modifier.fillMaxWidth().testTag("sftp_name_input")
                        )

                        if (!isNewDirectoryCreation) {
                            OutlinedTextField(
                                value = sftpContentInput,
                                onValueChange = { sftpContentInput = it },
                                label = { Text("Stream Contents (text/ASCII)") },
                                minLines = 3,
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, focusedLabelColor = CyberCyan),
                                modifier = Modifier.fillMaxWidth().testTag("sftp_content_input")
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (sftpFilenameInput.isNotBlank()) {
                                if (isNewDirectoryCreation) {
                                    onSftpCreateDir(activeSession?.sessionId ?: "", sftpFilenameInput)
                                } else {
                                    coroutineScope.launch {
                                        isSimulatingTransfer = true
                                        transferTitle = "SFTP UPLOADING DATA..."
                                        transferProgress = 0f
                                        while (transferProgress < 1f) {
                                            delay(120)
                                            transferProgress += 0.2f
                                        }
                                        isSimulatingTransfer = false
                                        onSftpCreateFile(activeSession?.sessionId ?: "", sftpFilenameInput, sftpContentInput)
                                    }
                                }
                                showSftpCreateDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("INJECT", color = CyberBlack, fontFamily = FontFamily.Monospace)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showSftpCreateDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGray),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("ABORT", color = TermWhite, fontFamily = FontFamily.Monospace)
                    }
                },
                containerColor = CyberDark,
                shape = RoundedCornerShape(4.dp)
            )
        }

        // SFTP FILE CONTROLLER MODAL EDITOR VIEW
        editingFileEntry?.let { entry ->
            AlertDialog(
                onDismissRequest = { editingFileEntry = null },
                title = {
                    Text(
                        text = "SFTP DECK READER: ${entry.name.uppercase()}",
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Edit live secure host files:", color = TermMuted, fontSize = 11.sp)
                        OutlinedTextField(
                            value = editingFileContent,
                            onValueChange = { editingFileContent = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .testTag("file_content_editor"),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, focusedLabelColor = CyberCyan)
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                downloadPicker.launch(entry.name)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("DOWNLOAD", color = CyberBlack, fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isSimulatingTransfer = true
                                    transferTitle = "SFTP INJECTING UPDATE..."
                                    transferProgress = 0f
                                    while (transferProgress < 1f) {
                                        delay(100)
                                        transferProgress += 0.25f
                                    }
                                    isSimulatingTransfer = false
                                    onSftpCreateFile(activeSession?.sessionId ?: "", entry.name, editingFileContent)
                                    editingFileEntry = null
                                    Toast.makeText(context, "Resource update injected successfully.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("SAVE CHANGES", color = CyberBlack, fontFamily = FontFamily.Monospace)
                        }
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { editingFileEntry = null },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGray),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("CLOSE", color = TermWhite, fontFamily = FontFamily.Monospace)
                    }
                },
                containerColor = CyberDark,
                shape = RoundedCornerShape(4.dp)
            )
        }

        // Simulating packet transfer overlay spinner loader
        AnimatedVisibility(
            visible = isSimulatingTransfer,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberDark.copy(alpha = 0.95f))
                    .border(1.dp, CyberCyan, RoundedCornerShape(8.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = transferTitle,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    CircularProgressIndicator(
                        progress = { transferProgress },
                        color = CyberCyan,
                        trackColor = CyberGray,
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "${(transferProgress * 100).toInt()}% DATA LINK SYNC",
                        color = TermMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun MetricValueRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TermMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = color, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SFTPNodeCard(
    entry: FileEntry,
    onOpenNode: () -> Unit,
    onDeleteNode: () -> Unit
) {
    val termColor = if (entry.isDirectory) CyberCyan else CyberGreen

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(CyberDark)
            .border(0.5.dp, termColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .clickable { onOpenNode() }
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = if (entry.isDirectory) "Dir" else "File",
                tint = termColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = entry.name,
                    color = TermWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = if (entry.isDirectory) "DIR" else formatSize(entry.size),
                    color = TermMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        IconButton(
            onClick = onDeleteNode,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Purge file",
                tint = CyberPink,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
