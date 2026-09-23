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
import com.indiza.cotisation.users.Role
import com.indiza.cotisation.users.SessionDao
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
    val role: Role = Role.MEMBRE,
    val breakdown: List<com.indiza.cotisation.bank.CaisseBreakdown> = emptyList(),
    val soldeAmendes: Long = 0L,
    val soldeFraisCaisse: Long = 0L,
    val allTontineCaisses: List<CaisseEntity> = emptyList(),
    val cotisationsCumuleesMap: Map<Long, Long> = emptyMap(),
    val accessDenied: Boolean = false
) {
    val dejaBeneficiaires: List<Long>
        get() = partsEncaisseesJusquiciMap.keys.toList()

    val peutDupliquer: Boolean
        get() {
            val isClosed = caisse?.statut == CaisseStatus.CLOTUREE
            val dateFinStr = tontine?.dateFin?.toString() ?: ""
            if (!isClosed || dateFinStr.isEmpty()) return false
            if (role != Role.PRESIDENT && role != Role.INITIATEUR) return false

            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            return dateFinStr >= todayStr
        }

    val participantsAjour: List<Long>
        get() {
            val montantFixe = caisse?.montantFixe ?: 0L
            val nbParticipants = participants.size
            if (montantFixe == 0L || nbParticipants == 0) return emptyList()

            return participants.filter { p ->
                val maxPossible = p.nombreParts * nbParticipants * montantFixe
                val dejaCotise = cotisationsCumuleesMap[p.member.id] ?: 0L
                dejaCotise >= maxPossible
            }.map { it.member.id }
        }

    fun getMontantRestant(memberId: Long): Long {
        val participant = participants.find { it.member.id == memberId } ?: return 0L
        val montantFixe = caisse?.montantFixe ?: 0L
        val nbParticipants = participants.size
        val maxPossible = participant.nombreParts * nbParticipants * montantFixe
        val dejaCotise = cotisationsCumuleesMap[memberId] ?: 0L
        return if (maxPossible > dejaCotise) maxPossible - dejaCotise else 0L
    }

    val caisseTerminee: Boolean
        get() = caisse?.redistribution == true && 
                participants.isNotEmpty() && 
                participants.all { p -> 
                    val dejaRecu = partsEncaisseesJusquiciMap[p.member.id] ?: 0
                    dejaRecu >= p.nombreParts 
                }

    val peutCloturerDefinitivement: Boolean
        get() = currentCycle != null &&
                !currentCycle.estCloture &&
                caisseTerminee &&
                (soldeGlobal == 0L || role == Role.PRESIDENT)

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
                participants.isNotEmpty() &&
                (toursDuCycle.isEmpty() || toursDuCycle.all { it.estTermine }) &&
                dejaCotise.size >= participants.size


    val peutAjouterTour: Boolean
        get() = currentCycle != null &&
                !currentCycle.estCloture &&
                soldeGlobal >= (caisse?.montantFixe ?: 0L)

    val peutCloturerTour: Boolean
        get() = currentCycle != null && !currentCycle.estCloture && toursDuCycle.any { !it.estTermine }

    val peutAjouterMembres: Boolean
        get() {
            if (caisse == null) return false
            if (caisse.statut == CaisseStatus.CLOTUREE) return false
            // Dans une caisse à redistribution, on bloque si quelqu'un a déjà reçu
            if (caisse.redistribution && partsEncaisseesJusquiciMap.isNotEmpty()) return false
            return true
        }
}

