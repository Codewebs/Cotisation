package com.indiza.cotisation.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.indiza.cotisation.Screen
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.NeonCyan

data class ReportCategory(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsMenuScreen(
    caisseId: Long,
    cycleId: Long,
    navController: NavController,
    viewModel: ReportViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val header by viewModel.headerState.collectAsState()
    var selectedCycleId by remember(cycleId) { mutableStateOf(cycleId) }

    LaunchedEffect(caisseId, selectedCycleId) {
        viewModel.loadFinanceReport(caisseId, selectedCycleId)
    }

    val categories = listOf(
        ReportCategory(
            "Finance",
            "Bilan, Solvabilité, Performance",
            Icons.Default.AccountBalanceWallet,
            NeonCyan,
            Screen.FinanceReport.createRoute(caisseId, selectedCycleId)
        ),
        ReportCategory(
            "Présence",
            "Retards, Top Cotisants, Amendes",
            Icons.Default.EventAvailable,
            AfroGold,
            Screen.AttendanceReport.createRoute(caisseId, selectedCycleId)
        ),
        ReportCategory(
            "Prêts",
            "Dettes, Intérêts, Prévisionnel",
            Icons.Default.MonetizationOn,
            Color(0xFFFF7A59),
            Screen.LoanReport.createRoute(caisseId, selectedCycleId)
        ),
        ReportCategory(
            "Remboursements",
            "Suivi des versements reçus",
            Icons.Default.Payments,
            NeonCyan,
            Screen.RemboursementReport.createRoute(caisseId, selectedCycleId)
        ),
        ReportCategory(
            "Membres",
            "Synthèse individuelle",
            Icons.Default.People,
            Color(0xFF8BE9FD),
            Screen.MemberReport.createRoute(caisseId, selectedCycleId)
        ),
        ReportCategory(
            "Clôture",
            "Bilan final, Historique",
            Icons.Default.AssignmentTurnedIn,
            Color(0xFF50FA7B),
            Screen.CycleClosureReport.createRoute(caisseId, selectedCycleId)
        ),
        ReportCategory(
            "Prédictif",
            "Date fin, Alertes risque",
            Icons.Default.Insights,
            Color(0xFFBD93F9),
            Screen.PredictiveReport.createRoute(caisseId, selectedCycleId)
        )
    )

    CosmicBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Rapports & Statistiques", fontWeight = FontWeight.Bold) },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                BasicReportHeader(header)

                // --- FILTRE DE CYCLES ---
                if (header.allCycles.size > 1) {
                    Text(
                        "Changer de Cycle :",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ScrollableTabRow(
                        selectedTabIndex = header.allCycles.indexOfFirst { it.id == selectedCycleId }.coerceAtLeast(0),
                        containerColor = Color.Transparent,
                        contentColor = NeonCyan,
                        edgePadding = 0.dp,
                        divider = {},
                        indicator = {}
                    ) {
                        header.allCycles.forEach { cycle ->
                            val isSelected = cycle.id == selectedCycleId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCycleId = cycle.id },
                                label = { Text("Cycle ${cycle.numeroCycle}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = Color.Black,
                                    labelColor = Color.White,
                                    containerColor = Color.White.copy(alpha = 0.05f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color.White.copy(alpha = 0.2f),
                                    selectedBorderColor = NeonCyan
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Text(
                    "Choisissez une catégorie pour explorer les données du Cycle ${header.cycleNumber}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(categories) { category ->
                        ReportCard(category) {
                            navController.navigate(category.route)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportCard(category: ReportCategory, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.85f)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, category.color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = category.color,
                modifier = Modifier.size(32.dp)
            )
            
            Column {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 2
                )
            }
        }
    }
}
