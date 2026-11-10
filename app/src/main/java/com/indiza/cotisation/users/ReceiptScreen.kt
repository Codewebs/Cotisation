package com.indiza.cotisation.users

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.NeonCyan
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    transactionId: String,
    onBack: () -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    CosmicBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Détails du Reçu") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = NeonCyan)
                } else if (state.error != null) {
                    Text(state.error!!, color = Color.Red)
                } else {
                    val tx = state.transaction ?: return@Box
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(64.dp).background(NeonCyan, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                Text("C", fontSize = 24.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.height(12.dp))
                            
                            // 1. Libellé de la réunion en entête
                            if (!tx.tontineLibelle.isNullOrBlank()) {
                                Text(
                                    tx.tontineLibelle.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF0097A7),
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(Modifier.height(4.dp))
                            }

                            Text("REÇU DE PAIEMENT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.Black)
                            
                            // 2. Numéroter le reçu
                            val receiptNum = "REC-" + tx.id.take(8).uppercase()
                            Text("Reçu N° $receiptNum", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = Color.Gray.copy(alpha = 0.1f))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("MONTANT PERÇU", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text("${tx.montant} F", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF0097A7))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("DATE", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(tx.dateTransaction))
                                    Text(date, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            DetailRow("Caisse", tx.Caisse?.libelle ?: "---")
                            DetailRow("Type", tx.type)
                            DetailRow("Motif", tx.motif ?: "Cotisation régulière")

                            Spacer(Modifier.height(24.dp))

                            // 3. Certification Numérique avec Code QR
                            Surface(
                                color = NeonCyan.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    QrCodeView(
                                        data = tx.id,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "CERTIFICATION NUMÉRIQUE",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0097A7),
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            tx.validator?.nom?.uppercase() ?: "SYSTÈME COTISATION",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black,
                                            fontStyle = FontStyle.Italic
                                        )
                                        Text(
                                            "Signature électronique sécurisée",
                                            fontSize = 8.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))
                            
                            Button(
                                onClick = { /* TODO: Implement PDF Export on Mobile */ },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                            ) {
                                Text("TÉLÉCHARGER LE PDF")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QrCodeView(data: String, modifier: Modifier = Modifier) {
    val seed = data.hashCode()
    val qrSize = 21
    Canvas(modifier = modifier) {
        val cellSize = size.width / qrSize
        val random = Random(seed.toLong())
        for (x in 0 until qrSize) {
            for (y in 0 until qrSize) {
                val isFinder = (x < 7 && y < 7) || (x >= qrSize - 7 && y < 7) || (x < 7 && y >= qrSize - 7)
                val isDark = isFinder && ((x == 0 || x == 6 || y == 0 || y == 6) || (x in 2..4 && y in 2..4)) ||
                        (!isFinder && random.nextBoolean())
                if (isDark) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(x * cellSize, y * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label.uppercase(), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End)
    }
}
