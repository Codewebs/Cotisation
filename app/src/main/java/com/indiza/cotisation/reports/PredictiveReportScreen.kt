package com.indiza.cotisation.reports

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.NeonCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictiveReportScreen(
    caisseId: Long,
    cycleId: Long,
    viewModel: ReportViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    var currentCycleId by remember(cycleId) { mutableStateOf(cycleId) }
    val header by viewModel.headerState.collectAsState()

    LaunchedEffect(caisseId, currentCycleId) {
        viewModel.loadFinanceReport(caisseId, currentCycleId)
    }

    CosmicBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Analyse Prédictive", fontWeight = FontWeight.Bold) },
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
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicReportHeader(header, onCycleSelected = { currentCycleId = it })
                
                Spacer(Modifier.weight(0.5f))
                
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "L'Analyse Prédictive arrive bientôt !",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Nous utiliserons vos données historiques pour estimer la date de fin de cycle et alerter sur les risques de liquidités.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = AfroGold, contentColor = Color.Black)
                ) {
                    Text("RETOUR")
                }

                Spacer(Modifier.weight(1f))
            }
        }
    }
}
