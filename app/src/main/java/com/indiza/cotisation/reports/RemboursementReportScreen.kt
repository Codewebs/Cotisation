package com.indiza.cotisation.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemboursementReportScreen(
    caisseId: Long,
    cycleId: Long,
    viewModel: ReportViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    var currentCycleId by remember(cycleId) { mutableStateOf(cycleId) }
    val header by viewModel.headerState.collectAsState()
    // Pour l'instant on utilise LoanReportState car les remboursements y sont liés
    val state by viewModel.loanState.collectAsState()

    LaunchedEffect(caisseId, currentCycleId) {
        viewModel.loadLoanReport(caisseId, currentCycleId)
    }

    CosmicBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Rapport des Remboursements", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* PDF */ }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = NeonCyan)
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
                        label = "Total Remboursé (Cycle)",
                        value = "${state.rendementInteretsCycle} F", // Placeholder, à affiner
                        color = NeonCyan,
                        description = "Montant total des remboursements (capital + intérêts) perçus durant ce cycle."
                    )
                }

                item {
                    Text(
                        "Détails des Remboursements",
                        style = MaterialTheme.typography.titleMedium,
                        color = AfroGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Ici on pourrait lister les transactions de type REMBOURSEMENT
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.85f))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Aucun remboursement détaillé pour le moment.", color = Color.Gray)
                        }
                    }
                }
                
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
