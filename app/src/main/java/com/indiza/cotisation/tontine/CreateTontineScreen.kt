package com.indiza.cotisation.tontine

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.caisses.CaisseItem
import com.indiza.cotisation.caisses.CaisseType

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.indiza.cotisation.caisses.CaisseConfigScreen
import com.indiza.cotisation.caisses.CaisseDraft
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.AfroOrange
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.DatePickerField
import com.indiza.cotisation.tools.DeepViolet
import com.indiza.cotisation.tools.NeonCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTontineScreen(
    viewModel: CreateTontineViewModel = hiltViewModel(),
    onCancel: () -> Unit,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    var showTypeSelection by remember { mutableStateOf(false) }
    var selectedTypeForConfig by remember { mutableStateOf<CaisseType?>(null) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect { success ->
            if (success) {
                onSuccess() // Navigue vers la liste
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    BoxWithConstraints {
        // --- 1. CAPTURE DES CONTRAINTES ---
        val screenWidth = maxWidth  // On stocke la largeur ici
        val isSmallScreen = screenWidth < 360.dp
        val horizontalPadding = if (isSmallScreen) 12.dp else 16.dp

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle Tontine", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.heightIn(max = 60.dp), // Réduit pour gagner de l'espace
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                TextButton(onClick = onCancel) { Text("Annuler") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { viewModel.save() },
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Enregistrer", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.05f), // Effet verre sombre
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Nouvelle Tontine",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = state.libelle,
                    onValueChange = viewModel::onLibelleChange,
                    label = { Text("Libellé de la tontine") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.exercice,
                    onValueChange = viewModel::onExerciceChange,
                    label = { Text("Libellé de l'exercice (ex: 2024-2025)") },
                    placeholder = { Text("Pour distinguer des années précédentes") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(16.dp))

                if (screenWidth < 400.dp) {
                    // Mode vertical pour petits écrans
                    DatePickerField("Début", state.dateDebut, viewModel::onDateDebutChange, Modifier.fillMaxWidth())
                    DatePickerField("Fin", state.dateFin, viewModel::onDateFinChange, Modifier.fillMaxWidth())
                } else {
                    // Mode horizontal pour écrans larges
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DatePickerField("Début", state.dateDebut, viewModel::onDateDebutChange, Modifier.weight(1f))
                        DatePickerField("Fin", state.dateFin, viewModel::onDateFinChange, Modifier.weight(1f))
                    }
                }


                // Mode de la Tontine (Online / Offline)
                Text("Type de Tontine", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModeCard(
                        modifier = Modifier.weight(1f),
                        title = "EN LIGNE",
                        description = "Synchronisée, multi-appareils",
                        isSelected = state.isOnline,
                        onClick = { viewModel.onOnlineChange(true) }
                    )
                    ModeCard(
                        modifier = Modifier.weight(1f),
                        title = "HORS LIGNE",
                        description = "Locale, ce téléphone uniquement",
                        isSelected = !state.isOnline,
                        onClick = { viewModel.onOnlineChange(false) }
                    )
                }

                Spacer(Modifier.height(24.dp))

                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
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
                                color = Color.White
                            )
                            Text(
                                "Les membres ne pourront pas voir les noms réels.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
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

                Spacer(Modifier.height(24.dp))
                Text("Caisses", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                state.caisses.forEach { caisse ->
                    CaisseItem(
                        caisse = caisse,
                        onRemove = { viewModel.removeCaisse(caisse.id) })
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showTypeSelection = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ajouter une caisse") }

                if (showTypeSelection) {
                    TypeSelectionDialog(
                        onTypeSelected = { type ->
                            selectedTypeForConfig = type
                            showTypeSelection = false
                        },
                        onDismiss = { showTypeSelection = false }
                    )
                }

// ÉTAPE 2 : Configuration complète (Plein écran ou BottomSheet)
                if (selectedTypeForConfig != null) {
                    Dialog(
                        onDismissRequest = { selectedTypeForConfig = null },
                        properties = DialogProperties(usePlatformDefaultWidth = false) // Pour le plein écran
                    ) {
                        CaisseConfigScreen(
                            typeInitial = selectedTypeForConfig!!,
                            onSave = { draft ->
                                viewModel.addCaisse(draft)
                                selectedTypeForConfig = null
                            },
                            onCancel = { selectedTypeForConfig = null }
                        )
                    }
                }
            }
        }
    }
    }
}


@Composable
fun ModeCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        color = if (isSelected) NeonCyan.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) NeonCyan else Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun TypeSelectionDialog(
    onTypeSelected: (CaisseType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quel type de caisse ajouter ?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CaisseType.values().forEach { type ->
                    OutlinedButton(
                        onClick = { onTypeSelected(type) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(type.getDisplayName())
                    }
                }
            }
        },
        confirmButton = {}
    )
}

fun CaisseType.getDisplayName(): String {
    return when (this) {
        CaisseType.COTISATION_SIMPLE -> "Cotisation simple"
        CaisseType.SECOURS -> "Caisse de secours"
        CaisseType.DEVELOPPEMENT -> "Caisse de développement"
        CaisseType.PRET -> "Caisse de prêt"
        CaisseType.FONCTIONNEMENT -> "Caisse de fonctionnement"
        CaisseType.PERSONNALISEE -> "Caisse personnalisée"
    }
}