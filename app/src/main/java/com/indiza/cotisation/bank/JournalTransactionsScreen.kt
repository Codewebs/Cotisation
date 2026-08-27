package com.indiza.cotisation.bank

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.DeepDarkViolet
import com.indiza.cotisation.tools.GlassWhite
import com.indiza.cotisation.tools.NeonCyan
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun JournalTransactionsScreen(
    caisseId: Long,
    viewModel: TransactionViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val transactions by viewModel.getJournalTransactions(caisseId).collectAsState(initial = emptyList())

    CosmicBackground {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Text(
                    "JOURNAL DES OPÉRATIONS",
                    style = MaterialTheme.typography.titleMedium,
                    color = AfroGold,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Aucune transaction",
                        color = GlassWhite.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(transactions) { tx ->
                        TransactionRow(tx)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(item: TransactionWithMember) {
    val tx = item.transaction
    val member = item.member
    val isPositive = tx.montant > 0

    val color = when (tx.type) {
        TransactionType.COTISATION -> NeonCyan
        TransactionType.REMBOURSEMENT -> Color.Green
        TransactionType.PENALITE_ECHEC -> AfroGold
        TransactionType.PENALITE_RETARD -> DeepDarkViolet
        TransactionType.PRET, TransactionType.SORTIE_SECOURS -> Color.Red
        else -> Color.White
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101827).copy(alpha = 0.85f)),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.24f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp, 42.dp)
                    .background(color, RoundedCornerShape(999.dp))
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.nomSaisi,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = color.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tx.type.name.take(4),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = color,
                            fontSize = 9.sp
                        )
                    }
                }

                Text(
                    text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(tx.dateTransaction),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )

                tx.motif?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isPositive) "+" else ""}${tx.montant} F",
                    fontWeight = FontWeight.Black,
                    color = if (isPositive) Color.White else color,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "FCFA",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 8.sp
                )
            }
        }
    }
}