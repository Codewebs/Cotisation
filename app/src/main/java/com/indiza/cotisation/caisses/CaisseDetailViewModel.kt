package com.indiza.cotisation.caisses

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.bank.CycleDao
import com.indiza.cotisation.bank.CycleEntity
import com.indiza.cotisation.bank.TourDao
import com.indiza.cotisation.bank.TourEntity
import com.indiza.cotisation.bank.TransactionDao
import com.indiza.cotisation.sync.SyncStatus
import com.indiza.cotisation.tontine.TontineMemberDao
import com.indiza.cotisation.tontine.TontineMemberWithUser
import com.indiza.cotisation.tontinemembers.CaisseParticipantEntity
import com.indiza.cotisation.tontinemembers.ParticipantWithParts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

data class CaisseDetailUiState(
    val caisse: CaisseEntity? = null,
    val participants: List<ParticipantWithParts> = emptyList(),
    val currentCycle: CycleEntity? = null,
    val allTontineMembers: List<TontineMemberWithUser> = emptyList(),
    val dejaBeneficiaires: List<Long> = emptyList(),
    val dejaCotise: List<Long> = emptyList(),
    val toursCompletes: Int = 0,
    val soldeGlobal: Long = 0L,
    val soldeDuCycle: Long = 0L,
    val soldePrecedent: Long = 0L,
    val totalPrets: Long = 0L,
    val allCycles: List<CycleEntity> = emptyList(),
    val isViewingOldCycle: Boolean = false,
    val isLoading: Boolean = true,
    val toursDuCycle: List<TourEntity> = emptyList(),
    val partsEncaisseesMap: Map<Long, Int> = emptyMap(),
    val participantsPlanifies: List<ParticipantWithParts> = emptyList(),
) {
    val beneficiaireActuel: ParticipantWithParts?
        get() = participants.getOrNull(toursCompletes)

    val cycleTermine: Boolean
        get() = participants.isNotEmpty() && toursCompletes >= participants.size

    val isReadOnly: Boolean
        get() = currentCycle?.estCloture == true

    val peutCloturerCycle: Boolean
        get() = currentCycle != null &&
                !currentCycle.estCloture &&
                System.currentTimeMillis() >= currentCycle.dateDebut &&
                dejaCotise.size >= participants.size &&
                participants.isNotEmpty()

    val peutAjouterTour: Boolean
        get() = currentCycle != null &&
                !currentCycle.estCloture &&
                soldeGlobal >= (caisse?.montantFixe ?: 0L)

    val peutCloturerTour: Boolean
        get() = toursDuCycle.any { !it.estTermine }
}

