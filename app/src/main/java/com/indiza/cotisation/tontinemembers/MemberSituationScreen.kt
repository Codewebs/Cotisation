package com.indiza.cotisation.tontinemembers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.indiza.cotisation.caisses.CaisseType
import com.indiza.cotisation.tontine.TontineMemberEntity
import com.indiza.cotisation.tools.NeonCyan
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.ui.graphics.StrokeCap
import com.indiza.cotisation.tools.AfroGold

@Composable
fun MemberSituationScreen(
    state: MemberSituationState,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECTION 1 : IDENTITÉ (Générique) ---
        MemberHeaderCard(state.member)

        // --- SECTION 2 : PARTS (Générique mais variable) ---
        PartsSummaryCard(state)

        // --- SECTION 3 : DYNAMIQUE SELON LE TYPE ---
        when (state.caisse.type) {
            CaisseType.COTISATION_SIMPLE -> {
                CagnotteSituationSection(state)
            }
            CaisseType.PRET -> {
                LoanSituationSection(state)
            }
            CaisseType.SECOURS -> {
                SocialSituationSection(state)
            }
            else -> { /* Autres cas */ }
        }

        // --- SECTION 4 : DISCIPLINE (Retards/Echecs) ---
        DisciplineCard(state.retardsCount, state.echecsCount)
    }
}

@Composable
fun MemberHeaderCard(member: TontineMemberEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar avec initiale
            Box(
                modifier = Modifier.size(48.dp).background(NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(member.nomSaisi.take(1), color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(member.nomSaisi, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${member.role.displayName} • ID: ${member.id} • ${member.matricule ?: "Pas de matricule"}", style = MaterialTheme.typography.bodySmall)
                Text(member.phone, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
@Composable
fun LoanSituationSection(state: MemberSituationState) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Situation des Prêts", color = Color.Red, fontWeight = FontWeight.Bold)
            Divider(Modifier.padding(vertical = 8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoColumn("Emprunté", "${state.montantEmprunte} FCFA")
                InfoColumn("Remboursé", "${state.montantRembourse} FCFA")
            }

            val reste = state.montantEmprunte - state.montantRembourse
            if (reste > 0) {
                Text(
                    "Reste à payer : $reste FCFA",
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
@Composable
fun PartsSummaryCard(state: MemberSituationState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Nombre de Parts", style = MaterialTheme.typography.labelMedium)
                Text("${state.participantInfo.nombreParts}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Total Cotisé", style = MaterialTheme.typography.labelMedium)
                Text("${state.totalCotise} FCFA", style = MaterialTheme.typography.titleLarge, color = NeonCyan)
            }
        }
    }
}
@Composable
fun CagnotteSituationSection(state: MemberSituationState) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AfroGold)
                Spacer(Modifier.width(8.dp))
                Text("Rotation de la Cagnotte", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = if (state.participantInfo.estBeneficiaire) 1f else 0.5f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = AfroGold,
            )
            Text(
                text = if (state.participantInfo.estBeneficiaire) "Déjà encaissé" else "En attente d'encaissement",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
@Composable
fun SocialSituationSection(state: MemberSituationState) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Fonds Social / Secours", style = MaterialTheme.typography.titleSmall, color = Color.Magenta)
            Spacer(Modifier.height(8.dp))
            Text("Statut : Membre à jour", style = MaterialTheme.typography.bodyMedium)
            // On peut ajouter ici le nombre d'assistances reçues si vous avez l'info
        }
    }
}
@Composable
fun DisciplineCard(retards: Int, echecs: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Bloc Retards
        Surface(
            modifier = Modifier.weight(1f),
            color = if (retards > 0) Color(0xFFFF9800).copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (retards > 0) Color(0xFFFF9800) else Color.Transparent)
        ) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Retards", style = MaterialTheme.typography.labelSmall)
                Text("$retards", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        // Bloc Échecs
        Surface(
            modifier = Modifier.weight(1f),
            color = if (echecs > 0) Color.Red.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (echecs > 0) Color.Red else Color.Transparent)
        ) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Échecs", style = MaterialTheme.typography.labelSmall)
                Text("$echecs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InfoColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
