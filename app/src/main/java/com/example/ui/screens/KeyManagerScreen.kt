package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RSAKeyPair
import com.example.ui.components.CyberButton
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyManagerScreen(
    keyPairs: List<RSAKeyPair>,
    onGenerateKey: (String) -> Unit,
    onDeleteKey: (RSAKeyPair) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showGenDialog by remember { mutableStateOf(false) }
    var aliasInput by remember { mutableStateOf("") }
    
    var selectedKeyToView by remember { mutableStateOf<RSAKeyPair?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RSA_KEYRING",
                        color = CyberCyan,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "MANAGE REUSABLE RSA HANDSHAKE CREDENTIALS",
                        color = TermMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                CyberButton(
                    text = "CLOSE",
                    onClick = onBack,
                    color = CyberPink
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action: Generate cryptographic key
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(CyberDark)
                    .border(0.5.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = "Key Generator",
                        tint = CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CREATE RSA DECK SIGNATURE",
                            color = TermWhite,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Generate local RSA-2048 key pairs in OpenSSH structure.",
                            color = TermMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                CyberButton(
                    text = "GENERATE",
                    onClick = {
                        aliasInput = "signature-node-" + (keyPairs.size + 1)
                        showGenDialog = true
                    },
                    color = CyberCyan
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Keys list
            if (keyPairs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "No Keys",
                            tint = CyberGrayLight,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO KEYS FOUND ON keyring",
                            color = TermMuted,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Instantiate an encryption node above to allow secure key handshake authentication.",
                            color = TermMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(keyPairs) { keyPair ->
                        KeyCard(
                            keyPair = keyPair,
                            onViewClick = {
                                selectedKeyToView = if (selectedKeyToView?.id == keyPair.id) null else keyPair
                            },
                            onCopyPublic = {
                                clipboardManager.setText(AnnotatedString(keyPair.publicKey))
                                Toast.makeText(context, "Public key appended to clipboard matrix.", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = { onDeleteKey(keyPair) }
                        )
                    }
                }
            }
        }

        // Custom Dialog popup
        if (showGenDialog) {
            AlertDialog(
                onDismissRequest = { showGenDialog = false },
                title = {
                    Text(
                        text = "SPAWN RSA ALIAS CODE",
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Assign a cryptographic tag identifier to the quantum keypair:",
                            color = TermWhite,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = aliasInput,
                            onValueChange = { aliasInput = it },
                            label = { Text("Keypair Alias Tag") },
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CyberGrayLight,
                                focusedLabelColor = CyberCyan
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("key_alias_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (aliasInput.isNotBlank()) {
                                onGenerateKey(aliasInput)
                                showGenDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("GENERATE PAIR", color = CyberBlack, fontFamily = FontFamily.Monospace)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showGenDialog = false },
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

        // Display SSH Public Private keys contents details overlay drawer
        selectedKeyToView?.let { keyPair ->
            ModalBottomSheet(
                onDismissRequest = { selectedKeyToView = null },
                containerColor = CyberDark,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .systemBarsPadding()
                ) {
                    Text(
                        text = "SSH CHIP REGISTER: ${keyPair.alias.uppercase()}",
                        color = CyberCyan,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "AUTHORIZED_KEYS FORMAT (PUBLIC KEY)",
                        color = CyberGreen,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(CyberBlack)
                            .border(0.5.dp, CyberGreen.copy(alpha = 0.4f))
                            .padding(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = keyPair.publicKey,
                                    color = TermGreen,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    CyberButton(
                        text = "COPY PUBLIC KEY",
                        onClick = {
                            clipboardManager.setText(AnnotatedString(keyPair.publicKey))
                            Toast.makeText(context, "Public key copied.", Toast.LENGTH_SHORT).show()
                        },
                        color = CyberGreen,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "PRIVATE KEY COMPARTMENT (RSA PRIVATE KEY)",
                        color = CyberPink,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(CyberBlack)
                            .border(0.5.dp, CyberPink.copy(alpha = 0.4f))
                            .padding(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = keyPair.privateKey,
                                    color = CyberPink.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    CyberButton(
                        text = "COPY PRIVATE KEY",
                        onClick = {
                            clipboardManager.setText(AnnotatedString(keyPair.privateKey))
                            Toast.makeText(context, "Private key copied.", Toast.LENGTH_SHORT).show()
                        },
                        color = CyberPink,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun KeyCard(
    keyPair: RSAKeyPair,
    onViewClick: () -> Unit,
    onCopyPublic: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(CyberDark)
            .border(0.5.dp, CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .clickable { onViewClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = "Key logo",
                tint = CyberCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = keyPair.alias.uppercase(),
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "RSA-2048 | Public hash: ...${keyPair.publicKey.takeLast(24)}",
                    color = TermMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Row {
            IconButton(onClick = onCopyPublic) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy public key",
                    tint = CyberGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Purge key",
                    tint = CyberPink,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
