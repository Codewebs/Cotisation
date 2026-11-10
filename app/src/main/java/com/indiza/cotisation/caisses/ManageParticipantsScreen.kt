package com.indiza.cotisation.caisses

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
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
    var initialSelection by remember(caisseId) { mutableStateOf<Map<Long, Int>?>(null) }
    var isSaving by remember(caisseId) { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(caisseId) {
        println("DEBUG_MANAGE: Ouverture de l'écran de gestion des participants pour caisse $caisseId")
        println("DEBUG_MANAGE:   - Membres tontine disponibles: ${state.allTontineMembers.size}")
        println("DEBUG_MANAGE:   - Participants actuels: ${state.participants.size}")
    }

    LaunchedEffect(state.caisse?.id, state.isLoading, state.participants) {
        if (state.caisse?.id == caisseId && !state.isLoading && initialSelection == null) {
            initialSelection = state.participants.associate {
                it.member.id to it.nombreParts
            }
        }
    }

    val selectedParts = initialSelection ?: state.participants.associate {
        it.member.id to it.nombreParts
    }
    val allSelected = state.allTontineMembers.isNotEmpty() && 
                      state.allTontineMembers.all { m -> m.member.id in selectedParts }

    fun saveAndExit() {
        if (isSaving) return
        val selection = initialSelection ?: return onBack()
        coroutineScope.launch {
            isSaving = true
            try {
                viewModel.saveCaisseParticipantSelection(selection)
                onBack()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    e.message ?: "Impossible d'enregistrer les participants."
                )
            } finally {
                isSaving = false
            }
        }
    }

    BackHandler(enabled = !isSaving, onBack = ::saveAndExit)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gérer les Participants", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = ::saveAndExit, enabled = !isSaving) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    if (state.peutAjouterMembres || allSelected) {
                        TextButton(
                            enabled = !isSaving,
                            onClick = {
                                val updated = selectedParts.toMutableMap()
                                if (allSelected) {
                                    updated.keys.toList().forEach { memberId ->
                                        if ((state.partsEncaisseesMap[memberId] ?: 0) == 0) {
                                            updated.remove(memberId)
                                        }
                                    }
                                } else {
                                    state.allTontineMembers.forEach { member ->
                                        updated.putIfAbsent(member.member.id, 1)
                                    }
                                }
                                initialSelection = updated
                            }
                        ) {
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
                val isChecked = memberId in selectedParts

                // Nombre de parts touchées (0 par défaut)
                val partsDejaTouchees = state.partsEncaisseesMap[memberId] ?: 0
                val totalPartsActuelles = selectedParts[memberId] ?: 1
                val canBeRemoved = !isChecked || (partsDejaTouchees == 0 && !isSaving)

                ParticipantSelectionRow(
                    name = memberWithUser.member.nomSaisi,
                    phone = memberWithUser.member.phone,
                    isSelected = isChecked,
                    partsCount = totalPartsActuelles,
                    minPartsAllowed = partsDejaTouchees, 
                    canBeRemoved = canBeRemoved,
                    canBeAdded = state.peutAjouterMembres && !isSaving,
                    canChangeParts = state.peutAjouterMembres && !isSaving,
                    onToggle = { selected ->
                        val updated = selectedParts.toMutableMap()
                        if (selected) updated[memberId] = totalPartsActuelles
                        else updated.remove(memberId)
                        initialSelection = updated
                    },
                    onPartsChange = { newParts ->
                        initialSelection = selectedParts.toMutableMap().apply {
                            put(memberId, newParts)
                        }
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
    canChangeParts: Boolean = true,
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
                    enabled = canChangeParts && partsCount > minPartsAllowed && partsCount > 1
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

                IconButton(onClick = { onPartsChange(partsCount + 1) }, enabled = canChangeParts) {
                    Icon(Icons.Default.Add, null, tint = NeonCyan)
                }
            }
        }
    }
}