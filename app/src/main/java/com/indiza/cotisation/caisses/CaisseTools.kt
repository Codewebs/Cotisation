package com.indiza.cotisation.caisses

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CaisseTheme {
    fun getTheme(type: CaisseType): CaisseThemeData {
        return when (type) {
            CaisseType.PRET -> CaisseThemeData(
                color = Color(0xFF2196F3),
                icon = Icons.Default.AttachMoney,
                label = "Prêt / Crédit"
            )
            CaisseType.SECOURS -> CaisseThemeData(
                color = Color(0xFFE91E63),
                icon = Icons.Default.HealthAndSafety,
                label = "Secours / Social"
            )
            CaisseType.DEVELOPPEMENT -> CaisseThemeData(
                color = Color(0xFFFF9800),
                icon = Icons.Default.TrendingUp,
                label = "Développement"
            )
            CaisseType.COTISATION_SIMPLE -> CaisseThemeData(
                color = Color(0xFF4CAF50),
                icon = Icons.Default.AccountBalanceWallet,
                label = "Cotisation Simple"
            )
            CaisseType.FONCTIONNEMENT -> CaisseThemeData(
                color = Color(0xFF9C27B0),
                icon = Icons.Default.Settings,
                label = "Fonctionnement"
            )
            else -> CaisseThemeData(
                color = Color(0xFF607D8B),
                icon = Icons.Default.Tune,
                label = "Personnalisée"
            )
        }
    }
}

data class CaisseThemeData(
    val color: Color,
    val icon: ImageVector,
    val label: String
)
