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
import com.indiza.cotisation.caisses.CaisseDetailViewModel
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.NeonCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleClosureReportScreen(
    caisseId: Long,
    cycleId: Long,
    viewModel: CaisseDetailViewModel = hiltViewModel(),
    reportViewModel: ReportViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    var currentCycleId by remember(cycleId) { mutableStateOf(cycleId) }
    val state by viewModel.state.collectAsState()
    val reportState by reportViewModel.financeState.collectAsState()
    val header by reportViewModel.headerState.collectAsState()

    LaunchedEffect(caisseId, currentCycleId) {
        reportViewModel.loadFinanceReport(caisseId, currentCycleId)
        viewModel.selectCycleById(currentCycleId)
    }

    CosmicBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Bilan de Clôture", fontWeight = FontWeight.Bold) },
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
                        label = "Total Récolté (Cycle ${header.cycleNumber})",
                        value = "${reportState.totalCotisationsCycle} F",
                        color = NeonCyan,
                        description = "Somme des cotisations encaissées durant ce cycle précis."
                    )
                }
                item {
                    ReportMetricCard(
                        label = "Total Distribué (Cycle ${header.cycleNumber})",
                        value = "${reportState.totalSortiesCycle} F",
                        color = AfroGold,
                        description = "Somme des décaissements (cagnottes, aides) effectués durant ce cycle."
                    )
                }
                item {
                    ReportMetricCard(
                        label = "Solde du Cycle",
                        value = "${reportState.soldeCycle} F",
                        color = Color.White,
                        description = "Différence entre les entrées et les sorties générées uniquement par ce cycle."
                    )
                }
                item {
                    ReportMetricCard(
                        label = "Solde Caisse Jusqu'ici",
                        value = "${reportState.soldeJusqueIci} F",
                        color = NeonCyan.copy(alpha = 0.8f),
                        description = "Montant qui était réellement en caisse à la fin de ce cycle (incluant les cycles précédents)."
                    )
                }
                
                item {
                    Text(
                        "Historique des Tours",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.85f))
                    ) {
                        Text(
                            "Ce cycle a comporté ${state.toursDuCycle.size} tours validés.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
