package com.indiza.cotisation.tontine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.caisses.CaisseDao
import com.indiza.cotisation.users.Role
import com.indiza.cotisation.users.SessionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


enum class TontineSortOrder {
    DATE_DESC, // Plus récentes d'abord
    NAME_ASC,  // Alphabétique
    CAISSE_COUNT // Par nombre de caisses
}
@HiltViewModel
class TontineListViewModel @Inject constructor(
    private val tontineRepository: TontineRepository,
    private val sessionDao: SessionDao,
    private val tontineDao: TontineDao,
    private val tontineMemberDao: TontineMemberDao,
    private val syncManager: com.indiza.cotisation.sync.SyncManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime = _lastSyncTime.asStateFlow()

    // État de la recherche
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _deleteError = MutableSharedFlow<String>()
    val deleteError = _deleteError.asSharedFlow()

    private val _sortOrder = MutableStateFlow(TontineSortOrder.DATE_DESC)
    val sortOrder = _sortOrder.asStateFlow()

    private val _onlineTontines = MutableStateFlow<List<TontineWithCaissesAndRole>>(emptyList())
    val onlineTontines = _onlineTontines.asStateFlow()

    // Le Flow unique : il récupère les tontines locales (isOnline=0) 
    // et les combine avec les tontines récupérées du serveur en temps réel
    val tontines: StateFlow<List<TontineWithCaissesAndRole>> = combine(
        tontineDao.getLocalTontinesWithCaisses(),
        _onlineTontines,
        _searchQuery,
        _sortOrder
    ) { localList, onlineList, query, sort ->
        val userId = sessionDao.getCurrentSession()?.userId
        
        val localWithRole = localList.map { item ->
            val memberRole = tontineMemberDao.getUserRoleInTontine(item.tontine.id, userId ?: "") 
                ?: Role.INITIATEUR

            TontineWithCaissesAndRole(
                tontine = item.tontine,
                caisses = item.caisses,
                role = memberRole
            )
        }

        (localWithRole + onlineList).filter {
            it.tontine.libelle.contains(query, ignoreCase = true)
        }.sortedWith(
            when (sort) {
                TontineSortOrder.DATE_DESC -> compareByDescending { it.tontine.dateDebut }
                TontineSortOrder.NAME_ASC -> compareBy { it.tontine.libelle.lowercase() }
                TontineSortOrder.CAISSE_COUNT -> compareByDescending { it.caisses.size }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            sessionDao.hasActiveSessionFlow().collect { active ->
                if (active) {
                    refreshTontines()
                }
            }
        }
        loadLastSyncTime()
    }

    private fun loadLastSyncTime() {
        val prefs = context.getSharedPreferences("sync_prefs", android.content.Context.MODE_PRIVATE)
        _lastSyncTime.value = prefs.getLong("last_sync_time", 0L).takeIf { it > 0L }
    }

    fun refreshTontines() = viewModelScope.launch {
        try {
            syncManager.triggerSync()
            // Pour le mode Duo : on récupère les tontines distantes et on met à jour le StateFlow
            val remoteTontines = tontineRepository.fetchMyOnlineTontines()
            _onlineTontines.value = remoteTontines
            
            loadLastSyncTime()
        } catch (e: Exception) {
            // En cas d'erreur (serveur offline), on vide la liste des tontines en ligne
            // pour ne garder que les locales (Duo Mode)
            _onlineTontines.value = emptyList()
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSortOrderChange(newOrder: TontineSortOrder) {
        _sortOrder.value = newOrder
    }

    fun deleteTontine(tontineId: Long) = viewModelScope.launch {
        val result = tontineRepository.deleteTontineIfEmpty(tontineId)
        result.onFailure { error ->
            _deleteError.emit(error.message ?: "Erreur lors de la suppression")
        }
    }
}
