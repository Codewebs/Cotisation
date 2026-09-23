package com.indiza.cotisation.caisses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.NeonCyan


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageParticipantsScreen(
    caisseId: Long,
    viewModel: CaisseDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(caisseId) {
        println("DEBUG_MANAGE: Ouverture de l'écran de gestion des participants pour caisse $caisseId")
        println("DEBUG_MANAGE:   - Membres tontine disponibles: ${state.allTontineMembers.size}")
        println("DEBUG_MANAGE:   - Participants actuels: ${state.participants.size}")
    }

    val allSelected = state.allTontineMembers.isNotEmpty() && 
                      state.allTontineMembers.all { m -> state.participants.any { it.member.id == m.member.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gérer les Participants", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    if (state.peutAjouterMembres) {
                        TextButton(onClick = { viewModel.toggleAllParticipants(!allSelected) }) {
                            Text(
                                text = if (allSelected) "TOUT DÉSELECTIONNER" else "TOUT SÉLECTIONNER",
                                color = NeonCyan,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(state.allTontineMembers) { memberWithUser ->
                val memberId = memberWithUser.member.id
                val participant = state.participants.find { it.memberWithUser.member.id == memberId }
                val isChecked = participant != null

                // Nombre de parts touchées (0 par défaut)
                val partsDejaTouchees = state.partsEncaisseesMap[memberId] ?: 0
                val totalPartsActuelles = participant?.nombreParts ?: 1

                ParticipantSelectionRow(
                    name = memberWithUser.member.nomSaisi,
                    phone = memberWithUser.member.phone,
                    isSelected = isChecked,
                    partsCount = totalPartsActuelles,
                    minPartsAllowed = partsDejaTouchees, 
                    canBeRemoved = partsDejaTouchees == 0, 
                    canBeAdded = state.peutAjouterMembres, // Nouvelle contrainte
                    onToggle = { selected ->
                        viewModel.toggleParticipantCaisse(memberId, selected, totalPartsActuelles)
                    },
                    onPartsChange = { newParts ->
                        viewModel.toggleParticipantCaisse(memberId, true, newParts)
                    }
                )
            }
        }
    }
}

@Composable
fun ParticipantSelectionRow(
    name: String,
    phone: String,
    isSelected: Boolean,
    partsCount: Int,
    minPartsAllowed: Int,
    canBeRemoved: Boolean,
    canBeAdded: Boolean,
    onToggle: (Boolean) -> Unit,
    onPartsChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bloquer le décochage si le membre a déjà touché de l'argent
        // Bloquer le cochage si la caisse est verrouillée pour les nouveaux
        val enabled = if (isSelected) canBeRemoved else canBeAdded

        Checkbox(
            checked = isSelected,
            onCheckedChange = onToggle,
            enabled = enabled
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = Color.White)
            if (isSelected && !canBeRemoved) {
                Text("Déjà bénéficiaire : retrait impossible", color = AfroGold, style = MaterialTheme.typography.labelSmall)
            } else if (!isSelected && !canBeAdded) {
                Text("Caisse verrouillée : ajout impossible", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
        }

        if (isSelected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Bloquer le bouton Moins si on atteint le nombre de parts déjà bouffées
                IconButton(
                    onClick = { onPartsChange(partsCount - 1) },
                    enabled = partsCount > minPartsAllowed && partsCount > 1
                ) {
                    Icon(
                        Icons.Default.Remove,
                        null,
                        tint = if (partsCount > minPartsAllowed) NeonCyan else Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$partsCount", color = Color.White, fontWeight = FontWeight.Bold)
                    if (minPartsAllowed > 0) {
                        Text("Reçues: $minPartsAllowed", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }

                IconButton(onClick = { onPartsChange(partsCount + 1) }) {
                    Icon(Icons.Default.Add, null, tint = NeonCyan)
                }
            }
        }
    }
}