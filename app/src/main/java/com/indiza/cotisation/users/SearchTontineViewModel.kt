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

    private val _searchResults = MutableStateFlow<List<TontineEntity>>(emptyList())
    val searchResults: StateFlow<List<TontineEntity>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

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

    fun clearSearch() {
        _searchResults.value = emptyList()
        _errorMessage.value = null
    }
}