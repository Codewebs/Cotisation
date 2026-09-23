package com.indiza.cotisation.caisses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
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
import com.indiza.cotisation.tools.GlassWhite
import com.indiza.cotisation.tools.NeonCyan

@Composable
fun CaisseHeader(
    caisse: com.indiza.cotisation.caisses.CaisseEntity?,
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
    dateLimitePaiement: Int? = null,
    totalAmendes: Long = 0L,
    totalPenalites: Long = 0L,
    montantDistribue: Long = 0L,
    beneficiaires: List<String> = emptyList(),
    montantRecolte: Long = 0L,
    onReportClick: () -> Unit = {},
    onToggleLock: () -> Unit = {},
    onBalanceClick: () -> Unit = {}
) {
    if (caisse == null) return

    val montantObjectif = if (montantCible > 0L) montantCible else {
        val baseMontant = caisse.montantFixe ?: 0L
        if (totalParticipants > 0) baseMontant * totalParticipants else baseMontant
    }
    
    val ratioMembres = if (totalParticipants > 0) nombreCotisants.toFloat() / totalParticipants.toFloat() else 0f
    val ratioSolde = if (montantObjectif > 0L) montantRecolte.toFloat() / montantObjectif.toFloat() else 0f
    val montantAmende = caisse.penaliteRetardFixe ?: 0L
    val montantPenalite = caisse.penaliteEchecFixe ?: 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    if (caisse.type != CaisseType.PRET) {
                        Text(
                            text = "Cycle ${currentCycle?.numeroCycle ?: 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AfroGold
                        )
                    } else {
                        val taux = caisse.tauxInteret ?: 0.0
                        Text(
                            text = "Taux d'intérêt : $taux%",
                            style = MaterialTheme.typography.labelSmall,
                            color = AfroGold
                        )
                    }
                }

                MiniTag(caisse.type.name, NeonCyan.copy(alpha = 0.2f))
            }

            Spacer(Modifier.height(16.dp))

            if (caisse.type == CaisseType.PRET) {
                // Design spécifique pour le PRÊT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "CAPITAL DISPONIBLE",
                        value = "$soldeDuCycle F",
                        accentColor = NeonCyan,
                        helper = "Prêtable"
                    )

                    DetailMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "PRÊTS EN COURS",
                        value = "$totalPrets F",
                        accentColor = AfroGold,
                        helper = "À recouvrer"
                    )
                }
            } else {
                // Design classique pour les autres (COTISATION, SECOURS, etc.)
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
                        modifier = Modifier.weight(1f).clickable { onBalanceClick() },
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
            }

            Spacer(Modifier.height(12.dp))

            if (caisse.type != CaisseType.PRET) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailMetricCard(
                        modifier = Modifier.weight(1f),
                        label = if (currentCycle?.estCloture == true) "TOT. AMENDES" else "AMENDES",
                        value = "${if (currentCycle?.estCloture == true) totalAmendes else montantAmende} F",
                        accentColor = Color(0xFFFFC857),
                        helper = "Retard"
                    )

                    DetailMetricCard(
                        modifier = Modifier.weight(1f),
                        label = if (currentCycle?.estCloture == true) "TOT. PÉNALITÉS" else "PÉNALITÉS",
                        value = "${if (currentCycle?.estCloture == true) totalPenalites else montantPenalite} F",
                        accentColor = Color(0xFFFF7A59),
                        helper = "Échec"
                    )

                    if (currentCycle?.estCloture != true) {
                        DetailMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "DATE LIMITE",
                            value = dateLimitePaiement?.let { "Jour $it" } ?: "—",
                            accentColor = Color(0xFF8BE9FD),
                            helper = "Paiement"
                        )
                    } else {
                        DetailMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "RÉCOLTÉ",
                            value = "$montantRecolte F",
                            accentColor = NeonCyan,
                            helper = "Total Cycle"
                        )
                    }
                }
            } else {
                // Pour le prêt, on peut mettre d'autres infos si nécessaire,
                // ou laisser vide pour aérer le design spécifique
            }

            if (currentCycle?.estCloture == true) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "DISTRIBUÉ",
                        value = "$montantDistribue F",
                        accentColor = AfroGold,
                        helper = "Cagnottes"
                    )
                    DetailMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "EN CAISSE",
                        value = "$soldeDuCycle F",
                        accentColor = Color.White,
                        helper = "Solde Final"
                    )
                }

                if (beneficiaires.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "BÉNÉFICIAIRES : ${beneficiaires.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(Modifier.padding(top = 14.dp, bottom = 12.dp), color = Color.White.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (caisse.type == CaisseType.PRET) {
                    Column {
                        Text(
                            "STATUT",
                            color = GlassWhite.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "Ouverte",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (totalPrets > 0) {
                    Column {
                        Text(
                            "PRÊTS EN COURS",
                            color = Color.Red.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "${kotlin.math.abs(totalPrets)} F",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        
                        Spacer(Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = onToggleLock,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                if (caisse.ordreFige) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Figer l'ordre",
                                tint = if (caisse.ordreFige) AfroGold else NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = onReportClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Rapport",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (caisse.type != CaisseType.PRET && prochainBeneficiaire != null) {
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
