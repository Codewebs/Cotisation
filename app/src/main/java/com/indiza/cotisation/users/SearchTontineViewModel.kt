package com.indiza.cotisation.users

import android.os.Build
import androidx.annotation.RequiresApi
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
    private val tontineRepository: TontineRepository,
    private val syncManager: com.indiza.cotisation.sync.SyncManager
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

    fun refreshSearch(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                syncManager.triggerSync()
                val results = tontineRepository.searchTontines(query)
                _searchResults.value = results
                _joinStatus.value = "Actualisé en local et en ligne"
            } catch (e: Exception) {
                _errorMessage.value = "Erreur actualisation: ${e.message}"
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
                searchTontines(query)
            } catch (e: Exception) {
                _errorMessage.value = "Erreur adhésion: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelJoinRequest(tontineRemoteId: String, query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                tontineRepository.cancelJoinTontine(tontineRemoteId)
                _joinStatus.value = "Demande retirée avec succès"
                searchTontines(query)
            } catch (e: Exception) {
                _errorMessage.value = "Erreur retrait: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
        _errorMessage.value = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun openOrJoinDefaultTontine(onOpen: (TontineEntity) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _joinStatus.value = null
            try {
                val results = tontineRepository.searchTontines("Les amis du dimanche")
                val existing = results.filter {
                    it.tontine.libelle.equals("Les amis du dimanche", ignoreCase = true) &&
                        it.tontine.remoteId != null
                }.maxByOrNull { it.memberCount }

                if (existing != null) {
                    val status = existing.membershipStatus
                    if (status == "MEMBER") {
                        onOpen(existing.tontine)
                    } else if (status == "PENDING") {
                        _joinStatus.value = "Votre demande d'adhésion pour « Les amis du dimanche » est en attente de validation."
                    } else if (existing.memberCount == 0L) {
                        val claimed = tontineRepository.claimEmptyDefaultTontine(existing.tontine)
                        if (claimed != null) {
                            onOpen(claimed)
                        } else {
                            tontineRepository.requestJoinTontine(existing.tontine.remoteId!!)
                            _joinStatus.value = "Demande d'adhésion envoyée à « Les amis du dimanche ». En attente de validation."
                        }
                    } else {
                        tontineRepository.requestJoinTontine(existing.tontine.remoteId!!)
                        _joinStatus.value = "Demande d'adhésion envoyée à « Les amis du dimanche ». En attente de validation."
                    }
                } else {
                    _errorMessage.value = "La réunion par défaut est introuvable. Veuillez réessayer plus tard."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erreur: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
