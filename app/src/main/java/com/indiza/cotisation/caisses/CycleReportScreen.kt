package com.indiza.cotisation.caisses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiza.cotisation.tools.NeonCyan
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleReportScreen(
    caisseId: Long,
    cycleId: Long,
    cycleNumber: Int,
    onBack: () -> Unit,
    viewModel: CaisseDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(cycleId) {
        viewModel.selectCycleById(cycleId)
    }

    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Column {
            // Manual Top Bar
            CenterAlignedTopAppBar(
                title = { Text("Rapport du Cycle n°$cycleNumber", color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.Black)
                    }
                },
                actions = {
                    IconButton(onClick = { /* PDF */ }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ReportHeader(
                    tontineName = state.tontine?.libelle ?: "",
                    caisseName = state.caisse?.libelle ?: "",
                    cycleNumber = state.currentCycle?.numeroCycle ?: 1,
                    date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                )

                Spacer(Modifier.height(24.dp))

                ReportTable(state.reportRows)
            }
        }
    }
}

@Composable
fun ReportHeader(tontineName: String, caisseName: String, cycleNumber: Int, date: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (tontineName.isNotEmpty()) {
            Text(
                text = tontineName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = "ETAT DES COTISATIONS - $caisseName",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Cycle n°$cycleNumber - Date: $date",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.Black)
    }
}

@Composable
fun ReportTable(rows: List<MemberReportRow>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = Color.LightGray,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableCell(text = "Membre", columnWeight = 0.25f, isHeader = true)
                TableCell(text = "Dû (Cycle)", columnWeight = 0.15f, isHeader = true)
                TableCell(text = "Payé (Cycle)", columnWeight = 0.15f, isHeader = true)
                TableCell(text = "Manquant", columnWeight = 0.15f, isHeader = true)
                TableCell(text = "Historique Total", columnWeight = 0.2f, isHeader = true)
                TableCell(text = "Statut", columnWeight = 0.1f, isHeader = true)
            }
        }

        rows.forEach { row ->
            ReportRow(row)
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun ReportRow(row: MemberReportRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableCell(text = row.memberName, columnWeight = 0.25f)
        TableCell(text = "${row.totalACotiser} F", columnWeight = 0.15f)
        TableCell(text = "${row.cotiseCeCycle} F", columnWeight = 0.15f)
        TableCell(text = "${row.manquant} F", columnWeight = 0.15f)
        TableCell(text = "${row.dejaCotise} F", columnWeight = 0.2f)
        TableCell(
            text = row.status, 
            columnWeight = 0.1f, 
            color = if (row.status == "À JOUR") Color(0xFF2E7D32) else Color.Red
        )
    }
}

@Composable
fun RowScope.TableCell(
    text: String, 
    columnWeight: Float, 
    isHeader: Boolean = false,
    color: Color = Color.Black
) {
    Text(
        text = text,
        modifier = Modifier.weight(columnWeight),
        style = if (isHeader) MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold) 
                else MaterialTheme.typography.bodySmall,
        color = color,
        textAlign = TextAlign.Start,
        maxLines = 1
    )
}
