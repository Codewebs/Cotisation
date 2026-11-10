package com.indiza.cotisation.tontine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.bank.CycleDao
import com.indiza.cotisation.bank.CycleEntity
import com.indiza.cotisation.bank.TransactionDao
import com.indiza.cotisation.bank.TransactionEntity
import com.indiza.cotisation.caisses.CaisseDao
import com.indiza.cotisation.caisses.CaisseDraft
import com.indiza.cotisation.caisses.CaisseWithActiveCycle
import com.indiza.cotisation.sync.SyncAction
import com.indiza.cotisation.sync.SyncQueueDao
import com.indiza.cotisation.sync.SyncQueueEntity
import com.indiza.cotisation.sync.SyncStatus
import com.indiza.cotisation.users.Role
import com.indiza.cotisation.users.UserRepository
import com.indiza.cotisation.users.SessionDao
import com.indiza.cotisation.tontinemembers.MemberUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.indiza.cotisation.sync.SyncManager
import com.indiza.cotisation.users.SessionManager
import com.indiza.cotisation.bank.SimpleTransaction
import com.indiza.cotisation.bank.IncomeStats

@HiltViewModel
class TontineDetailViewModel @Inject constructor(
    private val repository: TontineRepository,
    private val userRepository: UserRepository,
    private val sessionDao: SessionDao,
    private val cycleDao: CycleDao,
    private val tontineMemberDao: TontineMemberDao,
    private val tontineDao: TontineDao,
    private val caisseDao: CaisseDao,
    private val syncQueueDao: SyncQueueDao,
    private val syncManager: SyncManager,
    private val sessionManager: SessionManager,
    private val transactionDao: TransactionDao
) : ViewModel() {

    private val _state = MutableStateFlow<TontineDetailUiState>(TontineDetailUiState.Loading)
    val state: StateFlow<TontineDetailUiState> = _state.asStateFlow()

    private val _filterOption = MutableStateFlow(CaisseFilterOption.ALPHABETICAL)
    val filterOption: StateFlow<CaisseFilterOption> = _filterOption.asStateFlow()

    private var loadJob: Job? = null

    fun loadTontine(tontineId: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val session = sessionDao.getCurrentSession()
            val userId = session?.userId ?: ""
            
            val flows = listOf(
                tontineDao.getTontineWithCaisses(tontineId),
                tontineMemberDao.getTontineMembersWithUsers(tontineId),
                filterOption,
                transactionDao.getTontineGlobalBalance(tontineId),
                transactionDao.getTontineTotalOutflows(tontineId),
                transactionDao.getTontineTotalInflows(tontineId),
                transactionDao.getTontineTotalActiveLoans(tontineId),
                transactionDao.getTontineForecastGains(tontineId),
                transactionDao.getTontineLendableBalance(tontineId),
                transactionDao.getTontineNonLendableBalance(tontineId),
                transactionDao.getTontineTransactionsHistory(tontineId),
                transactionDao.getTontineIncomeDistribution(tontineId)
            )

            combine(flows) { array ->
                val tontineData = array[0] as TontineWithCaisses
                val members = array[1] as List<TontineMemberWithUser>
                val filter = array[2] as CaisseFilterOption
                val bal = array[3] as Long?
                val out = array[4] as Long?
                val `in` = array[5] as Long?
                val loans = array[6] as Long?
                val gains = array[7] as Long?
                val lend = array[8] as Long?
                val nonLend = array[9] as Long?
                val history = array[10] as List<SimpleTransaction>
                val income = array[11] as List<IncomeStats>

                val userMember = members.find { it.member.userId == userId }
                val userRole = userMember?.member?.role ?: Role.MEMBRE
                
                val caissesWithCycles = tontineData.caisses.map { caisse ->
                    val activeCycle = cycleDao.getCycleActifSnapshot(caisse.id)
                        ?: cycleDao.getAllCycles(caisse.id).firstOrNull()?.firstOrNull()
                        ?: cycleDao.insertCycle(CycleEntity(caisseId = caisse.id, numeroCycle = 1)).let { cycleDao.getCycleById(it)!! }
                    
                    CaisseWithActiveCycle(caisse, activeCycle.id)
                }

                val displayMembers = getDisplayMembers(userId, tontineData.tontine, userRole, members)
                val takenRoles = members.map { it.member.role }.toSet()

                TontineDetailUiState.Success(
                    tontineWithCaisses = tontineData,
                    caissesWithCycles = caissesWithCycles,
                    role = userRole,
                    displayMembers = displayMembers,
                    takenUniqueRoles = takenRoles,
                    isInitiator = tontineData.tontine.creatorId == userId,
                    filterOption = filter,
                    financialSummary = TontineFinancialSummary(
                        turnoverIn = `in` ?: 0L,
                        turnoverOut = out ?: 0L,
                        globalBalance = bal ?: 0L,
                        totalActiveLoans = loans ?: 0L,
                        forecastGains = gains ?: 0L,
                        lendableAmount = lend ?: 0L,
                        nonLendableAmount = nonLend ?: 0L
                    ),
                    canInitiateLoans = userRole == Role.TRESORIER || userRole == Role.INITIATEUR,
                    canConsultLoans = userRole != Role.MEMBRE,
                    hasRequiredRolesForLoans = takenRoles.contains(Role.TRESORIER) && takenRoles.contains(Role.COMMISSAIRE_AUX_COMPTES),
                    growthStats = history,
                    incomeStats = income
                )
            }.collect {
                _state.value = it
            }
        }
    }

    fun setFilterOption(option: CaisseFilterOption) {
        _filterOption.value = option
    }

    fun getCaisseSolde(caisseId: Long): Flow<Long?> {
        return transactionDao.getCaisseBalance(caisseId)
    }

    fun addCaisseToTontine(tontineId: Long, draft: CaisseDraft) = viewModelScope.launch {
        repository.insertCaisse(tontineId, draft)
    }

    fun deleteCaisse(caisseId: Long, onResult: (Result<Unit>) -> Unit) = viewModelScope.launch {
        onResult(repository.deleteCaisse(caisseId))
    }

    fun generateInviteLink(tontineId: Long, onResult: (String?) -> Unit) = viewModelScope.launch {
        val tontine = tontineDao.getById(tontineId)
        val senderId = sessionDao.getCurrentSession()?.userId
        if (tontine?.remoteId == null || senderId.isNullOrBlank()) {
            onResult(null)
            return@launch
        }
        try {
            val inviteCode = repository.generateInviteLink(tontine.remoteId, senderId)
            onResult("cotisation://join?code=$inviteCode")
        } catch (e: Exception) {
            onResult(null)
        }
    }

    fun broadcastReport(tontineId: Long, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val tontine = tontineDao.getById(tontineId)
        val senderId = sessionDao.getCurrentSession()?.userId
        if (tontine?.remoteId == null || senderId.isNullOrBlank()) {
            onResult(false)
            return@launch
        }
        onResult(
            runCatching {
                repository.broadcastReport(tontine.remoteId, senderId)
            }.isSuccess
        )
    }

    fun addMember(tontineId: Long, nom: String, phone: String, role: Role) = viewModelScope.launch {
        try {
            val matricule = "M-${System.currentTimeMillis().toString().takeLast(4)}"
            val tontine = tontineDao.getById(tontineId)
            if (tontine?.isOnline == true && tontine.remoteId != null && tontine.syncStatus == SyncStatus.SYNCED) {
                val member = runCatching {
                    repository.addOnlineMember(tontine.remoteId, nom, phone, role)
                }.getOrNull()
                repository.addMemberToTontine(
                    tontineId = tontineId,
                    nom = nom,
                    phone = phone,
                    role = role,
                    matricule = matricule,
                    remoteId = member?.id,
                    userId = member?.userId
                )
            } else {
                repository.addMemberToTontine(tontineId, nom, phone, role, matricule)
            }
        } catch (e: Exception) {
            println("ERROR_ADD_MEMBER: ${e.message}")
        }
    }

    fun getDisplayMembers(currentUserId: String, tontine: TontineEntity, userRole: Role, members: List<TontineMemberWithUser>): List<MemberUiModel> {
        val isPrivileged = userRole == Role.INITIATEUR || userRole == Role.TRESORIER || userRole == Role.SECRETAIRE
        
        return members.map { item ->
            val isMe = item.member.userId == currentUserId
            val shouldHide = !isPrivileged && !isMe && tontine.isAnonymous
            
            val rawName = item.member.nomSaisi
            
            MemberUiModel(
                id = item.member.id.toString(),
                displayName = if (shouldHide) "Adhérent ${item.member.id}" else rawName,
                matricule = item.member.matricule ?: "",
                role = item.member.role,
                isMe = isMe,
                isInitiator = item.member.userId == tontine.creatorId,
                isLinked = item.member.userId != null,
                phoneNumber = if (shouldHide) "****" else item.member.phone ?: ""
            )
        }
    }

    fun updateMember(memberId: String, nom: String, phone: String, role: Role) = viewModelScope.launch {
        try {
            tontineMemberDao.updateMemberInfo(memberId.toLong(), nom, phone, role)
        } catch (e: Exception) {
            // Gérer l'erreur
        }
    }

    fun goOnline(tontineId: Long) = viewModelScope.launch {
        val tontine = tontineDao.getById(tontineId) ?: return@launch
        if (tontine.isOnline) return@launch

        val newRemoteId = tontine.remoteId ?: java.util.UUID.randomUUID().toString()
        tontineDao.updateRemoteInfo(tontineId, newRemoteId, SyncStatus.PENDING_CREATE)
        repository.updateOnlineStatus(tontineId, true)

        syncQueueDao.enqueue(SyncQueueEntity(entityType = "TONTINE", entityLocalId = tontineId, action = SyncAction.CREATE))

        val caisses = repository.getTontineWithCaisses(tontineId).first().caisses
        caisses.forEach { caisse ->
            val caisseRemoteId = caisse.remoteId ?: java.util.UUID.randomUUID().toString()
            caisseDao.updateRemoteInfo(caisse.id, caisseRemoteId, SyncStatus.PENDING_CREATE)
            syncQueueDao.enqueue(SyncQueueEntity(entityType = "CAISSE", entityLocalId = caisse.id, action = SyncAction.CREATE))
        }
        
        syncManager.triggerSync()
        loadTontine(tontineId)
    }

    fun refreshLiveTontine(tontineId: Long, onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                syncManager.triggerSync()

                val tontine = tontineDao.getById(tontineId)
                if (tontine?.remoteId != null) {
                    val remoteDetails = repository.getOnlineTontineDetails(tontine.remoteId)
                    repository.syncOnlineTontineMembers(tontineId, remoteDetails.members.orEmpty())

                    val caisses = caisseDao.getCaissesByTontineSnapshot(tontineId)
                    caisses.forEach { caisse ->
                        repository.syncOnlineTransactions(caisse)
                    }
                }

                loadTontine(tontineId)
                onResult("Réunion actualisée en direct")
            } catch (e: Exception) {
                onResult("Erreur d'actualisation: ${e.message}")
            }
        }
    }

    fun getTransactionsForCycles(cycleIds: List<Long>): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByCycles(cycleIds)
    }
}

enum class CaisseFilterOption { ALPHABETICAL, CREATION_DATE_ASC, CREATION_DATE_DESC, GROUP_BY_TYPE, GROUP_BY_CREATOR }

data class TontineFinancialSummary(
    val turnoverIn: Long,
    val turnoverOut: Long,
    val globalBalance: Long,
    val totalActiveLoans: Long,
    val forecastGains: Long,
    val lendableAmount: Long,
    val nonLendableAmount: Long
)

sealed class TontineDetailUiState {
    object Loading : TontineDetailUiState()
    data class Error(val message: String) : TontineDetailUiState()
    data class Success(
        val tontineWithCaisses: TontineWithCaisses,
        val caissesWithCycles: List<CaisseWithActiveCycle>,
        val role: Role,
        val displayMembers: List<MemberUiModel>,
        val takenUniqueRoles: Set<Role>,
        val isInitiator: Boolean,
        val filterOption: CaisseFilterOption,
        val financialSummary: TontineFinancialSummary,
        val canInitiateLoans: Boolean,
        val canConsultLoans: Boolean,
        val hasRequiredRolesForLoans: Boolean,
        val growthStats: List<SimpleTransaction>,
        val incomeStats: List<IncomeStats>
    ) : TontineDetailUiState()
}
