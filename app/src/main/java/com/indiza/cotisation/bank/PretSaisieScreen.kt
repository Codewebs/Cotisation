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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.tontinemembers.ParticipantWithParts
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.NeonCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PretSaisieScreen(
    caisse: CaisseEntity,
    participant: ParticipantWithParts,
    viewModel: TransactionViewModel,
    onDismiss: () -> Unit,
    cycleId: Long
) {
    var montantPret by remember { mutableStateOf("") }
    var moisDiffere by remember { mutableStateOf(0) }
    var showDiffereOption by remember { mutableStateOf(false) }
    
    var selectedOriginCaisseId by remember { mutableStateOf<Long?>(null) }
    val allCaisses by viewModel.caisseDao.getCaissesByTontine(caisse.tontineId).collectAsState(initial = emptyList())
    var showOriginSelector by remember { mutableStateOf(false) }

    CosmicBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Accorder un Prêt", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // --- HEADER ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(participant.memberWithUser.member.nomSaisi.take(1), style = MaterialTheme.typography.headlineSmall)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(participant.memberWithUser.member.nomSaisi, style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text("Caisse : ${caisse.libelle}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(32.dp))

                // --- SAISIE MONTANT ---
                Text("MONTANT DU PRÊT", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                OutlinedTextField(
                    value = montantPret,
                    onValueChange = { if (it.all { char -> char.isDigit() }) montantPret = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                    suffix = { Text("FCFA", color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                Spacer(Modifier.height(24.dp))

                // --- OPTIONS ORIGINE DES FONDS ---
                if (caisse.possibilitePret) {
                    Text("ORIGINE DES FONDS (OPTIONNEL)", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showOriginSelector = true },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = selectedOriginCaisseId?.let { id -> allCaisses.find { it.id == id }?.libelle } ?: "Caisse actuelle (${caisse.libelle})",
                                color = Color.White
                            )
                        }
                        
                        androidx.compose.material3.DropdownMenu(
                            expanded = showOriginSelector,
                            onDismissRequest = { showOriginSelector = false },
                            modifier = Modifier.background(Color(0xFF1A1A1A))
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Caisse actuelle (${caisse.libelle})", color = Color.White) },
                                onClick = { 
                                    selectedOriginCaisseId = null
                                    showOriginSelector = false 
                                }
                            )
                            allCaisses.filter { it.id != caisse.id }.forEach { c ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(c.libelle, color = Color.White) },
                                    onClick = {
                                        selectedOriginCaisseId = c.id
                                        showOriginSelector = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // --- OPTIONS DIFFÉRÉ ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showDiffereOption, onCheckedChange = { showDiffereOption = it })
                            Text("Appliquer un différé de paiement", color = Color.White)
                        }

                        if (showDiffereOption) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                Text("Durée : ", color = Color.Gray)
                                IconButton(onClick = { if (moisDiffere > 0) moisDiffere-- }) {
                                    Icon(Icons.Default.Remove, contentDescription = null, tint = NeonCyan)
                                }
                                Text("$moisDiffere mois", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = { moisDiffere++ }) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan)
                                }
                            }
                            Text(
                                "Les remboursements ne commenceront qu'après cette période.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(32.dp))

                // --- ACTIONS ---
                Button(
                    onClick = {
                        val montant = montantPret.toLongOrNull() ?: 0L
                        if (montant > 0) {
                            viewModel.enregistrerPret(
                                caisseId = caisse.id,
                                memberId = participant.memberWithUser.member.id,
                                montant = montant,
                                cycleId = cycleId,
                                moisDiffere = if (showDiffereOption) moisDiffere else 0,
                                originCaisseId = selectedOriginCaisseId,
                                onSuccess = onDismiss
                            )
                        }
                    },
                    enabled = montantPret.isNotEmpty() && (montantPret.toLongOrNull() ?: 0L) > 0,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("CONFIRMER LE PRÊT", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("ANNULER", color = Color.Gray)
                }
            }
        }
    }
}
