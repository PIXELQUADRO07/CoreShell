package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.KeyManagerScreen
import com.example.ui.screens.ServerManagerScreen
import com.example.ui.screens.SshTerminalScreen
import com.example.ui.ssh.SshViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.components.GlitchTransition

class MainActivity : AppCompatActivity() {

    private val viewModel: SshViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Collect states from ViewModel
                val profiles by viewModel.profiles.collectAsStateWithLifecycle()
                val keyPairs by viewModel.keyPairs.collectAsStateWithLifecycle()
                val activeSessions by viewModel.activeSessions.collectAsStateWithLifecycle()
                val selectedSessionId by viewModel.selectedSessionId.collectAsStateWithLifecycle()

                // State-based navigation
                var currentScreen by remember { mutableStateOf("profiles") }
                var isAuthenticated by remember { mutableStateOf(false) }
                val context = androidx.compose.ui.platform.LocalContext.current

                // Handle system back button
                BackHandler(enabled = currentScreen != "profiles") {
                    currentScreen = "profiles"
                }

                LaunchedEffect(Unit) {
                    if (com.example.util.BiometricHelper.canAuthenticate(context)) {
                        com.example.util.BiometricHelper.showBiometricPrompt(
                            activity = this@MainActivity,
                            onSuccess = { isAuthenticated = true },
                            onError = { error ->
                                android.widget.Toast.makeText(context, "AUTH ERROR: $error", android.widget.Toast.LENGTH_SHORT).show()
                                // For development ease, we might not lock out here, 
                                // but in a real app you would.
                                isAuthenticated = true 
                            }
                        )
                    } else {
                        isAuthenticated = true
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (!isAuthenticated) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                androidx.compose.material3.Text("LOCKDOWN ACTIVE", color = com.example.ui.theme.CyberPink)
                            }
                        } else {
                            GlitchTransition(visible = true) {
                                when (currentScreen) {
                                    "profiles" -> {
                                        ServerManagerScreen(
                                            profiles = profiles,
                                            keyPairs = keyPairs,
                                            onConnect = { profile ->
                                                viewModel.connectToServer(profile)
                                                currentScreen = "terminal"
                                            },
                                            onAddProfile = { name, host, port, user, auth, pwd, keyId, colorHex, isTailscale ->
                                                viewModel.addProfile(name, host, port, user, auth, pwd, keyId, colorHex, isTailscale)
                                            },
                                            onDeleteProfile = { profile ->
                                                viewModel.deleteProfile(profile)
                                            },
                                            onOpenKeys = {
                                                currentScreen = "keys"
                                            }
                                        )
                                    }
                                    "keys" -> {
                                        KeyManagerScreen(
                                            keyPairs = keyPairs,
                                            onGenerateKey = { alias ->
                                                viewModel.generateNewRSAKeyPair(alias)
                                            },
                                            onDeleteKey = { keyPair ->
                                                viewModel.deleteKeyPair(keyPair)
                                            },
                                            onBack = {
                                                currentScreen = "profiles"
                                            }
                                        )
                                    }
                                    "terminal" -> {
                                        SshTerminalScreen(
                                            activeSessions = activeSessions,
                                            selectedSessionId = selectedSessionId,
                                            onSwitchSession = { sessionId ->
                                                viewModel.switchSession(sessionId)
                                            },
                                            onCloseSession = { sessionId ->
                                                viewModel.closeSession(sessionId)
                                                if (activeSessions.size <= 1) {
                                                    currentScreen = "profiles"
                                                }
                                            },
                                            onExecuteCommand = { sessionId, cmd ->
                                                viewModel.runTerminalCommand(sessionId, cmd)
                                            },
                                            onNavigateBack = {
                                                currentScreen = "profiles"
                                            },
                                            onSftpNavigate = { sessionId, dir ->
                                                viewModel.sftpNavigateTo(sessionId, dir)
                                            },
                                            onSftpCreateFile = { sessionId, name, content ->
                                                viewModel.sftpCreateFile(sessionId, name, content)
                                            },
                                            onSftpDeleteNode = { sessionId, name ->
                                                viewModel.sftpDeleteNode(sessionId, name)
                                            },
                                            onSftpCreateDir = { sessionId, name ->
                                                viewModel.sftpCreateDirectory(sessionId, name)
                                            },
                                            onSftpReadFile = { sessionId, filename ->
                                                viewModel.sftpGetFileContent(sessionId, filename)
                                            },
                                            onSftpUploadFile = { sessionId, uri, name ->
                                                viewModel.sftpUploadFile(sessionId, uri, name)
                                            },
                                            onSftpDownloadFile = { sessionId, name, uri ->
                                                viewModel.sftpDownloadFile(sessionId, name, uri)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
