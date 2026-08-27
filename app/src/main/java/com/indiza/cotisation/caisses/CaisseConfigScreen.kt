package com.indiza.cotisation.caisses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.indiza.cotisation.tontine.FrequenceSelector
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.NeonCyan

@Composable
fun CaisseConfigScreen(
    typeInitial: CaisseType,
    existingCaisse: CaisseEntity? = null,
    onSave: (CaisseDraft) -> Unit,
    onCancel: () -> Unit
) {


    var draft by remember {
        mutableStateOf(
            existingCaisse?.let {
                CaisseDraft(
                    libelle = it.libelle,
                    type = it.type,
                    montantFixe = it.montantFixe,
                    montantMinimum = it.montantMinimum,
                    frequence = it.frequence,
                    redistribution = it.redistribution,
                    possibilitePret = it.possibilitePret,
                    tauxInteret = it.tauxInteret,
                    penaliteRetardFixe = it.penaliteRetardFixe,
                    penaliteEchecFixe = it.penaliteEchecFixe,
                    dateLimiteJour = it.dateLimiteJour
                )
            } ?: initialiserCaisseParType(typeInitial)
        )
    }

    // --- LOGIQUE DE VALIDATION ---
    val isLibelleValid = draft.libelle.isNotBlank()

    // Au moins un des deux montants doit être renseigné
    val isMontantValid = (draft.montantFixe != null && draft.montantFixe!! > 0) ||
            (draft.montantMinimum != null && draft.montantMinimum!! > 0)

    // Si c'est une cotisation simple, la durée est obligatoire
    val isDureeValid = if (typeInitial == CaisseType.COTISATION_SIMPLE) {
        draft.dureeCycles != null && draft.dureeCycles!! > 0
    } else true

    // Si prêt activé, le taux est obligatoire
    val isTauxValid = if (draft.possibilitePret) {
        draft.tauxInteret != null && draft.tauxInteret!! >= 0
    } else true

    val isFormValid = isLibelleValid && isMontantValid && isDureeValid && isTauxValid

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (existingCaisse == null) "Nouvelle Caisse" else "Modifier : ${draft.libelle}",
                style = MaterialTheme.typography.titleMedium,
                color = NeonCyan
            )
            Text(
                "Configuration : ${draft.type}",
                style = MaterialTheme.typography.titleMedium,
                color = NeonCyan
            )

            Spacer(Modifier.height(12.dp))

            // --- SECTION DE BASE ---
            OutlinedTextField(
                value = draft.libelle,
                onValueChange = { draft = draft.copy(libelle = it) },
                label = { Text("Nom de la caisse", style = MaterialTheme.typography.bodySmall) },
                isError = !isLibelleValid,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            // --- MONTANT ---
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft.montantFixe?.toString() ?: "",
                    onValueChange = {
                        draft = draft.copy(
                            montantFixe = it.toLongOrNull(),
                            montantMinimum = null
                        )
                    },
                    label = { Text("Montant fixe", style = MaterialTheme.typography.bodySmall) },
                    isError = !isMontantValid,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = draft.montantMinimum?.toString() ?: "",
                    onValueChange = {
                        draft = draft.copy(
                            montantMinimum = it.toLongOrNull(),
                            montantFixe = null
                        )
                    },
                    label = { Text("Minimum", style = MaterialTheme.typography.bodySmall) },
                    isError = !isMontantValid,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            if (!isMontantValid) {
                Text("Veuillez saisir un montant", color = Color.Red, style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(12.dp))

            // --- PARAMÈTRES BINAIRES ---
            SwitchOption(
                title = "Redistribution",
                subtitle = "Un membre reçoit la cagnotte à chaque tour",
                checked = draft.redistribution,
                enabled = draft.type != CaisseType.COTISATION_SIMPLE,
                onCheckedChange = { draft = draft.copy(redistribution = it) }
            )

            SwitchOption(
                title = "Possibilité de prêt",
                subtitle = "Autoriser les emprunts",
                checked = draft.possibilitePret,
                enabled = draft.type != CaisseType.PRET,
                onCheckedChange = { draft = draft.copy(possibilitePret = it) }
            )

            // --- OPTIONS PRÊT ---
            if (draft.possibilitePret) {
                Spacer(Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = NeonCyan.copy(alpha = 0.05f)
                    )
                ) {
                    Column(Modifier.padding(10.dp)) {

                        Text(
                            "Paramètres du crédit",
                            style = MaterialTheme.typography.titleSmall,
                            color = NeonCyan
                        )

                        Spacer(Modifier.height(6.dp))

                        OutlinedTextField(
                            value = draft.tauxInteret?.toString() ?: "",
                            onValueChange = {
                                draft = draft.copy(tauxInteret = it.toDoubleOrNull())
                            },
                            label = { Text("Taux (%)", style = MaterialTheme.typography.bodySmall) },
                            isError = !isTauxValid,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- PÉNALITÉS ---
            Text(
                "Pénalités de retard",
                style = MaterialTheme.typography.titleSmall,
                color = AfroGold
            )

            Spacer(Modifier.height(4.dp))

            // 2. Montant Retard
            OutlinedTextField(
                value = draft.penaliteRetardFixe?.toString() ?: "",
                onValueChange = { draft = draft.copy(penaliteRetardFixe = it.toLongOrNull()) },
                label = { Text("Amende de retard (FCFA)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.AccessTime, null, tint = Color.Red) }
            )

            // 3. Montant Échec (Non-cotisation)
            OutlinedTextField(
                value = draft.penaliteEchecFixe?.toString() ?: "",
                onValueChange = { draft = draft.copy(penaliteEchecFixe = it.toLongOrNull()) },
                label = { Text("Amende d'échec / Non-cotisation (FCFA)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.Block, null, tint = Color.Red) }
            )

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = draft.dateLimiteJour?.toString() ?: "",
                onValueChange = { draft = draft.copy(dateLimiteJour = it.toIntOrNull()?.coerceIn(1, 31)) },
                label = { Text("Jour limite de paiement (1-31)") },
                placeholder = { Text("Ex: 5 (pour le 5 du mois)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.height(20.dp))
            if (typeInitial == CaisseType.COTISATION_SIMPLE) {
                Text("Temporalité", color = NeonCyan, style = MaterialTheme.typography.labelMedium)

                // Sélection Fréquence
                FrequenceSelector(
                    selected = draft.frequence,
                    onSelected = { draft = draft.copy(frequence = it) }
                )

                // Saisie de la durée
                OutlinedTextField(
                    value = draft.dureeCycles?.toString() ?: "",
                    onValueChange = { draft = draft.copy(dureeCycles = it.toIntOrNull()) },
                    label = { Text("Nombre de ${draft.frequence.name}") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // AFFICHAGE INTERACTIF DE LA CONVERSION
                draft.dureeCycles?.let { d ->
                    val conversion = draft.frequence.getConversionLabel(d)
                    if (conversion.isNotEmpty()) {
                        Surface(
                            color = NeonCyan.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
                        ) {
                            Text(
                                text = "Équivaut à environ : $conversion",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isFormValid) Color.Black else Color.Gray,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onSave(draft) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Text(
                    text = if (existingCaisse == null) "CRÉER LA CAISSE" else "ENREGISTRER LES MODIFICATIONS",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}


@Composable
fun SwitchOption(title: String, subtitle: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = NeonCyan, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}