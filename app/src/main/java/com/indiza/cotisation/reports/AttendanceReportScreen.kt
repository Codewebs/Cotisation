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
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.NeonCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceReportScreen(
    caisseId: Long,
    cycleId: Long,
    viewModel: ReportViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    var currentCycleId by remember(cycleId) { mutableStateOf(cycleId) }
    val state by viewModel.attendanceState.collectAsState()
    val header by viewModel.headerState.collectAsState()

    LaunchedEffect(caisseId, currentCycleId) {
        viewModel.loadAttendanceReport(caisseId, currentCycleId)
    }

    CosmicBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Rapport de Présence", fontWeight = FontWeight.Bold) },
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
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ReportMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Retards (Cycle)",
                            value = "${state.retardsCountCycle}",
                            color = Color.Red,
                            description = "Nombre de retards enregistrés spécifiquement durant ce cycle."
                        )
                        ReportMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Cumul Retards",
                            value = "${state.retardsCountJusqueIci}",
                            color = Color.Red.copy(alpha = 0.7f),
                            description = "Total des retards depuis le début jusqu'à la fin de ce cycle."
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ReportMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Amendes (Cycle)",
                            value = "${state.totalAmendesCycle} F",
                            color = AfroGold,
                            description = "Pénalités récoltées durant ce cycle."
                        )
                        ReportMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Cumul Amendes",
                            value = "${state.totalAmendesJusqueIci} F",
                            color = AfroGold.copy(alpha = 0.7f),
                            description = "Total des pénalités récoltées historiquement jusqu'à ce cycle."
                        )
                    }
                }
                
                item {
                    Text(
                        "Top Cotisants du Cycle ${header.cycleNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(state.topCotisants) { (name, amount) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.85f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, color = Color.White)
                            Text("${amount} F", color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