@HiltViewModel
class CaisseDetailViewModel @Inject constructor(
    private val caisseDao: CaisseDao,
    private val tontineMemberDao: TontineMemberDao,
    private val tontineDao: TontineDao,
    private val transactionDao: TransactionDao,
    val cycleDao: CycleDao,
    val tourDao: TourDao,
    private val sessionDao: SessionDao,
    private val repository: com.indiza.cotisation.tontine.TontineRepository,
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

        val session = sessionDao.getCurrentSession()
        if (tontine?.isOnline == true && caisse.remoteId != null && session != null) {
            syncOnlineCaisseMembers(caisse)
        }
        
        loadLocalCaisseData(caisseId, caisse.tontineId)
    }

    private fun syncOnlineCaisseMembers(caisse: CaisseEntity) = viewModelScope.launch {
        println("DEBUG_CAISSE: syncOnlineCaisseMembers - Début synchro pour caisse ${caisse.id}")
        try {
            val remoteTontineId = _state.value.tontine?.remoteId ?: return@launch
            println("DEBUG_CAISSE:   - Récupération détails tontine en ligne: $remoteTontineId")
            val tontineDetails = repository.getOnlineTontineDetails(remoteTontineId)
            
            println("DEBUG_CAISSE:   - ${tontineDetails.members?.size ?: 0} membres trouvés en ligne. Synchro locale...")
            repository.syncOnlineTontineMembers(caisse.tontineId, tontineDetails.members.orEmpty())
            
            // Sync participants if online
            println("DEBUG_CAISSE:   - Récupération participants de la caisse ${caisse.remoteId}")
            val participants = repository.getOnlineParticipants(caisse.remoteId!!)
            println("DEBUG_CAISSE:   - ${participants.size} participants trouvés en ligne.")
            
            participants.forEach { p ->
                val member = tontineMemberDao.getMemberByRemoteId(caisse.tontineId, p.memberId)
                if (member != null) {
                    caisseDao.addParticipantToCaisse(CaisseParticipantEntity(
                        caisseId = caisse.id,
                        memberId = member.id,
                        ordrePassage = p.ordrePassage ?: 0,
                        nombreParts = p.nombreParts
                    ))
                } else {
                    println("DEBUG_CAISSE:   - Attention: Membre ${p.memberId} non trouvé localement après synchro.")
                }
            }
            println("DEBUG_CAISSE: syncOnlineCaisseMembers - Fin synchro réussie.")
        } catch (e: Exception) {
            println("DEBUG_CAISSE: syncOnlineCaisseMembers error: ${e.message}")
            if (e.message?.contains("403") == true || e.message?.contains("401") == true) {
                _state.update { it.copy(accessDenied = true) }
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun loadLocalCaisseData(caisseId: Long, tontineId: Long) = viewModelScope.launch {
        println("DEBUG_CAISSE: loadLocalCaisseData - Caisse ID: $caisseId")
        val cycleInitial = if (requestedCycleId != null && requestedCycleId > 0) {
            println("DEBUG_CAISSE: Cycle spécifié: $requestedCycleId")
            cycleDao.getCycleById(requestedCycleId)
        } else {
            println("DEBUG_CAISSE: Recherche cycle actif...")
            val actif = cycleDao.getCycleActifSnapshot(caisseId)
            if (actif == null) {
                println("DEBUG_CAISSE: Aucun cycle actif, récupération du dernier cycle clôturé...")
                cycleDao.getAllCycles(caisseId).first().firstOrNull()
            } else {
                actif
            }
        }

        if (cycleInitial == null) {
            println("DEBUG_CAISSE: Aucun cycle du tout, création du premier...")
            val newCycleId = createFirstCycle(caisseId).id
            _currentCycle.value = cycleDao.getCycleById(newCycleId)
        } else {
            println("DEBUG_CAISSE: Cycle chargé: ID=${cycleInitial.id}, Numero=${cycleInitial.numeroCycle}")
            _currentCycle.value = cycleInitial
            if (cycleInitial.estCloture) {
                _state.update { it.copy(isViewingOldCycle = true) }
            }
        }

        launch {
            val session = sessionDao.getCurrentSession()
            if (session != null) {
                val role = tontineMemberDao.getUserRoleInTontine(tontineId, session.userId)
                if (role != null) {
                    _state.update { it.copy(role = role) }
                }
            }
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
            tontineMemberDao.getTontineMembersWithUsers(tontineId).collect { members ->
                _state.update { it.copy(allTontineMembers = members) }
            }
        }

        launch {
            caisseDao.getCaissesByTontine(tontineId).collect { list ->
                _state.update { it.copy(allTontineCaisses = list) }
            }
        }

        launch {
            _currentCycle.flatMapLatest { cycle ->
                if (cycle == null) kotlinx.coroutines.flow.flowOf(emptyList())
                else tourDao.getPartsEncaisseesParMembreDuCycle(cycle.id)
            }.collect { list ->
                _state.update { it.copy(partsEncaisseesMap = list.associate { it.memberId to it.count }) }
            }
        }

        launch {
            _currentCycle.flatMapLatest { cycle ->
                if (cycle == null) kotlinx.coroutines.flow.flowOf(emptyList())
                else tourDao.getPartsEncaisseesJusquauCycle(caisseId, cycle.numeroCycle)
            }.collect { list ->
                _state.update { it.copy(partsEncaisseesJusquiciMap = list.associate { it.memberId to it.count }) }
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
            transactionDao.getCaisseBreakdown(caisseId).collect { list ->
                _state.update { it.copy(breakdown = list) }
            }
        }

        launch {
            transactionDao.getSoldeAmendes(caisseId).collect { solde ->
                _state.update { it.copy(soldeAmendes = solde ?: 0L) }
            }
        }

        launch {
            transactionDao.getSoldeFraisCaisse(caisseId).collect { solde ->
                _state.update { it.copy(soldeFraisCaisse = solde ?: 0L) }
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
            tontineMemberDao.getParticipantsByCaisse(caisseId).collect { participants ->
                val map = mutableMapOf<Long, Long>()
                participants.forEach { p ->
                    map[p.member.id] = transactionDao.getTotalCotiseReel(caisseId, p.member.id) ?: 0L
                }
                _state.update { it.copy(cotisationsCumuleesMap = map) }
            }
        }

        launch {
            transactionDao.getSoldeGlobal(caisseId).collect { solde ->
                _state.update { it.copy(soldeGlobal = solde ?: 0L) }
            }
        }

        launch {
            transactionDao.getTotalPretsEnCours(caisseId).collect { total ->
                val absoluteTotal = if (total != null && total < 0) -total else 0L
                _state.update { it.copy(totalPrets = absoluteTotal) }
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

        // Récupérer les bénéficiaires du cycle pour les colorier
        val beneficiariesIds = tourDao.getPartsEncaisseesParMembreDuCycleSnapshot(cycleId).map { it.memberId }
        
        var soldeCumule = transactionDao.getSoldeCyclesPrecedents(caisseId, cycle.numeroCycle).first() ?: 0L

        val rows = coroutineScope {
            participants.map { p ->
                async {
                    val memberId = p.member.id
                    val cibleCeCycle = p.nombreParts * montantFixe
                    val dejaCotiseJusqueIci = transactionDao.getSommeCotisJusqueCycle(caisseId, memberId, cycle.numeroCycle) ?: 0L
                    
                    // Récupérer la dernière transaction de cotisation de ce cycle pour la date
                    val history = transactionDao.getMemberHistoryInCaisse(memberId, caisseId).first()
                    val lastTx = history.find { it.cycleId == cycleId && it.type == com.indiza.cotisation.bank.TransactionType.COTISATION }
                    
                    val cotiseCeCycle = lastTx?.montant ?: 0L
                    val dateStr = lastTx?.let { 
                        java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it.dateTransaction))
                    }

                    MemberReportRow(
                        memberName = p.member.nomSaisi,
                        totalACotiser = cibleCeCycle,
                        dejaCotise = dejaCotiseJusqueIci,
                        manquant = if (cibleCeCycle > cotiseCeCycle) cibleCeCycle - cotiseCeCycle else 0L,
                        cotiseCeCycle = cotiseCeCycle,
                        dateCotisation = dateStr,
                        isBeneficiaire = beneficiariesIds.contains(memberId),
                        soldeCaisseMoment = 0L, 
                        status = if (cotiseCeCycle >= cibleCeCycle) "À JOUR" else "RETARD"
                    )
                }
            }.awaitAll()
        }
        
        val finalRows = rows.map { row ->
            soldeCumule += row.cotiseCeCycle
            row.copy(soldeCaisseMoment = soldeCumule)
        }

        _state.update { it.copy(reportRows = finalRows) }
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
            typeInteret = draft.typeInteret,
            penaliteRetardFixe = draft.penaliteRetardFixe,
            penaliteEchecFixe = draft.penaliteEchecFixe,
            fraisCaisse = draft.fraisCaisse,
            respecterFrequenceStrictement = draft.respecterFrequenceStrictement,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_UPDATE 
        )

        caisseDao.updateCaisse(updatedCaisse)
        // Re-charger pour actualiser l'UI
        loadData(caisseId)
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
        val maxNum = cycleDao.getMaxNumeroCycle(caisseId) ?: 0
        val c = CycleEntity(caisseId = caisseId, numeroCycle = maxNum + 1)
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
        val tontine = _state.value.tontine
        val caisse = _state.value.caisse
        
        participants.forEachIndexed { index, p ->
            tontineMemberDao.updateParticipantOrder(caisseId, p.member.id, index)
            
            if (tontine?.isOnline == true && tontine.remoteId != null && caisse?.remoteId != null) {
                if (p.member.userId != null) {
                    try {
                        repository.updateOnlineParticipantOrder(caisse.remoteId, p.member.userId, index)
                    } catch (e: Exception) {}
                }
            }
        }
        
        caisse?.let {
            val updated = it.copy(ordreFige = true)
            caisseDao.updateCaisse(updated)
            _state.update { s -> s.copy(ordreFige = true, participants = participants) }
        }
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

    fun annulerTour(memberId: Long, cycleId: Long) = viewModelScope.launch {
        tourDao.deleteToursByMemberAndCycle(memberId, cycleId)
    }

    fun annulerTousLesToursPlanifies(cycleId: Long) = viewModelScope.launch {
        // On récupère les planifiés actuels
        val planifies = tourDao.getPartsPlanifieesParMembreDuCycleSnapshot(cycleId)
        planifies.forEach { p ->
            tourDao.deleteToursByMemberAndCycle(p.memberId, cycleId)
        }
    }

    fun toggleParticipantCaisse(memberId: Long, selected: Boolean, parts: Int) = viewModelScope.launch {
        if (selected && !state.value.peutAjouterMembres) return@launch

        val tontine = _state.value.tontine
        val caisse = _state.value.caisse
        
        if (tontine?.isOnline == true && tontine.remoteId != null && caisse?.remoteId != null) {
            val member = tontineMemberDao.getMemberById(memberId)
            if (member?.userId != null) {
                try {
                    if (selected) {
                        repository.addOnlineParticipant(caisse.remoteId, member.userId, parts)
                    } else {
                        // TODO: Add delete participant API if needed, 
                        // for now we assume online management is primary
                    }
                } catch (e: Exception) {
                    // Log error or notify UI
                }
            }
        }

        // Garder la modif locale pour la réactivité immédiate et le cache
        if (selected) {
            caisseDao.addParticipantToCaisse(CaisseParticipantEntity(caisseId, memberId, 0, false, parts))
        } else {
            caisseDao.removeParticipantFromCaisse(caisseId, memberId)
        }
    }

    fun exporterFond(type: com.indiza.cotisation.bank.TransactionType, montant: Long, motif: String, targetCaisseId: Long? = null) = viewModelScope.launch {
        val cycle = _currentCycle.value ?: return@launch
        val session = sessionDao.getCurrentSession() ?: return@launch
        val member = tontineMemberDao.getMemberEntity(tontineId = _state.value.caisse?.tontineId ?: 0L, userId = session.userId) ?: return@launch
        
        // 1. Sortie de la caisse actuelle
        transactionDao.insertTransaction(
            com.indiza.cotisation.bank.TransactionEntity(
                caisseId = caisseId,
                memberId = member.id,
                cycleId = cycle.id,
                montant = -montant,
                type = type,
                motif = motif,
                validatedById = session.userId
            )
        )

        // 2. Entrée dans la caisse cible (si spécifiée)
        if (targetCaisseId != null) {
            transactionDao.insertTransaction(
                com.indiza.cotisation.bank.TransactionEntity(
                    caisseId = targetCaisseId,
                    memberId = member.id,
                    cycleId = cycle.id, // TODO: find active cycle for target?
                    montant = montant,
                    type = com.indiza.cotisation.bank.TransactionType.TRANSFERT_ENTREE,
                    motif = "Import : $motif",
                    validatedById = session.userId
                )
            )
        }
    }

    fun duplicateCaisse(newLibelle: String, onResult: (Long) -> Unit) = viewModelScope.launch {
        val newId = repository.duplicateCaisse(caisseId, newLibelle)
        onResult(newId)
    }

    fun toggleAllParticipants(selected: Boolean) = viewModelScope.launch {
        if (selected && !state.value.peutAjouterMembres) return@launch

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
