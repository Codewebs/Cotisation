package com.indiza.cotisation.tontine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import com.indiza.cotisation.tools.DatePickerField
import com.indiza.cotisation.tools.NeonCyan


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTontineScreen(
    viewModel: EditTontineViewModel = hiltViewModel(),
    tontineId: Long,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    // On initialise le ViewModel dès que l'écran s'affiche
    LaunchedEffect(tontineId) {
        viewModel.initTontine(tontineId)
    }

    LaunchedEffect(Unit) {
        viewModel.updateSuccess.collect { if (it) onBack() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Modifier la tontine") }) },
        bottomBar = {
            Button(
                onClick = viewModel::update,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Enregistrer les modifications") }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = state.libelle,
                onValueChange = viewModel::onLibelleChange,
                label = { Text("Nom de la tontine") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.exercice,
                onValueChange = viewModel::onExerciceChange,
                label = { Text("Exercice (Libellé spécial)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatePickerField(
                    label = "Début",
                    selectedDate = state.dateDebut,
                    onDateSelected = viewModel::onDateDebutChange,
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    label = "Fin",
                    selectedDate = state.dateFin,
                    onDateSelected = viewModel::onDateFinChange,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(24.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Mode Anonyme",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Les membres ne pourront pas voir les noms des autres adhérents.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.isAnonymous,
                        onCheckedChange = { viewModel.onAnonymousChange(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}