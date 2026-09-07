package com.indiza.cotisation.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.NeonCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceReportScreen(
    caisseId: Long,
    cycleId: Long,
    viewModel: ReportViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    var currentCycleId by remember(cycleId) { mutableStateOf(cycleId) }
    val state by viewModel.financeState.collectAsState()
    val header by viewModel.headerState.collectAsState()

    LaunchedEffect(caisseId, currentCycleId) {
        viewModel.loadFinanceReport(caisseId, currentCycleId)
    }

    CosmicBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Rapport Financier", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    BasicReportHeader(header, onCycleSelected = { currentCycleId = it })
                }

                item {
                    ReportMetricCard(
                        label = "Solde Cumulé (Jusqu'au Cycle ${header.cycleNumber})",
                        value = "${state.soldeJusqueIci} F",
                        color = NeonCyan,
                        description = "Le montant qui était disponible dans la caisse à la fin de ce cycle. Inclut tous les cycles précédents."
                    )
                }
                item {
                    ReportMetricCard(
                        label = "Résultat du Cycle ${header.cycleNumber}",
                        value = "${state.soldeCycle} F",
                        color = AfroGold,
                        description = "Le bénéfice ou la perte (Entrées - Sorties) généré uniquement durant ce cycle d'activité."
                    )
                }
                item {
                    ReportMetricCard(
                        label = "Performance du Cycle",
                        value = String.format("%.0f%%", state.performanceCycle * 100),
                        color = Color(0xFF50FA7B),
                        helper = "Récolte vs Objectif théorique",
                        description = "Le taux de recouvrement des cotisations attendues pour ce cycle uniquement."
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ReportMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Cotisations (Cycle)",
                            value = "${state.totalCotisationsCycle} F",
                            color = NeonCyan,
                            description = "Total des cotisations versées durant ce cycle."
                        )
                        ReportMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Cumul Cotisations",
                            value = "${state.totalCotisationsJusqueIci} F",
                            color = NeonCyan.copy(alpha = 0.7f),
                            description = "Somme de toutes les cotisations depuis la création jusqu'à la fin de ce cycle."
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ReportMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Sorties (Cycle)",
                            value = "${state.totalSortiesCycle} F",
                            color = Color(0xFFFF7A59),
                            description = "Total des décaissements effectués durant ce cycle."
                        )
                        ReportMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Cumul Sorties",
                            value = "${state.totalSortiesJusqueIci} F",
                            color = Color(0xFFFF7A59).copy(alpha = 0.7f),
                            description = "Somme de tous les décaissements depuis la création jusqu'à la fin de ce cycle."
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
