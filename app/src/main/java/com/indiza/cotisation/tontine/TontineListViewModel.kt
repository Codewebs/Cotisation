package com.indiza.cotisation.tontine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.caisses.CaisseDao
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
    private val tontineDao: TontineDao
) : ViewModel() {

    // État de la recherche
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val currentUserId: String? = runBlocking {
        sessionDao.getCurrentSession()?.userId
    }

    private val _sortOrder = MutableStateFlow(TontineSortOrder.DATE_DESC)
    val sortOrder = _sortOrder.asStateFlow()

    private val userTontinesFlow: Flow<List<TontineWithCaissesAndRole>> =
        flow {
            val userId = sessionDao.getCurrentSession()?.userId ?: return@flow
            emitAll(tontineRepository.getUserTontinesWithRole(userId))
        }


    private val _deleteError = MutableSharedFlow<String>()
    val deleteError = _deleteError.asSharedFlow()

    val tontines2: StateFlow<List<TontineWithCaissesAndRole>> =
        combine(
            userTontinesFlow,
            _searchQuery,
            _sortOrder
        ) { list, query, sort ->

            list
                .filter {
                    it.tontine.libelle.contains(query, ignoreCase = true)
                }
                .sortedWith(
                    when (sort) {
                        TontineSortOrder.DATE_DESC ->
                            compareByDescending { it.tontine.dateDebut }

                        TontineSortOrder.NAME_ASC ->
                            compareBy { it.tontine.libelle.lowercase() }

                        TontineSortOrder.CAISSE_COUNT ->
                            compareByDescending { it.caisses.size }
                    }
                )
        }
            .stateIn(
                viewModelScope,
                SharingStarted.Lazily,
                emptyList()
            )


    // Le Flow combiné : il réagit dès que la DB change, que la recherche change ou que le tri change
    val tontines: StateFlow<List<TontineWithCaisses>> = combine(
        tontineDao.getAllTontinesWithCaisses(),
        _searchQuery,
        _sortOrder
    ) { list, query, sort ->
        list.filter {
            it.tontine.libelle.contains(query, ignoreCase = true)
        }.sortedWith(
            when (sort) {
                TontineSortOrder.DATE_DESC -> compareByDescending { it.tontine.dateDebut }
                TontineSortOrder.NAME_ASC -> compareBy { it.tontine.libelle.lowercase() }
                TontineSortOrder.CAISSE_COUNT -> compareByDescending { it.caisses.size }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
