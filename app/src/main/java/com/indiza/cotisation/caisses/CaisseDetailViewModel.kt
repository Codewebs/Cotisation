package com.indiza.cotisation.caisses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.bank.CycleDao
import com.indiza.cotisation.bank.CycleEntity
import com.indiza.cotisation.bank.TourDao
import com.indiza.cotisation.bank.TourEntity
import com.indiza.cotisation.bank.TransactionDao
import com.indiza.cotisation.sync.SyncStatus
import com.indiza.cotisation.tontine.TontineDao
import com.indiza.cotisation.tontine.TontineEntity
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.*
import javax.inject.Inject

data class CaisseDetailUiState(
    val caisse: CaisseEntity? = null,
    val tontine: TontineEntity? = null,
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
    val toursDetailCycle: List<com.indiza.cotisation.bank.TourWithBeneficiaires> = emptyList(),
    val partsEncaisseesMap: Map<Long, Int> = emptyMap(),
    val globalPartsEncaisseesMap: Map<Long, Int> = emptyMap(),
    val partsEncaisseesJusquiciMap: Map<Long, Int> = emptyMap(),
    val participantsPlanifies: List<ParticipantWithParts> = emptyList(),
    val ordreFige: Boolean = false,
    val totalAmendes: Long = 0L,
    val totalPenalites: Long = 0L,
    val montantDistribue: Long = 0L,
    val montantRecolte: Long = 0L,
    val beneficiairesDuCycle: List<com.indiza.cotisation.bank.BeneficiaireWithMember> = emptyList(),
    val reportRows: List<MemberReportRow> = emptyList(),
) {
    val beneficiaireActuel: ParticipantWithParts?
        get() = participants.find { p ->
            val memberId = p.memberWithUser.member.id
            val dejaRecuGlobal = partsEncaisseesJusquiciMap[memberId] ?: 0
            val dejaPlanifieCycle = participantsPlanifies.sumOf { if (it.member.id == memberId) it.nombreParts else 0 }
            (dejaRecuGlobal + dejaPlanifieCycle) < p.nombreParts
        }

    val cycleTermine: Boolean
        get() = currentCycle?.estCloture == true || 
                (participants.isNotEmpty() && toursDuCycle.isNotEmpty() && toursDuCycle.all { it.estTermine })

    val isReadOnly: Boolean
        get() = currentCycle?.estCloture == true

    val peutCloturerCycle: Boolean
        get() = currentCycle != null &&
                !currentCycle.estCloture &&
                dejaCotise.size >= participants.size &&
                participants.isNotEmpty() &&
                (toursDuCycle.isEmpty() || toursDuCycle.all { it.estTermine })

    val peutAjouterTour: Boolean
        get() = currentCycle != null &&
                !currentCycle.estCloture &&
                soldeGlobal >= (caisse?.montantFixe ?: 0L)

    val peutCloturerTour: Boolean
        get() = currentCycle != null && !currentCycle.estCloture && toursDuCycle.any { !it.estTermine }
}

