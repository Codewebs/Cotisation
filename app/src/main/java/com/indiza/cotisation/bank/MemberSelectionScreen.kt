package com.indiza.cotisation.bank

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiza.cotisation.CaisseAction
import com.indiza.cotisation.caisses.CaisseDetailViewModel
import com.indiza.cotisation.tontine.TontineMemberWithUser
import com.indiza.cotisation.tools.NeonCyan
import androidx.compose.runtime.* // ← IMPORT COMPLET
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.indiza.cotisation.tontinemembers.ParticipantWithParts
import com.indiza.cotisation.tools.AfroGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberSelectionScreen(
    caisseId: Long,
    actionType: CaisseAction,
    viewModel: CaisseDetailViewModel = hiltViewModel(),
    onMemberSelected: (ParticipantWithParts, Long) -> Unit,
    navController: NavController,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cycleId = state.currentCycle?.id ?: 0L

    val participantsAafficher = if (actionType == CaisseAction.TOUR_ENCAISSER) {
        state.participantsPlanifies
    } else {
        state.participants
    }

    LaunchedEffect(state, actionType) {
        println("DEBUG_SELECTION: === Entrée Interface Sélection ===")
        println("DEBUG_SELECTION: Action: $actionType")
        println("DEBUG_SELECTION: Caisse ID: $caisseId")
        println("DEBUG_SELECTION: Cycle ID: $cycleId")
        println("DEBUG_SELECTION: Nb Participants Total (Caisse): ${state.participants.size}")
        state.participants.forEach { p ->
            println("DEBUG_SELECTION:   - Participant: ${p.member.nomSaisi} (ID: ${p.member.id})")
        }
        println("DEBUG_SELECTION: Nb Participants Planifiés: ${state.participantsPlanifies.size}")
        state.participantsPlanifies.forEach { p ->
            println("DEBUG_SELECTION:   - Planifié: ${p.member.nomSaisi} (ID: ${p.member.id})")
        }
        println("DEBUG_SELECTION: Final participantsAafficher size: ${participantsAafficher.size}")
        participantsAafficher.forEach { p ->
            println("DEBUG_SELECTION:   - A Afficher: ${p.member.nomSaisi}")
        }
        println("DEBUG_SELECTION: =================================")
    }

    if (state.currentCycle == null) return
    val membresCotises = if (actionType == CaisseAction.COTISATION) state.dejaCotise.size else 0

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when(actionType) {
                            CaisseAction.TOUR_ENCAISSER -> "Attribuer Cagnotte"
                            CaisseAction.COTISATION -> "Enregistrer Cotisation"
                            CaisseAction.PRET -> "Accorder un Prêt"
                            CaisseAction.REMBOURSEMENT -> "Enregistrer Remboursement"
                            CaisseAction.SORTIE_SECOURS -> "Sortie de Secours"
                            CaisseAction.SORTIE -> "Nouvelle Sortie"
                            else -> "Sélectionner un membre"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (actionType == CaisseAction.COTISATION || actionType == CaisseAction.TOUR_ENCAISSER) {
                val participantsNonTraites = participantsAafficher.filter { p ->
                    if (actionType == CaisseAction.COTISATION) !state.dejaCotise.contains(p.memberWithUser.member.id)
                    else true
                }

                if (participantsNonTraites.isNotEmpty()) {
                    Button(
                        onClick = {
                            viewModel.reinitialiserExclusions()
                            navController.navigate("recapitulatif_masse/$caisseId/${actionType.name}/$cycleId")
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.DoneAll, null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("VALIDER POUR TOUS (${participantsNonTraites.size})", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (participantsAafficher.isEmpty()) {
            EmptyStatePlaceholder(actionType, padding)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (actionType == CaisseAction.COTISATION) {
                    Surface(
                        color = NeonCyan.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.18f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cotisations",
                                color = NeonCyan,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$membresCotises / ${participantsAafficher.size}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(participantsAafficher) { participant ->
                        val memberId = participant.memberWithUser.member.id
                        val aDejaCotise = state.dejaCotise.contains(memberId)
                        val estPlanifie = actionType == CaisseAction.TOUR_ENCAISSER && 
                                          state.participantsPlanifies.any { it.member.id == memberId }

                        val estDesactive = when (actionType) {
                            CaisseAction.COTISATION -> aDejaCotise
                            else -> false
                        }

                        val messageStatus = when {
                            actionType == CaisseAction.COTISATION && aDejaCotise -> "Cotisation déjà effectuée"
                            estPlanifie -> "TOUR PROGRAMMÉ"
                            else -> null
                        }

                        MemberSelectionCard(
                            participant = participant,
                            estDesactive = estDesactive,
                            messageStatus = messageStatus,
                            onClick = { onMemberSelected(participant, cycleId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemberSelectionCard(
    participant: ParticipantWithParts,
    estDesactive: Boolean,
    messageStatus: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !estDesactive) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (estDesactive) Color.White.copy(alpha = 0.05f) else Color(0xFF1A1A1A)
        ),
        border = if (estDesactive) null else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .alpha(if (estDesactive) 0.4f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(if (estDesactive) Color.Gray else NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    participant.memberWithUser.member.nomSaisi.take(1),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = participant.memberWithUser.member.nomSaisi,
                    color = if (estDesactive) Color.Gray else Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${participant.nombreParts} part${if (participant.nombreParts > 1) "s" else ""}",
                        color = AfroGold,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    messageStatus?.let {
                        Text(it, color = AfroGold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Badge(
                containerColor = AfroGold.copy(alpha = 0.18f),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("${participant.nombreParts}P", color = AfroGold, style = MaterialTheme.typography.labelSmall)
            }

            if (!estDesactive) {
                Icon(Icons.Default.ChevronRight, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Default.CheckCircle, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder(actionType: CaisseAction, padding: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text(
            text = if (actionType == CaisseAction.TOUR_ENCAISSER)
                "Aucun tour planifié pour ce cycle.\nVeuillez d'abord utiliser 'Ajouter Tour' dans les détails de la caisse."
            else "Aucun participant trouvé.",
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}