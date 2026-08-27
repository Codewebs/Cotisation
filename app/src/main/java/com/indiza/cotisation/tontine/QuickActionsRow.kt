package com.indiza.cotisation.tontine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.caisses.CaisseType
import com.indiza.cotisation.users.Permission
import com.indiza.cotisation.users.Role

@Composable
fun QuickActionsRow(role: Role) {
    val permissions = role.permissions

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (permissions.contains(Permission.COLLECT_MONEY)) {
            ActionButton(Icons.Default.AddCard, "Encaisser", Color(0xFF2196F3))
        }
        if (permissions.contains(Permission.DISBURSE_MONEY)) {
            ActionButton(Icons.Default.Payments, "Distribuer", Color(0xFF4CAF50))
        }
        if (permissions.contains(Permission.ADD_MEMBER)) {
            ActionButton(Icons.Default.PersonAdd, "Membres", Color.Black)
        }
        if (permissions.contains(Permission.AUDIT_ACCOUNTS)) {
            ActionButton(Icons.Default.Assessment, "Audit", Color(0xFF9C27B0))
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White, CircleShape)
                .shadow(2.dp, CircleShape)
                .border(0.5.dp, color.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun CaisseDetailCard(
    caisse: CaisseEntity,
    soldeGlobal: Long = 0L, // Passer le solde depuis le State du parent
    onClick: () -> Unit
) {
    // Icône avec cercle coloré selon le type
    val typeColor = when(caisse.type) {
        CaisseType.PRET -> Color(0xFF2196F3)
        CaisseType.SECOURS -> Color(0xFF4CAF50)
        else -> Color.Black
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp), ambientColor = typeColor, spotColor = typeColor)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icône avec cercle coloré selon le type
                val typeColor = when(caisse.type) {
                    CaisseType.PRET -> Color(0xFF2196F3)
                    CaisseType.SECOURS -> Color(0xFF4CAF50)
                    else -> Color.Black
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(typeColor.copy(alpha = 0.1f), CircleShape)
                        .border(0.5.dp, typeColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when(caisse.type) {
                            CaisseType.PRET -> Icons.Default.TrendingUp
                            CaisseType.SECOURS -> Icons.Default.HealthAndSafety
                            else -> Icons.Default.AccountBalanceWallet
                        },
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        text = caisse.libelle.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    // Badge Fréquence
                    Text(
                        text = "FREQUENCE: ${caisse.frequence.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                // Affichage du montant fixe
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (caisse.montantFixe != null) "${caisse.montantFixe}" else "Libre",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                    Text("FCFA", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            if (caisse.type == CaisseType.PRET || caisse.tauxInteret != null) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Percent, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                    Text(
                        text = " Intérêt: ${caisse.tauxInteret}% (${caisse.typeInteret})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // --- FOOTER : SOLDE GLOBAL ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SOLDE EN CAISSE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = "$soldeGlobal FCFA",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
