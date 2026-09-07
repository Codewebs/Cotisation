package com.indiza.cotisation.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.indiza.cotisation.tools.NeonCyan
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
    helper: String? = null,
    description: String? = null
) {
    var showInfo by remember { mutableStateOf(false) }

    if (showInfo && description != null) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(label, color = color, fontWeight = FontWeight.Bold) },
            text = { Text(description, color = Color.White) },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("COMPRIS", color = NeonCyan)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = description != null) { showInfo = true },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.85f)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = color)
                if (description != null) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = color.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(value, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            if (helper != null) {
                Text(helper, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicReportHeader(
    header: ReportHeaderState,
    onCycleSelected: ((Long) -> Unit)? = null
) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val periode = if (header.dateDebut != null) {
        val start = sdf.format(Date(header.dateDebut))
        val end = if (header.dateFin != null) " au " + sdf.format(Date(header.dateFin)) else " (En cours)"
        "Du $start$end"
    } else ""

    var showCyclePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = header.caisseName.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${header.tontineName} • Cycle n°${header.cycleNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            if (onCycleSelected != null && header.allCycles.size > 1) {
                Box {
                    IconButton(onClick = { showCyclePicker = true }) {
                        Icon(Icons.Default.UnfoldMore, "Changer de cycle", tint = NeonCyan)
                    }
                    DropdownMenu(
                        expanded = showCyclePicker,
                        onDismissRequest = { showCyclePicker = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        header.allCycles.forEach { cycle ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        "Cycle ${cycle.numeroCycle}${if (cycle.id == header.currentCycleId) " (Actuel)" else ""}",
                                        color = if (cycle.id == header.currentCycleId) NeonCyan else Color.White
                                    ) 
                                },
                                onClick = {
                                    onCycleSelected(cycle.id)
                                    showCyclePicker = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (header.exercice.isNotEmpty()) {
            Text(
                text = "Exercice : ${header.exercice}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        if (periode.isNotEmpty()) {
            Text(
                text = periode,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = Color.White.copy(alpha = 0.1f))
    }
}
