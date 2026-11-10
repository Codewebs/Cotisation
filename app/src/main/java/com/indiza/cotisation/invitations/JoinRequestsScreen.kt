package com.indiza.cotisation.invitations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.tools.*

val CardBackground = Color(0xCC1A102A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRequestsScreen(
    tontineId: Long,
    onBack: () -> Unit,
    viewModel: JoinRequestsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(tontineId) {
        viewModel.loadRequests(tontineId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepViolet, Color(0xFF0F081D))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Demandes d'adhésion", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.background(GlassWhite, CircleShape).size(40.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AfroOrange)
                }
            } else if (state.requests.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.4f))
                        Spacer(Modifier.height(16.dp))
                        Text("Aucune demande en attente", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        state.error?.let {
                            Surface(
                                color = Color.Red.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                            ) {
                                Text(it, color = Color.Red, modifier = Modifier.padding(12.dp), fontSize = 12.sp)
                            }
                        }
                    }

                    items(state.requests) { request ->
                        RequestCard(
                            request = request,
                            onAccept = { viewModel.approveSimple(request.id) },
                            onReject = { viewModel.reject(request.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestCard(
    request: JoinRequestResponse,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, AfroGold.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.sweepGradient(listOf(AfroOrange, AfroGold)),
                            shape = CircleShape
                        )
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = DeepViolet
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = request.requesterNameOrDefault().take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = AfroGold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = request.requesterNameOrDefault(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    if (request.requester.email.isNotBlank()) {
                        Text(
                            text = "Email : ${request.requester.email}",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        text = "Tél : ${request.requester.phone.ifBlank { "Non renseigné" }}",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    if (request.requester.matricule.isNotBlank()) {
                        Text(
                            text = "Matricule : ${request.requester.matricule}",
                            fontSize = 11.sp,
                            color = NeonCyan
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    val relativeTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                        request.createdAt,
                        System.currentTimeMillis(),
                        android.text.format.DateUtils.MINUTE_IN_MILLIS
                    ).toString()
                    Text(
                        text = "Demandé $relativeTime",
                        fontSize = 11.sp,
                        color = AfroGold.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(40.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("REFUSER", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ACCEPTER", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun JoinRequestResponse.requesterNameOrDefault(): String {
    return if (requester.nom.isNotBlank()) {
        requester.nom
    } else if (requester.email.isNotBlank()) {
        requester.email
    } else {
        "Membre"
    }
}
