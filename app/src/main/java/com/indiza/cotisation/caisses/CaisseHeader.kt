package com.indiza.cotisation.caisses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indiza.cotisation.bank.CycleEntity
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.DeepDarkViolet
import com.indiza.cotisation.tools.DeepWhite
import com.indiza.cotisation.tools.GlassWhite
import com.indiza.cotisation.tools.NeonCyan

@Composable
fun CaisseHeader(
    caisse: CaisseEntity?,
    currentCycle: CycleEntity?,
    soldeDuCycle: Long = 0L,
    soldePrecedent: Long = 0L,
    totalPrets: Long = 0L,
    toursCompletes: Int = 0,
    nombreCotisants: Int = 0,
    nombreBeneficiaires: Int = 0,
    prochainBeneficiaire: String? = null,
    totalParticipants: Int = 0,
    montantCible: Long = 0L,
    dateLimitePaiement: Int? = null
) {
    if (caisse == null) return

    val montantObjectif = if (montantCible > 0L) montantCible else {
        val baseMontant = caisse.montantFixe ?: 0L
        if (totalParticipants > 0) baseMontant * totalParticipants else baseMontant
    }
    // Le solde affiché correspond au montant réellement disponible en caisse pour ce cycle,
    // c'est-à-dire l'argent non encore attribué à une cagnotte, un prêt ou une autre sortie.
    val ratioMembres = if (totalParticipants > 0) nombreCotisants.toFloat() / totalParticipants.toFloat() else 0f
    val ratioSolde = if (montantObjectif > 0L) soldeDuCycle.toFloat() / montantObjectif.toFloat() else 0f
    val montantAmende = caisse.penaliteRetardFixe ?: 0L
    val montantPenalite = caisse.penaliteEchecFixe ?: 0L

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A1E2A),
                        Color(0xFF0F172A)
                    )
                )
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.85f)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = caisse.libelle.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cycle ${currentCycle?.numeroCycle ?: 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AfroGold
                    )
                }

                MiniTag(caisse.type.name, NeonCyan.copy(alpha = 0.2f))
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "MÉMBRES À JOUR",
                    value = "$nombreCotisants / ${if (totalParticipants > 0) totalParticipants else nombreCotisants}",
                    accentColor = NeonCyan,
                    helper = "${(ratioMembres * 100).toInt()}%"
                )

                DetailMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "SOLDE EN CAISSE / CIBLE",
                    value = "$soldeDuCycle / $montantObjectif F",
                    accentColor = AfroGold,
                    helper = if (soldePrecedent > 0) {
                        "${(ratioSolde * 100).toInt()}% (Préc. +$soldePrecedent F)"
                    } else {
                        "${(ratioSolde * 100).toInt()}%"
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "AMENDES",
                    value = "$montantAmende F",
                    accentColor = Color(0xFFFFC857),
                    helper = "Retard"
                )

                DetailMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "PÉNALITÉS",
                    value = "$montantPenalite F",
                    accentColor = Color(0xFFFF7A59),
                    helper = "Échec"
                )

                DetailMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "DATE LIMITE",
                    value = dateLimitePaiement?.let { "Jour $it" } ?: "—",
                    accentColor = Color(0xFF8BE9FD),
                    helper = "Paiement"
                )
            }

            Divider(Modifier.padding(top = 14.dp, bottom = 12.dp), color = Color.White.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (totalPrets > 0) {
                    Column {
                        Text(
                            "PRÊTS EN COURS",
                            color = Color.Red.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "$totalPrets F",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Column {
                        Text(
                            "STATUT",
                            color = GlassWhite.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "Stable",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (prochainBeneficiaire != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "PROCHAINE RÉPARTITION",
                            color = GlassWhite.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            prochainBeneficiaire,
                            color = AfroGold,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color,
    helper: String
) {
    Surface(
        modifier = modifier,
        color = accentColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = helper,
                style = MaterialTheme.typography.labelSmall,
                color = GlassWhite.copy(alpha = 0.65f)
            )
        }
    }
}


@Composable
private fun MiniTag(label: String, color: Color) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        color = color.copy(alpha = 0.8f),
        modifier = Modifier
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            .padding(horizontal = 4.dp)
    )
}
@Composable
private fun CompactInfoItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", color = GlassWhite.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = GlassWhite, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        // Changez Color.White par Color.Black pour tester sur fond blanc
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoBadge(label: String) {
    Surface(
        color = GlassWhite.copy(alpha = 0.1f),
        shape = CircleShape
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = GlassWhite
        )
    }
}