@HiltViewModel
class CaisseDetailViewModel @Inject constructor(
    private val caisseDao: CaisseDao,
    private val tontineMemberDao: TontineMemberDao,
    private val transactionDao: TransactionDao,
    val cycleDao: CycleDao,
    val tourDao: TourDao,
    savedStateHandle: SavedStateHandle
) : ViewModel()
{

    private val _idsExclus = MutableStateFlow<Set<Long>>(emptySet())
    val idsExclus = _idsExclus.asStateFlow()

    private val requestedCycleId: Long? = savedStateHandle["cycleId"]

    private val _state = MutableStateFlow(CaisseDetailUiState())
    val state: StateFlow<CaisseDetailUiState> = _state.asStateFlow()

    private val caisseId: Long = checkNotNull(savedStateHandle["caisseId"])

    private val _currentCycle = MutableStateFlow<CycleEntity?>(null)

    init {
        loadData(caisseId)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun loadData(caisseId: Long) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }

        // ÉTAPE 1 : Récupérer la caisse
        val caisse = caisseDao.getCaisseById(caisseId)
        if (caisse == null) {
            _state.update { it.copy(isLoading = false) }
            return@launch
        }
        _state.update { it.copy(caisse = caisse) }

        // ÉTAPE 2 : Gérer le cycle actif ou demandé
        val cycleInitial = if (requestedCycleId != null) {
            cycleDao.getCycleById(requestedCycleId)
        } else {
            cycleDao.getCycleActifSnapshot(caisseId)
        }

        if (cycleInitial == null) {
            val newCycleId = createFirstCycle(caisseId).id
            _currentCycle.value = cycleDao.getCycleById(newCycleId)
        } else {
            _currentCycle.value = cycleInitial
        }

        // ÉTAPE 3 : Observations réactives dépendantes du cycle
        launch {
            _currentCycle.collect { cycle ->
                _state.update { it.copy(currentCycle = cycle) }
            }
        }

        launch {
            _currentCycle.flatMapLatest { cycle ->
                if (cycle == null) kotlinx.coroutines.flow.flowOf(emptyList())
                else tourDao.getParticipantsPlanifiesDuCycle(cycle.id)
            }.collect { list ->
                _state.update { it.copy(participantsPlanifies = list) }
            }
        }

        launch {
            _currentCycle.flatMapLatest { cycle ->
                if (cycle == null) kotlinx.coroutines.flow.flowOf(emptyMap())
                else tourDao.getPartsEncaisseesParMembreDuCycle(cycle.id).map { list ->
                    list.associate { it.memberId to it.count }
                }
            }.collect { map ->
                _state.update { it.copy(partsEncaisseesMap = map) }
            }
        }

        launch {
            _currentCycle.flatMapLatest { cycle ->
                if (cycle == null) kotlinx.coroutines.flow.flowOf(emptyList())
                else transactionDao.getCotisantsDuCycle(caisseId, cycle.id)
            }.collect { ids ->
                _state.update { it.copy(dejaCotise = ids) }
            }
        }

        launch {
            _currentCycle.flatMapLatest { cycle ->
                if (cycle == null) kotlinx.coroutines.flow.flowOf(0L)
                else transactionDao.getSoldeDuCycle(caisseId, cycle.id)
            }.collect { solde ->
                _state.update { it.copy(soldeDuCycle = solde ?: 0L) }
            }
        }

        launch {
            _currentCycle.flatMapLatest { cycle ->
                if (cycle == null) kotlinx.coroutines.flow.flowOf(0L)
                else transactionDao.getSoldeCyclesPrecedents(caisseId, cycle.numeroCycle)
            }.collect { solde ->
                _state.update { it.copy(soldePrecedent = solde ?: 0L) }
            }
        }

        launch {
            _currentCycle.flatMapLatest { cycle ->
                if (cycle == null) kotlinx.coroutines.flow.flowOf(emptyList())
                else tourDao.getToursParCycle(cycle.id)
            }.collect { tours ->
                _state.update { it.copy(toursDuCycle = tours) }
            }
        }

        // ÉTAPE 4 : Observations indépendantes du cycle
        launch {
            tontineMemberDao.getParticipantsWithPartsByCaisse(caisseId).collect { list ->
                _state.update { it.copy(participants = list) }
            }
        }

        launch {
            transactionDao.getCountToursCompletes(caisseId).collect { count ->
                _state.update { it.copy(toursCompletes = count) }
            }
        }

        launch {
            tontineMemberDao.getParticipantsByCaisse(caisseId).collect { list ->
                _state.update { it.copy(participants = list) }
            }
        }
        launch {
            cycleDao.getAllCycles(caisseId).collect { cycles ->
                _state.update { it.copy(allCycles = cycles) }
            }
        }

        launch {
            tontineMemberDao.getTontineMembersWithUsers(caisse.tontineId).collect { allMembers ->
                _state.update { it.copy(allTontineMembers = allMembers, isLoading = false) }
            }
        }
        // Observation du Solde Global
        launch {
            transactionDao.getSoldeGlobal(caisseId).collect { solde ->
                _state.update { it.copy(soldeGlobal = solde ?: 0L) }
            }
        }
        launch {
            transactionDao.getBeneficiairesCagnotte(caisseId).collect { ids ->
                _state.update { it.copy(dejaBeneficiaires = ids) }
            }
        }
        // Observation des Prêts
        launch {
            transactionDao.getTotalPretsEnCours(caisseId).collect { total ->
                // Note: les prêts sont enregistrés en négatif dans la table transactions
                // donc on prend l'opposé pour l'affichage "Prêts en cours"
                _state.update { it.copy(totalPrets = -(total ?: 0L)) }
            }
        }
    }

    // Dans CaisseDetailViewModel.kt
    fun updateCaisse(draft: CaisseDraft) = viewModelScope.launch {
        val currentCaisse = _state.value.caisse ?: return@launch

        val updatedCaisse = currentCaisse.copy(
            libelle = draft.libelle,
            montantFixe = draft.montantFixe,
            montantMinimum = draft.montantMinimum,
            frequence = draft.frequence,
            redistribution = draft.redistribution,
            possibilitePret = draft.possibilitePret,
            tauxInteret = draft.tauxInteret,
            penaliteRetardFixe = draft.penaliteRetardFixe,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_UPDATE // Très important pour le mode Offline First
        )

        caisseDao.updateCaisse(updatedCaisse)
        // Le Flow dans loadData mettra automatiquement l'UI à jour
    }

    fun moveParticipant(fromIndex: Int, toIndex: Int) {
        val list = _state.value.participants.toMutableList()
        if (toIndex in list.indices && fromIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _state.update { it.copy(participants = list) }
            saveNewOrder(list)
        }
    }

    fun toggleExclusion(memberId: Long) {
        _idsExclus.value = if (_idsExclus.value.contains(memberId)) {
            _idsExclus.value - memberId
        } else {
            _idsExclus.value + memberId
        }
    }

    fun reinitialiserExclusions() {
        _idsExclus.value = emptySet()
    }


    fun selectCycle(cycle: CycleEntity) {
        _currentCycle.value = cycle
        _state.update { it.copy(isViewingOldCycle = !cycle.estCloture) }
    }

    fun selectCycleById(cycleId: Long) = viewModelScope.launch {
        val cycle = cycleDao.getCycleById(cycleId) ?: return@launch
        selectCycle(cycle)
    }

    fun shuffleParticipants() {
        val shuffled = _state.value.participants.shuffled()
        _state.update { it.copy(participants = shuffled) }
        saveNewOrder(shuffled)
    }

    private suspend fun createFirstCycle(caisseId: Long): CycleEntity {
        val cycle = CycleEntity(caisseId = caisseId, numeroCycle = 1)
        val id = cycleDao.insertCycle(cycle)
        return cycle.copy(id = id)
    }
    fun getCycle(cycleId: Long): Flow<CycleEntity?> {
        return cycleDao.getCycleParId(cycleId)
    }

    private fun saveNewOrder(newList: List<ParticipantWithParts>) = viewModelScope.launch {
        try {
            val caisseId = _state.value.caisse?.id ?: return@launch

            newList.forEachIndexed { index, participant ->
                tontineMemberDao.updateParticipantOrder(
                    caisseId = caisseId,
                    memberId = participant.memberWithUser.member.id,
                    nouveauOrdre = index + 1 // L'ordre commence à 1
                )
            }
        } catch (e: Exception) {
            // Optionnel : Gérer l'erreur (ex: Snack-bar "Erreur de sauvegarde")
        }
    }

    fun toggleParticipant(memberId: Long, isSelected: Boolean, nombreParts: Int = 1) = viewModelScope.launch {
        val caisseId = _state.value.caisse?.id ?: return@launch
        if (isSelected) {
            val existingOrder = _state.value.participants.find { it.memberWithUser.member.id == memberId }?.memberWithUser?.member?.id
            val order = if (existingOrder != null) {
                _state.value.participants.indexOfFirst { it.memberWithUser.member.id == memberId } + 1
            } else {
                _state.value.participants.size + 1
            }
            caisseDao.addParticipantToCaisse(
                CaisseParticipantEntity(
                    caisseId = caisseId,
                    memberId = memberId,
                    ordrePassage = order,
                    nombreParts = nombreParts
                )
            )
        } else {
            caisseDao.removeParticipantFromCaisse(caisseId, memberId)
        }
    }
}
