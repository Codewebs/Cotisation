package com.indiza.cotisation.bank

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.tontine.TontineMemberWithUser
import com.indiza.cotisation.tontinemembers.ParticipantWithParts
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.GlassWhite
import com.indiza.cotisation.tools.NeonCyan
import com.indiza.cotisation.tools.SwitchColor1
import com.indiza.cotisation.tools.TextColor1
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun TransactionScreen(
    caisse: CaisseEntity,
    participant: ParticipantWithParts,
    onDismiss: () -> Unit,
    cycleId: Long,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val montantAverser = (caisse.montantFixe ?: 0L) * participant.nombreParts

    // On initialise le champ de saisie avec ce résultat
    var montantSaisi by remember { mutableStateOf(montantAverser.toString()) }

    val detteMembre by viewModel.getEtatDetteMembre(caisse.id, participant.memberWithUser.member.id).collectAsState(initial = 0L)

    val jourLimite = caisse.dateLimiteJour ?: 31
    val estEnRetard = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) > jourLimite
    var doitAppliquerPenalite by remember { mutableStateOf(estEnRetard) }

    var appliquerRetard by remember { mutableStateOf(estEnRetard) }
    var appliquerEchec by remember { mutableStateOf(false) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    val base = (caisse.montantFixe ?: 0L) * participant.nombreParts
    val penaliteRetard = if (appliquerRetard) (caisse.penaliteRetardFixe ?: 0L) else 0L
    val penaliteEchec = if (appliquerEchec) (caisse.penaliteEchecFixe ?: 0L) else 0L
    val totalGlobal = base + penaliteRetard + penaliteEchec

// Animation d'échelle pour le check vert
    val scale by animateFloatAsState(
        targetValue = if (isSuccess) 1.2f else 0.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "SuccessAnimation"
    )

    CosmicBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(42.dp).background(NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            participant.memberWithUser.member.nomSaisi.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            participant.memberWithUser.member.nomSaisi,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Cotisation : ${caisse.libelle}",
                            color = GlassWhite,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("État des dettes", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${detteMembre} FCFA",
                                color = if (detteMembre < 0) Color.Red else Color.Green,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Jour limite", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            Text("Le $jourLimite du mois", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = AfroGold, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${participant.nombreParts} part${if (participant.nombreParts > 1) "s" else ""} à cotiser",
                                color = AfroGold,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Montant par part", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            Text("${caisse.montantFixe} FCFA", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("MONTANT À ENCAISSER", style = MaterialTheme.typography.labelMedium, color = NeonCyan, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = montantSaisi,
                    onValueChange = { if (it.all { char -> char.isDigit() }) montantSaisi = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, color = NeonCyan),
                    suffix = { Text("FCFA", color = NeonCyan, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = NeonCyan,
                        unfocusedTextColor = Color.White
                    ),
                    trailingIcon = {
                        if (montantSaisi != montantAverser.toString()) {
                            IconButton(onClick = { montantSaisi = montantAverser.toString() }) {
                                Icon(Icons.Default.Refresh, "Réinitialiser", tint = AfroGold, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                )

                Spacer(Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (caisse.penaliteRetardFixe != null) {
                        PenaltyItem(
                            label = "Pénalité de retard",
                            labelColor = SwitchColor1,
                            montant = caisse.penaliteRetardFixe,
                            checked = appliquerRetard,
                            onCheckedChange = { appliquerRetard = it },
                            isWarning = estEnRetard
                        )
                    }

                    if (caisse.penaliteEchecFixe != null) {
                        PenaltyItem(
                            label = "Pénalité d'échec (non-versement)",
                            labelColor = SwitchColor1,
                            montant = caisse.penaliteEchecFixe,
                            checked = appliquerEchec,
                            onCheckedChange = { appliquerEchec = it },
                            isWarning = false
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text("RÉSUMÉ À PERCEVOIR", style = MaterialTheme.typography.labelMedium, color = Color.Gray, fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(8.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cotisation (${participant.nombreParts} parts)", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text("$base FCFA", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }

                        if (appliquerRetard) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Pénalité de retard", color = Color.Red.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                                Text("+ $penaliteRetard FCFA", color = Color.Red.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (appliquerEchec) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Pénalité d'échec", color = Color.Red.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                                Text("+ $penaliteEchec FCFA", color = Color.Red.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Divider(Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL À ENCAISSER", fontWeight = FontWeight.Bold, color = NeonCyan, style = MaterialTheme.typography.labelMedium)
                            Text(
                                "$totalGlobal FCFA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonCyan
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.enregistrerNonCotisation(
                                caisse = caisse,
                                memberId = participant.memberWithUser.member.id,
                                cycleId = cycleId,
                                appliquerPenalite = doitAppliquerPenalite
                            )
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.8f))
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Le membre n'a pas encore cotisé pour le cycle", style = MaterialTheme.typography.labelMedium)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Color.Gray)
                        ) {
                            Text("ANNULER", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                        Button(
                            onClick = {
                                val m = montantSaisi.toLongOrNull() ?: 0L
                                if (m > 0) {
                                    showConfirmDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text("VALIDER", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    containerColor = Color(0xFF1A1A1A),
                    title = {
                        Text("Confirmer la transaction", color = Color.White)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Voulez-vous enregistrer cette cotisation ?", color = GlassWhite)

                            if (appliquerRetard || appliquerEchec) {
                                Surface(
                                    color = Color.Red.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(8.dp)) {
                                        Text("Pénalités incluses :", color = Color.Red, style = MaterialTheme.typography.labelMedium)
                                        if (appliquerRetard) Text("• Retard: ${caisse.penaliteRetardFixe} FCFA", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                        if (appliquerEchec) Text("• Échec: ${caisse.penaliteEchecFixe} FCFA", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            Text(
                                "Total final : $totalGlobal FCFA",
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (isProcessing) return@Button
                                isProcessing = true
                                val m = montantSaisi.toLongOrNull() ?: 0L
                                viewModel.enregistrerCotisation(
                                    caisse = caisse,
                                    memberId = participant.memberWithUser.member.id,
                                    montant = m,
                                    appliquerRetard = appliquerRetard,
                                    appliquerEchec = appliquerEchec,
                                    cycleId = cycleId
                                ) { result ->
                                    isProcessing = false
                                    if (result.isSuccess) {
                                        isSuccess = true
                                    } else {
                                        android.widget.Toast.makeText(context, result.exceptionOrNull()?.message ?: "Erreur", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                    showConfirmDialog = false
                                }
                            },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text(if (isProcessing) "Traitement..." else "Confirmer", color = Color.Black)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = false }) {
                            Text("Annuler", color = Color.White)
                        }
                    }
                )
            }

            if (isSuccess) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable(enabled = false) {}
                        .zIndex(10f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(110.dp).scale(scale),
                            shape = CircleShape,
                            color = Color.Green.copy(alpha = 0.2f),
                            border = BorderStroke(4.dp, Color.Green)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Green,
                                modifier = Modifier.padding(18.dp).fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            "Transaction enregistrée !",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.scale(scale)
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    delay(1500)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
fun PenaltyItem(
    label: String,
    montant: Long,
    checked: Boolean,
    labelColor: Color = Color.LightGray,
    onCheckedChange: (Boolean) -> Unit,
    isWarning: Boolean
) {
    Surface(
        color = if (checked) Color.Red.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (checked) Color.Red.copy(alpha = 0.4f) else Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = labelColor)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = labelColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text("${montant} FCFA", color = if (checked) Color.Red else Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
            if (isWarning && !checked) {
                Icon(Icons.Default.Warning, "Attention", tint = AfroGold, modifier = Modifier.size(16.dp))
            }
        }
    }
}

