package com.indiza.cotisation.users

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                            Spacer(Modifier.height(16.dp))
                            Text("REÇU DE PAIEMENT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.Black)
                            Text("Validation Numérique", fontSize = 10.sp, color = Color.Gray)

                            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = Color.Gray.copy(alpha = 0.1f))

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

                            Spacer(Modifier.height(24.dp))

                            DetailRow("Caisse", tx.Caisse?.libelle ?: "---")
                            DetailRow("Type", tx.type)
                            DetailRow("Motif", tx.motif ?: "Cotisation régulière")

                            Spacer(Modifier.height(32.dp))

                            // Signature Section
                            Surface(
                                color = NeonCyan.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "CERTIFICATION NUMÉRIQUE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        tx.validator?.nom?.uppercase() ?: "SYSTÈME COTISATION",
                                        fontSize = 14.sp,
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

                            Spacer(Modifier.height(32.dp))
                            
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
fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label.uppercase(), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End)
    }
}
