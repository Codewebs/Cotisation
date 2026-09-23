package com.indiza.cotisation.caisses

import androidx.compose.foundation.background
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
                    cycleNumber = state.currentCycle?.numeroCycle ?: cycleNumber,
                    date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                    soldeInitial = state.soldePrecedent,
                    soldeFinal = state.soldePrecedent + state.montantRecolte
                )

                Spacer(Modifier.height(24.dp))

                ReportTable(state.reportRows)
            }
        }
    }
}

@Composable
fun ReportHeader(tontineName: String, caisseName: String, cycleNumber: Int, date: String, soldeInitial: Long, soldeFinal: Long) {
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
        
        Spacer(Modifier.height(8.dp))
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            HeaderStat("Solde Initial", "$soldeInitial F")
            HeaderStat("Collecté (Cycle)", "${soldeFinal - soldeInitial} F")
            HeaderStat("Solde Final", "$soldeFinal F")
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color.Black)
    }
}

@Composable
fun HeaderStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.Black)
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
                TableCell(text = "Membre", columnWeight = 0.2f, isHeader = true)
                TableCell(text = "Date/Heure", columnWeight = 0.2f, isHeader = true)
                TableCell(text = "Payé", columnWeight = 0.12f, isHeader = true)
                TableCell(text = "Manquant", columnWeight = 0.12f, isHeader = true)
                TableCell(text = "Solde Caisse", columnWeight = 0.18f, isHeader = true)
                TableCell(text = "Statut", columnWeight = 0.18f, isHeader = true)
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
    val backgroundColor = if (row.isBeneficiaire) NeonCyan.copy(alpha = 0.1f) else Color.Transparent
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableCell(
            text = row.memberName + if(row.isBeneficiaire) " 🏆" else "", 
            columnWeight = 0.2f,
            color = if(row.isBeneficiaire) Color.Blue else Color.Black,
            fontWeight = if(row.isBeneficiaire) FontWeight.Bold else FontWeight.Normal
        )
        TableCell(text = row.dateCotisation ?: "-", columnWeight = 0.2f)
        TableCell(text = "${row.cotiseCeCycle} F", columnWeight = 0.12f)
        TableCell(text = "${row.manquant} F", columnWeight = 0.12f, color = if(row.manquant > 0) Color.Red else Color.Black)
        TableCell(text = "${row.soldeCaisseMoment} F", columnWeight = 0.18f, fontWeight = FontWeight.Bold)
        TableCell(
            text = row.status, 
            columnWeight = 0.18f, 
            color = if (row.status == "À JOUR") Color(0xFF2E7D32) else Color.Red
        )
    }
}

@Composable
fun RowScope.TableCell(
    text: String, 
    columnWeight: Float, 
    isHeader: Boolean = false,
    color: Color = Color.Black,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        modifier = Modifier.weight(columnWeight),
        style = if (isHeader) MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold) 
                else MaterialTheme.typography.bodySmall.copy(fontWeight = fontWeight),
        color = color,
        textAlign = TextAlign.Start,
        maxLines = 2,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
    )
}
