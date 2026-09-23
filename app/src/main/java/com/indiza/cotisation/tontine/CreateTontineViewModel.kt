package com.indiza.cotisation.tontine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.caisses.CaisseDraft
import com.indiza.cotisation.caisses.CaisseType
import dagger.hilt.android.lifecycle.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CreateTontineViewModel @Inject constructor(
    private val repository: TontineRepository
) : ViewModel()
{

    private val _state = MutableStateFlow(CreateTontineUiState())
    val state = _state.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Boolean>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    fun onLibelleChange(value: String) {
        _state.update { it.copy(libelle = value) }
    }
    fun onExerciceChange(value: String) {
        _state.update { it.copy(exercice = value) }
    }

    fun onDateDebutChange(date: LocalDate) {
        _state.update {
            it.copy(
                dateDebut = date,
                dateFin = date.plusYears(1) // Ajustement automatique
            )
        }
    }
    fun onAnonymousChange(enabled: Boolean) {
        _state.update { it.copy(isAnonymous = enabled) }
    }
    fun onOnlineChange(enabled: Boolean) {
        _state.update { it.copy(isOnline = enabled) }
    }
    fun onDateFinChange(date: LocalDate) {
        _state.update { it.copy(dateFin = date) }
    }

    fun addCaisse(caisse: CaisseDraft) {
        val currentState = _state.value

        // Vérifier si une caisse de même type existe déjà
        val typeExists = currentState.caisses.any { it.type == caisse.type }

        if (typeExists) {
            // Déclencher l'erreur dans l'UI
            _state.update { it.copy(error = "Ce type de caisse (${caisse.type.name}) existe déjà.") }
            return
        }

        _state.update {
            it.copy(
                caisses = it.caisses + caisse,
                error = null // On efface l'erreur si l'ajout réussit
            )
        }
    }

    // Fonction pour effacer l'erreur manuellement (ex: après affichage d'une Snackbar)
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun removeCaisse(id: String) {
        _state.update { it.copy(caisses = it.caisses.filterNot { it.id == id }) }
    }

    // Dans CreateTontineViewModel.kt
    fun save() = viewModelScope.launch {
        val currentState = _state.value
        if (currentState.libelle.isBlank()) {
            _state.update { it.copy(error = "Le libellé de la tontine est obligatoire") }
            return@launch
        }

        if (currentState.caisses.isEmpty()) {
            _state.update { it.copy(error = "Veuillez ajouter au moins une caisse") }
            return@launch
        }
        if (currentState.exercice.isBlank()) {
            _state.update { it.copy(error = "Le libellé de l'exercice (ex: 2024) est requis") }
            return@launch
        }

        _state.update { it.copy(isSaving = true) }

        try {
            // 0. Vérification des doublons (Même nom pour le même initiateur)
            if (repository.isTontineNameDuplicate(currentState.libelle)) {
                _state.update { it.copy(error = "Vous avez déjà créé une tontine portant ce nom (${currentState.libelle})", isSaving = false) }
                return@launch
            }

            // 1. On crée uniquement la tontine
            val tontineId = repository.insertTontine(
                libelle = currentState.libelle,
                exercice = currentState.exercice,
                dateDebut = currentState.dateDebut,
                dateFin = currentState.dateFin,
                isOnline = currentState.isOnline,
                isAnonymous = currentState.isAnonymous
            )

            // 2. On insère toutes les caisses que l'utilisateur a ajoutées (y compris la cotisation si elle y est)
            currentState.caisses.forEach { caisseDraft ->
                repository.insertCaisse(tontineId, caisseDraft)
            }
            
            // 3. Déclencher la synchro une fois que tout est sauvé en local
            if (currentState.isOnline) {
                repository.triggerSync()
            }

            _saveSuccess.emit(true)

            // Logique de succès (navigation, etc.)
        } catch (e: Exception) {
            _state.update { it.copy(error = "Erreur lors de l'enregistrement: ${e.message}") }
        } finally {
            _state.update { it.copy(isSaving = false) }
        }
    }

}
