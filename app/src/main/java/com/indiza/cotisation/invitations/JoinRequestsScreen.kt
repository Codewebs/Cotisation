package com.indiza.cotisation.invitations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.NeonCyan
import com.indiza.cotisation.invitations.JoinRequestResponse

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

    CosmicBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Demandes d'adhésion", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else if (state.requests.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucune demande en attente", color = Color.Gray)
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
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            ) {
                                Text(it, color = Color.Red, modifier = Modifier.padding(12.dp), fontSize = 12.sp)
                            }
                        }
                    }

                    items(state.requests) { request ->
                        RequestItem(
                            request = request,
                            members = state.availableMembers,
                            onApprove = { memberId, update ->
                                viewModel.approve(request.id, memberId, update)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestItem(
    request: JoinRequestResponse,
    members: List<com.indiza.cotisation.tontine.TontineMemberWithUser>,
    onApprove: (Long, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }
    var updateInfo by remember { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(NeonCyan, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Text(request.requester.nom.take(1).uppercase(), fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(request.requester.nom, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Tél: ${request.requester.phone}", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(Modifier.height(16.dp))
            
            Text("Mapper avec un membre existant :", fontSize = 11.sp, color = NeonCyan)
            
            // Sélecteur de membre (Simplifié pour l'exemple, à améliorer avec un vrai dropdown)
            members.filter { it.member.userId == null }.forEach { m ->
                Row(
                    Modifier.fillMaxWidth().clickable { selectedMemberId = m.member.id }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedMemberId == m.member.id, onClick = { selectedMemberId = m.member.id })
                    Text(m.member.nomSaisi, color = Color.White, fontSize = 13.sp)
                }
            }

            if (selectedMemberId != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = updateInfo, onCheckedChange = { updateInfo = it })
                    Text("Mettre à jour les infos (Nom/Tél)", fontSize = 12.sp, color = Color.Gray)
                }

                Button(
                    onClick = { onApprove(selectedMemberId!!, updateInfo) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("VALIDER LE MAPPAGE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
