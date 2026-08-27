package com.indiza.cotisation.caisses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.indiza.cotisation.CaisseAction
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.NeonCyan

@Composable
fun CaisseActionButtons(
    caisseType: CaisseType?,
    possibilitePret: Boolean,
    onActionSelected: (CaisseAction) -> Unit
) {
    // LOG DE DEBUG : Vérifiez votre console Logcat
    LaunchedEffect(caisseType, possibilitePret) {
        println("DEBUG_FAB: Type=$caisseType, Pret=$possibilitePret")
    }

    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // 1. TOUJOURS : Cotisation
        ActionLabelButton(label = "Cotisation", icon = Icons.Default.AccountBalanceWallet, color = NeonCyan) {
            onActionSelected(CaisseAction.COTISATION)
        }

        // 2. CONDITIONNEL : Cagnotte (Vérifiez bien l'enum COTISATION_SIMPLE)
        if (caisseType == CaisseType.COTISATION_SIMPLE) {
            ActionLabelButton(label = "Attribuer Cagnotte", icon = Icons.Default.EmojiEvents, color = AfroGold) {
                onActionSelected(CaisseAction.TOUR_ENCAISSER)
            }
        }

        // 3. CONDITIONNEL : Prêts
        if (possibilitePret || caisseType == CaisseType.PRET) {
            ActionLabelButton(label = "Accorder un Prêt", icon = Icons.Default.MoneyOff, color = Color.Red) {
                onActionSelected(CaisseAction.PRET)
            }
            ActionLabelButton(label = "Remboursement", icon = Icons.Default.Payments, color = Color.Green) {
                onActionSelected(CaisseAction.REMBOURSEMENT)
            }
        }

        // 4. CONDITIONNEL : Secours / Fonds social
        if (caisseType == CaisseType.SECOURS) {
            ActionLabelButton(label = "Sortie Secours", icon = Icons.Default.MedicalServices, color = Color.Magenta) {
                onActionSelected(CaisseAction.SORTIE_SECOURS)
            }
        }
        if (caisseType !== CaisseType.PRET && caisseType !== CaisseType.COTISATION_SIMPLE) {
            ActionLabelButton(label = "Sortie", icon = Icons.Default.ExitToApp, color = Color.Magenta) {
                onActionSelected(CaisseAction.SORTIE)
            }
        }
    }
}

@Composable
fun ActionLabelButton(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(8.dp), color = Color.DarkGray, modifier = Modifier.padding(end = 8.dp)) {
            Text(label, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
        }
        FloatingActionButton(onClick = onClick, containerColor = color, contentColor = Color.Black, modifier = Modifier.size(48.dp)) {
            Icon(icon, contentDescription = null)
        }
    }
}