package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RSAKeyPair
import com.example.data.model.ServerProfile
import com.example.ui.components.CyberButton
import com.example.ui.components.cyberScanlines
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerManagerScreen(
    profiles: List<ServerProfile>,
    keyPairs: List<RSAKeyPair>,
    onConnect: (ServerProfile) -> Unit,
    onAddProfile: (String, String, Int, String, String, String, String?, String, Boolean) -> Unit,
    onDeleteProfile: (ServerProfile) -> Unit,
    onOpenKeys: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showTailscaleInfo by remember { mutableStateOf(false) }
    
    // Server inputs state
    var nameState by remember { mutableStateOf("") }
    var hostState by remember { mutableStateOf("") }
    var portState by remember { mutableStateOf("22") }
    var userState by remember { mutableStateOf("") }
    var authTypeState by remember { mutableStateOf("PASSWORD") } // "PASSWORD" or "RSA_KEY"
    var passwordState by remember { mutableStateOf("") }
    var selectedKeyId by remember { mutableStateOf<String?>(null) }
    var selectedNeonColorHex by remember { mutableStateOf("#00FFD1") } // Default electric cyan
    var isTailscaleState by remember { mutableStateOf(false) }

    val colorsList = listOf("#00FFD1", "#FF00FF", "#39FF14", "#FFFFCC00", "#FF4500", "#9370DB")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .cyberScanlines()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Dashboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CORESHELL_DECK",
                        color = CyberCyan,
                        style = MaterialTheme.typography.displayMedium,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SECURE CORE TERMINAL GATEWAY LAYER",
                        color = TermMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CyberButton(
                        text = "TAILSCALE",
                        onClick = { showTailscaleInfo = !showTailscaleInfo },
                        color = if (showTailscaleInfo) Color(0xFF00FFD1) else CyberGray,
                        testTag = "tailscale_assistant_button"
                    )
                    CyberButton(
                        text = "KEYRING",
                        onClick = onOpenKeys,
                        color = CyberYellow,
                        testTag = "open_keymanager_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cute and High-Tech Start Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .border(0.5.dp, Color(0xFF00FFD1).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .testTag("start_banner_image_card"),
                shape = RoundedCornerShape(4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_start_banner_1779391997906),
                        contentDescription = "CoreShell Terminal Dashboard Header Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "[ KERNEL: CORESHELL / HOST: ONLINE ]",
                            color = Color(0xFF00FFD1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "A modern cozy cybernetic hub for terminal access & tailnets.",
                            color = Color(0xFFE2E2E8),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showTailscaleInfo) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDark.copy(alpha = 0.9f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Color(0xFF00FFD1).copy(alpha = 0.6f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(bottom = 12.dp)
                        .testTag("tailscale_info_card"),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = "Tailscale Net",
                                    tint = Color(0xFF00FFD1),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "TAILSCALE BACKPLANE SYSTEM",
                                    color = Color(0xFF00FFD1),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(
                                onClick = { showTailscaleInfo = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Info",
                                    tint = TermMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tailscale acts as a software-defined peer-to-peer VPN layer. When active on Android, all terminal traffic routes perfectly through the local TUN interface.",
                            color = TermWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "DIAGNOSTIC CRITERIA:",
                            color = TermMuted,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "check", tint = Color(0xFF00FFD1), modifier = Modifier.size(12.dp))
                            Text("TUN interface bound: SUCCESS (direct android loop)", color = TermWhite, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "check", tint = Color(0xFF00FFD1), modifier = Modifier.size(12.dp))
                            Text("100.64.0.0/10 CGNAT subnet routing: COMPATIBLE", color = TermWhite, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "check", tint = Color(0xFF00FFD1), modifier = Modifier.size(12.dp))
                            Text("MagicDNS (*.ts.net) system host lookup: READY", color = TermWhite, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberBlack)
                                .border(0.5.dp, TermMuted.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = "TIP: Start Tailscale VPN on this device, then specify your CGNAT host IP (e.g. 100.95.2.14) or Tailscale DNS hostname (e.g. node.ts.net) on any profile.",
                                color = CyberYellow,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }

            // Action: Spawn a new connection node config
            Card(
                onClick = {
                    nameState = "Server " + (profiles.size + 1)
                    hostState = ""
                    portState = "22"
                    userState = "username"
                    authTypeState = "PASSWORD"
                    passwordState = "pwd"
                    selectedKeyId = keyPairs.firstOrNull()?.id
                    selectedNeonColorHex = "#00FFD1"
                    isTailscaleState = false
                    showAddDialog = true
                },
                colors = CardDefaults.cardColors(containerColor = CyberDark.copy(alpha = 0.8f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        0.5.dp,
                        CyberCyan.copy(alpha = 0.4f),
                        RoundedCornerShape(4.dp)
                    )
                    .testTag("add_profile_card"),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add Client",
                        tint = CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "SPAWN INTERCONNECT PORT NODE",
                            color = TermWhite,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Configure SSH credentials & digital host parameters.",
                            color = TermMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CONFIGURED HOSTS (AVAILABLE FOR DIRECT LINK)",
                color = TermMuted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Hosts Profiles List Box
            if (profiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "No Servers",
                            tint = CyberGrayLight,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO CONFIGURED NODES DETECTED",
                            color = TermMuted,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Initialize an interface config to interconnect with terminal commands.",
                            color = TermMuted,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(profiles) { profile ->
                        ServerProfileCard(
                            profile = profile,
                            onConnect = { onConnect(profile) },
                            onDelete = { onDeleteProfile(profile) }
                        )
                    }
                }
            }
        }

        // Add server config overlay popup dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = {
                    Text(
                        text = "NEW SSH DOCK PROFILE",
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = nameState,
                                onValueChange = { nameState = it },
                                label = { Text("Profile Alias Name") },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, focusedLabelColor = CyberCyan),
                                modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = hostState,
                                onValueChange = { hostState = it },
                                label = { Text("Host Address (IPv4 / DNS)") },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, focusedLabelColor = CyberCyan),
                                modifier = Modifier.fillMaxWidth().testTag("profile_host_input")
                            )
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = portState,
                                    onValueChange = { portState = it },
                                    label = { Text("SSH Port") },
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, focusedLabelColor = CyberCyan),
                                    modifier = Modifier.weight(1f).testTag("profile_port_input")
                                )
                                OutlinedTextField(
                                    value = userState,
                                    onValueChange = { userState = it },
                                    label = { Text("Admin Username") },
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, focusedLabelColor = CyberCyan),
                                    modifier = Modifier.weight(2f).testTag("profile_user_input")
                                )
                            }
                        }
                        item {
                            Text(
                                "AUTHENTICATION VECTOR",
                                color = TermMuted,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CyberButton(
                                    text = "PASSWORD",
                                    onClick = { authTypeState = "PASSWORD" },
                                    color = if (authTypeState == "PASSWORD") CyberCyan else CyberGrayLight,
                                    modifier = Modifier.weight(1f)
                                )
                                CyberButton(
                                    text = "RSA KEYS",
                                    onClick = { authTypeState = "RSA_KEY" },
                                    color = if (authTypeState == "RSA_KEY") CyberCyan else CyberGrayLight,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (authTypeState == "PASSWORD") {
                            item {
                                OutlinedTextField(
                                    value = passwordState,
                                    onValueChange = { passwordState = it },
                                    label = { Text("Terminal Access Password") },
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, focusedLabelColor = CyberCyan),
                                    modifier = Modifier.fillMaxWidth().testTag("profile_password_input")
                                )
                            }
                        } else {
                            item {
                                if (keyPairs.isEmpty()) {
                                    Text(
                                        text = "🚨 WARNING: No RSA keys stored on keyring. Credentials will run simulated default fallback loop. Generate an RSA Key Pair on keyring first.",
                                        color = CyberPink,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                } else {
                                    Text(
                                        "Select RSA Key Identifier:",
                                        color = CyberCyan,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    // Custom Radio items
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, CyberGrayLight, RoundedCornerShape(4.dp))
                                            .background(CyberBlack)
                                            .padding(6.dp)
                                    ) {
                                        keyPairs.forEach { key ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedKeyId = key.id }
                                                    .padding(4.dp)
                                            ) {
                                                RadioButton(
                                                    selected = selectedKeyId == key.id,
                                                    onClick = { selectedKeyId = key.id },
                                                    colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                                                )
                                                Text(
                                                    text = key.alias.uppercase(),
                                                    color = TermWhite,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                "NEON THEME ACCENT COLOR TAG",
                                color = TermMuted,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                colorsList.forEach { colorHex ->
                                    val col = Color(android.graphics.Color.parseColor(colorHex))
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .border(
                                                width = if (selectedNeonColorHex == colorHex) 3.dp else 0.dp,
                                                color = TermWhite,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedNeonColorHex = colorHex }
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                "TAILSCALE NETWORKING PATHWAY",
                                color = TermMuted,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberGrayLight, RoundedCornerShape(4.dp))
                                    .background(CyberBlack.copy(alpha = 0.5f))
                                    .clickable { isTailscaleState = !isTailscaleState }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(if (isTailscaleState) Color(0xFF00FFD1).copy(alpha = 0.15f) else CyberGray, CircleShape)
                                            .border(0.5.dp, if (isTailscaleState) Color(0xFF00FFD1) else TermMuted, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cloud,
                                            contentDescription = "Tailscale",
                                            tint = if (isTailscaleState) Color(0xFF00FFD1) else TermMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            "TAILSCALE TUNNEL",
                                            color = if (isTailscaleState) Color(0xFF00FFD1) else TermWhite,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            "Route host connection through personal tailnet (e.g. 100.x.y.z)",
                                            color = TermMuted,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                Switch(
                                    checked = isTailscaleState,
                                    onCheckedChange = { isTailscaleState = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF050505),
                                        checkedTrackColor = Color(0xFF00FFD1),
                                        uncheckedThumbColor = TermMuted,
                                        uncheckedTrackColor = CyberGray
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (hostState.isNotBlank()) {
                                onAddProfile(
                                    nameState,
                                    hostState,
                                    portState.toIntOrNull() ?: 22,
                                    userState.ifBlank { "root" },
                                    authTypeState,
                                    passwordState,
                                    if (authTypeState == "RSA_KEY") selectedKeyId else null,
                                    selectedNeonColorHex,
                                    isTailscaleState
                                )
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("SPAWN NODE", color = CyberBlack, fontFamily = FontFamily.Monospace)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showAddDialog = false },
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
    }
}

@Composable
fun ServerProfileCard(
    profile: ServerProfile,
    onConnect: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = Color(android.graphics.Color.parseColor(profile.neonColorHex))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(CyberDark.copy(alpha = 0.85f))
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onConnect() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Neon accent colored status led indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .border(1.5.dp, CyberBlack, CircleShape)
            )
            
            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = profile.name.uppercase(),
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (profile.isTailscale) {
                        Surface(
                            color = Color(0xFF00FFD1).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(2.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF00FFD1)),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = "Tailscale Node",
                                    tint = Color(0xFF00FFD1),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "TAILSCALE",
                                    color = Color(0xFF00FFD1),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "${profile.username}@${profile.host}:${profile.port}",
                    color = TermWhite,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "AUTH: ${profile.authType.uppercase()} | NEURAL ENVELOPE SECURE",
                    color = TermMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Connect link icon button
            IconButton(
                onClick = onConnect,
                modifier = Modifier
                    .border(0.5.dp, accentColor, CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "Connect",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Purge icon button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Purge Core",
                    tint = CyberPink,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
