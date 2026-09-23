package com.indiza.cotisation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.tontine.TontineEntity
import com.indiza.cotisation.tontine.TontineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchTontineViewModel @Inject constructor(
    private val tontineRepository: TontineRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<com.indiza.cotisation.tontine.TontineSearchResult>>(emptyList())
    val searchResults: StateFlow<List<com.indiza.cotisation.tontine.TontineSearchResult>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _joinStatus = MutableStateFlow<String?>(null)
    val joinStatus: StateFlow<String?> = _joinStatus

    fun searchTontines(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val results = tontineRepository.searchTontines(query)
                _searchResults.value = results
            } catch (e: Exception) {
                _errorMessage.value = "Erreur de recherche: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun requestJoin(tontineRemoteId: String, query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                tontineRepository.requestJoinTontine(tontineRemoteId)
                _joinStatus.value = "Demande envoyée avec succès"
                // Rafraîchir les résultats pour mettre à jour le statut PENDING
                searchTontines(query)
            } catch (e: Exception) {
                _errorMessage.value = "Erreur adhésion: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
        _errorMessage.value = null
    }
}