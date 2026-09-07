package com.indiza.cotisation.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun MemberReportScreen(
    caisseId: Long,
    cycleId: Long,
    viewModel: CaisseDetailViewModel = hiltViewModel(),
    reportViewModel: ReportViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    var currentCycleId by remember(cycleId) { mutableStateOf(cycleId) }
    val state by viewModel.state.collectAsState()
    val header by reportViewModel.headerState.collectAsState()

    LaunchedEffect(caisseId, currentCycleId) {
        reportViewModel.loadFinanceReport(caisseId, currentCycleId)
        viewModel.selectCycleById(currentCycleId)
    }

    CosmicBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Synthèse par Membre", fontWeight = FontWeight.Bold) },
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    BasicReportHeader(header, onCycleSelected = { currentCycleId = it })
                }
                
                items(state.reportRows) { row ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.85f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (row.status == "À JOUR") NeonCyan.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(row.memberName, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    row.status,
                                    color = if (row.status == "À JOUR") NeonCyan else Color.Red,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Cotisé ce cycle", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                    Text("${row.cotiseCeCycle} F", color = AfroGold)
                                }
                                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                    Text("Cumul", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                    Text("${row.dejaCotise} F", color = NeonCyan)
                                }
                                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                    Text("Manquant", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                    Text("${row.manquant} F", color = if (row.manquant > 0) Color.Red else Color.White)
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