@HiltViewModel
class CaisseDetailViewModel @Inject constructor(
    private val caisseDao: CaisseDao,
    private val tontineMemberDao: TontineMemberDao,
    private val tontineDao: TontineDao,
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

        val caisse = caisseDao.getCaisseById(caisseId)
        if (caisse == null) {
            _state.update { it.copy(isLoading = false) }
            return@launch
        }
        val tontine = tontineDao.getById(caisse.tontineId)
        _state.update { it.copy(caisse = caisse, tontine = tontine, ordreFige = caisse.ordreFige) }

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

        launch {
            _currentCycle.collect { cycle ->
                _state.update { it.copy(currentCycle = cycle) }
                if (cycle != null) {
                    loadReportRows(caisseId, cycle.id)
                }
            }
        }

        launch {
            tontineMemberDao.getParticipantsWithPartsByCaisse(caisseId).collect { list ->
                _state.update { it.copy(participants = list) }
            }
        }

        launch {
            tontineMemberDao.getTontineMembersWithUsers(caisse.tontineId).collect { members ->
                _state.update { it.copy(allTontineMembers = members) }
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
            transactionDao.getSoldeGlobal(caisseId).collect { solde ->
                _state.update { it.copy(soldeGlobal = solde ?: 0L) }
            }
        }

        launch {
            transactionDao.getTotalPretsEnCours(caisseId).collect { total ->
                _state.update { it.copy(totalPrets = total ?: 0L) }
            }
        }

        launch {
            cycleDao.getAllCycles(caisseId).collect { cycles ->
                _state.update { it.copy(allCycles = cycles) }
            }
        }

        launch {
            _currentCycle.flatMapLatest { cycle ->
                if (cycle == null) kotlinx.coroutines.flow.flowOf(emptyList<TourEntity>())
                else tourDao.getToursParCycle(cycle.id)
            }.collect { tours ->
                _state.update { it.copy(toursDuCycle = tours) }
            }
        }

        launch {
            _currentCycle.flatMapLatest { cycle ->
                if (cycle == null) kotlinx.coroutines.flow.flowOf(emptyList())
                else tourDao.getToursWithBeneficiairesParCycle(cycle.id)
            }.collect { tours ->
                val allBens = tours.flatMap { it.beneficiaires }
                _state.update { it.copy(toursDetailCycle = tours, beneficiairesDuCycle = allBens) }
            }
        }

        launch {
            _currentCycle.flatMapLatest { cycle ->
                if (cycle == null) kotlinx.coroutines.flow.flowOf(0L)
                else transactionDao.getSommeParTypeEtCycle(caisseId, cycle.id, com.indiza.cotisation.bank.TransactionType.PENALITE_RETARD)
            }.collect { total ->
                _state.update { it.copy(totalAmendes = total ?: 0L) }
            }
        }

        launch {
            _currentCycle.flatMapLatest { cycle ->
                if (cycle == null) kotlinx.coroutines.flow.flowOf(0L)
                else transactionDao.getSommeParTypeEtCycle(caisseId, cycle.id, com.indiza.cotisation.bank.TransactionType.PENALITE_ECHEC)
            }.collect { total ->
                _state.update { it.copy(totalPenalites = total ?: 0L) }
            }
        }

        launch {
            _currentCycle.flatMapLatest { cycle ->
                if (cycle == null) kotlinx.coroutines.flow.flowOf(0L)
                else transactionDao.getSommeParTypeEtCycle(caisseId, cycle.id, com.indiza.cotisation.bank.TransactionType.COTISATION)
            }.collect { total ->
                _state.update { it.copy(montantRecolte = total ?: 0L) }
            }
        }

        launch {
            _currentCycle.flatMapLatest { cycle ->
                if (cycle == null) kotlinx.coroutines.flow.flowOf(0L)
                else transactionDao.getMontantDistribueCycle(caisseId, cycle.id)
            }.collect { total ->
                _state.update { it.copy(montantDistribue = total ?: 0L) }
            }
        }

        _state.update { it.copy(isLoading = false) }
    }

    private fun loadReportRows(caisseId: Long, cycleId: Long) = viewModelScope.launch {
        val participants = tontineMemberDao.getParticipantsWithPartsByCaisse(caisseId).first()
        val caisse = caisseDao.getCaisseById(caisseId)
        val cycle = cycleDao.getCycleById(cycleId) ?: return@launch
        val montantFixe = caisse?.montantFixe ?: 0L

        val rows = coroutineScope {
            participants.map { p ->
                async {
                    val memberId = p.member.id
                    val cibleCeCycle = p.nombreParts * montantFixe
                    val dejaCotiseJusqueIci = transactionDao.getSommeCotisJusqueCycle(caisseId, memberId, cycle.numeroCycle) ?: 0L
                    val cotiseCeCycle = transactionDao.getSommeCotiseeParMembreDuCycle(caisseId, cycleId, memberId).first() ?: 0L

                    MemberReportRow(
                        memberName = p.member.nomSaisi,
                        totalACotiser = cibleCeCycle,
                        dejaCotise = dejaCotiseJusqueIci,
                        manquant = if (cibleCeCycle > cotiseCeCycle) cibleCeCycle - cotiseCeCycle else 0L,
                        cotiseCeCycle = cotiseCeCycle,
                        status = if (cotiseCeCycle >= cibleCeCycle) "À JOUR" else "RETARD"
                    )
                }
            }.awaitAll()
        }
        _state.update { it.copy(reportRows = rows) }
    }

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
            syncStatus = SyncStatus.PENDING_UPDATE 
        )

        caisseDao.updateCaisse(updatedCaisse)
    }

    fun moveParticipant(fromIndex: Int, toIndex: Int) {
        val list = _state.value.participants.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _state.update { it.copy(participants = list) }
    }

    fun toggleExclusion(memberId: Long) {
        val current = _idsExclus.value.toMutableSet()
        if (current.contains(memberId)) current.remove(memberId)
        else current.add(memberId)
        _idsExclus.value = current
    }

    fun reinitialiserExclusions() {
        _idsExclus.value = emptySet()
    }


    fun selectCycle(cycle: CycleEntity) {
        _currentCycle.value = cycle
        _state.update { it.copy(isViewingOldCycle = cycle.estCloture) }
    }

    fun selectCycleById(id: Long) = viewModelScope.launch {
        val c = cycleDao.getCycleById(id)
        if (c != null) selectCycle(c)
    }

    fun shuffleParticipants() {
        val list = _state.value.participants.shuffled()
        _state.update { it.copy(participants = list) }
    }

    suspend fun createFirstCycle(caisseId: Long): CycleEntity {
        val c = CycleEntity(caisseId = caisseId, numeroCycle = 1)
        val id = cycleDao.insertCycle(c)
        return c.copy(id = id)
    }
    fun getCycle(id: Long): Flow<CycleEntity?> {
        return cycleDao.getCycleParId(id)
    }

    fun toggleOrdreFige() = viewModelScope.launch {
        val caisse = _state.value.caisse ?: return@launch
        val newValue = !caisse.ordreFige
        caisseDao.updateCaisse(caisse.copy(ordreFige = newValue))
        _state.update { it.copy(ordreFige = newValue) }
    }

    fun saveNewOrder(participants: List<ParticipantWithParts>) = viewModelScope.launch {
        participants.forEachIndexed { index, p ->
            tontineMemberDao.updateParticipantOrder(caisseId, p.member.id, index)
        }
        
        val caisse = _state.value.caisse ?: return@launch
        caisseDao.updateCaisse(caisse.copy(ordreFige = true))
        _state.update { it.copy(ordreFige = true, participants = participants) }
    }

    fun terminerTour(tourId: Long) = viewModelScope.launch {
        tourDao.markTourAsTermine(tourId)
    }

    fun toggleParticipant(memberId: Long, estPlanifie: Boolean, parts: Int) = viewModelScope.launch {
        val cycle = _currentCycle.value ?: return@launch
        if (estPlanifie) {
            val tourId = tourDao.insertTour(TourEntity(cycleId = cycle.id, numeroTour = 0))
            tourDao.insertBeneficiaire(
                com.indiza.cotisation.bank.TourBeneficiaireEntity(
                    tourId = tourId,
                    memberId = memberId,
                    nombrePartsAttribuees = parts,
                    montantRecu = 0 
                )
            )
        } else {
            tourDao.deleteToursByMemberAndCycle(memberId, cycle.id)
        }
    }

    fun toggleParticipantCaisse(memberId: Long, selected: Boolean, parts: Int) = viewModelScope.launch {
        if (selected) {
            caisseDao.addParticipantToCaisse(CaisseParticipantEntity(caisseId, memberId, 0, false, parts))
        } else {
            caisseDao.removeParticipantFromCaisse(caisseId, memberId)
        }
    }

    fun toggleAllParticipants(selected: Boolean) = viewModelScope.launch {
        if (selected) {
            _state.value.allTontineMembers.forEachIndexed { index, member ->
                val alreadyExists = _state.value.participants.any { it.member.id == member.member.id }
                if (!alreadyExists) {
                    caisseDao.addParticipantToCaisse(CaisseParticipantEntity(caisseId, member.member.id, index + 1, false, 1))
                }
            }
        } else {
            // Uniquement ceux qui n'ont pas encore bouffé
            _state.value.participants.forEach { p ->
                val partsDejaTouchees = _state.value.partsEncaisseesMap[p.member.id] ?: 0
                if (partsDejaTouchees == 0) {
                    caisseDao.removeParticipantFromCaisse(caisseId, p.member.id)
                }
            }
        }
    }
}
