package com.indiza.cotisation.invitations

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.NeonCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinInvitationScreen(
    inviteCode: String,
    viewModel: JoinInvitationViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(inviteCode, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadInvitation(inviteCode)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.joinSuccess) {
        if (state.joinSuccess) {
            onSuccess()
        }
    }

    CosmicBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Invitation") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = NeonCyan)
                } else if (state.error != null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.error!!, color = Color.Red, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        if (!state.isAuthenticated) {
                            Button(onClick = onNavigateToAuth) { Text("SE CONNECTER") }
                            Spacer(Modifier.height(8.dp))
                        }
                        Button(onClick = onBack) { Text("RETOUR") }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(80.dp), tint = NeonCyan)
                        Spacer(Modifier.height(24.dp))
                        Text("VOUS ÊTES INVITÉ", style = MaterialTheme.typography.labelLarge, color = NeonCyan, letterSpacing = 2.sp)
                        Text(
                            state.tontineName ?: "NOM TONTINE",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        state.inviterName?.let { inviter ->
                            Text(
                                "Invité par $inviter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }

                        Spacer(Modifier.height(48.dp))

                        Button(
                            onClick = { 
                                if (!state.isAuthenticated) onNavigateToAuth()
                                else viewModel.join() 
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                        ) {
                            if (state.isJoining) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                            } else {
                                Text(if (state.isAuthenticated) "REJOINDRE LA TONTINE" else "SE CONNECTER POUR REJOINDRE", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
