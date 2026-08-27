package com.indiza.cotisation.tontine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject


@HiltViewModel
class EditTontineViewModel @Inject constructor(
    private val repository: TontineRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel()
{

    private var tontineId: Long = -1

    private val _state = MutableStateFlow(CreateTontineUiState())
    val state = _state.asStateFlow()

    private val _updateSuccess = MutableSharedFlow<Boolean>()
    val updateSuccess = _updateSuccess.asSharedFlow()

    init {
        loadTontineData()
    }
    fun initTontine(id: Long) {
        if (tontineId != -1L) return // Évite de recharger si déjà fait
        tontineId = id
        loadTontineData()
    }
    fun onAnonymousChange(enabled: Boolean) {
        _state.update { it.copy(isAnonymous = enabled) }
    }
    private fun loadTontineData() = viewModelScope.launch {
        repository.getTontineWithCaisses(tontineId)
            .filterNotNull() // <--- Crucial : Ignore si l'ID n'existe pas encore ou est invalide
            .collect { data ->
                // Ici "data" est garanti non-nul par filterNotNull()
                _state.update { it.copy(
                    libelle = data.tontine.libelle,
                    exercice = data.tontine.exercice ?: "",
                    dateDebut = data.tontine.dateDebut,
                    dateFin = data.tontine.dateFin
                ) }
            }
    }

    fun onLibelleChange(v: String) = _state.update { it.copy(libelle = v) }
    fun onExerciceChange(v: String) = _state.update { it.copy(exercice = v) }
    fun onDateDebutChange(d: LocalDate) = _state.update { it.copy(dateDebut = d, dateFin = d.plusYears(1)) }
    fun onDateFinChange(d: LocalDate) = _state.update { it.copy(dateFin = d) }

    fun update() = viewModelScope.launch {
        val s = _state.value
        if (s.libelle.isBlank() || s.exercice.isBlank()) {
            _state.update { it.copy(error = "Veuillez remplir tous les champs") }
            return@launch
        }

        try {
            repository.updateTontineBaseInfo(
                tontineId = tontineId,
                libelle = s.libelle,
                exercice = s.exercice,
                dateDebut = s.dateDebut,
                dateFin = s.dateFin
            )
            _updateSuccess.emit(true)
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
        }
    }
}