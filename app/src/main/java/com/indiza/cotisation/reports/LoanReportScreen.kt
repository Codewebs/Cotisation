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
fun LoanReportScreen(
    caisseId: Long,
    cycleId: Long,
    viewModel: ReportViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    var currentCycleId by remember(cycleId) { mutableStateOf(cycleId) }
    val state by viewModel.loanState.collectAsState()
    val header by viewModel.headerState.collectAsState()

    LaunchedEffect(caisseId, currentCycleId) {
        viewModel.loadLoanReport(caisseId, currentCycleId)
    }

    CosmicBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("État des Dettes & Prêts", fontWeight = FontWeight.Bold) },
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
                        label = "Total Dettes (Global)",
                        value = "${state.totalDettesJusqueIci} F",
                        color = Color(0xFFFF7A59),
                        description = "Montant total des capitaux actuellement détenus par les membres (prêts non encore remboursés), tous cycles confondus."
                    )
                }
                
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ReportMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Intérêts (Cycle)",
                            value = "${state.rendementInteretsCycle} F",
                            color = NeonCyan,
                            description = "Intérêts perçus sur les remboursements effectués durant ce cycle."
                        )
                        ReportMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Cumul Intérêts",
                            value = "${state.rendementInteretsJusqueIci} F",
                            color = NeonCyan.copy(alpha = 0.7f),
                            description = "Total historique des intérêts générés jusqu'à la fin de ce cycle."
                        )
                    }
                }

                item {
                    Text(
                        "Prévisionnel Trésorerie",
                        style = MaterialTheme.typography.titleMedium,
                        color = AfroGold,
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
                            "Analyse des remboursements attendus pour estimer les liquidités futures.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
