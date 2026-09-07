package com.indiza.cotisation.tontine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.bank.CycleDao
import com.indiza.cotisation.caisses.CaisseDraft
import com.indiza.cotisation.caisses.CaisseWithActiveCycle
import com.indiza.cotisation.tontinemembers.MemberUiModel
import com.indiza.cotisation.users.Permission
import com.indiza.cotisation.users.Role
import com.indiza.cotisation.users.SessionDao
import com.indiza.cotisation.users.SessionManager
import com.indiza.cotisation.bank.TransactionDao
import com.indiza.cotisation.users.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TontineDetailViewModel @Inject constructor(
    private val repository: TontineRepository,
    private val userRepository: UserRepository,
    private val sessionDao: SessionDao,
    private val cycleDao: CycleDao,
    private val tontineMemberDao: TontineMemberDao,
    private val sessionManager: SessionManager, 
    private val transactionDao: TransactionDao
) : ViewModel()
{

    private val _state = MutableStateFlow<TontineDetailUiState>(TontineDetailUiState.Loading)
    val state = _state.asStateFlow()

    private val _filterOption = MutableStateFlow(CaisseFilterOption.ALPHABETICAL)
    val filterOption = _filterOption.asStateFlow()

    private var loadJob: Job? = null

    @Suppress("UNCHECKED_CAST")
    fun loadTontine(id: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val currentSession = sessionDao.getCurrentSession()
            val userId = currentSession?.userId ?: return@launch

            val flows = listOf(
                repository.getTontineWithCaisses(id),
                tontineMemberDao.getTontineMembersWithUsers(id),
                _filterOption,
                transactionDao.getTontineGlobalBalance(id),
                transactionDao.getTontineTotalInflows(id),
                transactionDao.getTontineTotalOutflows(id),
                transactionDao.getTontineTotalActiveLoans(id),
                transactionDao.getTontineForecastGains(id),
                transactionDao.getTontineLendableBalance(id),
                transactionDao.getTontineNonLendableBalance(id)
            )

            combine(flows) { flowsArray ->
                val tontineData = flowsArray[0] as TontineWithCaisses
                val memberList = flowsArray[1] as List<TontineMemberWithUser>
                val filter = flowsArray[2] as CaisseFilterOption
                val balance = flowsArray[3] as Long?
                val inflows = flowsArray[4] as Long?
                val outflows = flowsArray[5] as Long?
                val loans = flowsArray[6] as Long?
                val gains = flowsArray[7] as Long?
                val lendable = flowsArray[8] as Long?
                val nonLendable = flowsArray[9] as Long?

                var caissesWithCycles = mutableListOf<CaisseWithActiveCycle>()
                for (caisse in tontineData.caisses) {
                    val activeCycleId = cycleDao.getCycleActifSnapshot(caisse.id)?.id ?: 0L
                    caissesWithCycles.add(CaisseWithActiveCycle(caisse, activeCycleId))
                }

                caissesWithCycles = when (filter) {
                    CaisseFilterOption.ALPHABETICAL -> caissesWithCycles.sortedBy { it.caisse.libelle }.toMutableList()
                    CaisseFilterOption.CREATION_DATE_ASC -> caissesWithCycles.sortedBy { it.caisse.updatedAt }.toMutableList()
                    CaisseFilterOption.CREATION_DATE_DESC -> caissesWithCycles.sortedByDescending { it.caisse.updatedAt }.toMutableList()
                    else -> caissesWithCycles 
                }

                val role = tontineMemberDao.getUserRoleInTontine(id, userId) ?: Role.MEMBRE

                val takenUniqueRoles = memberList.map { it.member.role }.filter { it.isUnique }.toSet()

                val displayMembers = getDisplayMembers(
                    currentUserId = userId,
                    tontine = tontineData.tontine,
                    currentUserRole = role,
                    members = memberList
                )

                val currentUserMember = memberList.find { it.user?.id == userId }
                val isInitiator = currentUserMember?.member?.isInitiator ?: false

                val roleCounts = memberList.groupBy { it.member.role }.mapValues { it.value.size }
                val requiredRoles = listOf(Role.PRESIDENT, Role.TRESORIER, Role.COMMISSAIRE_AUX_COMPTES, Role.SECRETAIRE)
                val existingRequiredRolesCount = requiredRoles.count { (roleCounts[it] ?: 0) > 0 }
                
                val canInitiateLoans = role == Role.PRESIDENT || role == Role.TRESORIER
                val canConsultLoans = requiredRoles.contains(role) || role == Role.INITIATEUR
                val hasRequiredRolesForLoans = existingRequiredRolesCount >= 3

                TontineDetailUiState.Success(
                    tontineWithCaisses = tontineData,
                    caissesWithCycles = caissesWithCycles,
                    role = role,
                    displayMembers = displayMembers,
                    takenUniqueRoles = takenUniqueRoles,
                    isInitiator = isInitiator,
                    filterOption = filter,
                    financialSummary = TontineFinancialSummary(
                        turnoverIn = inflows ?: 0L,
                        turnoverOut = outflows ?: 0L,
                        globalBalance = balance ?: 0L,
                        totalActiveLoans = loans ?: 0L,
                        forecastGains = gains ?: 0L,
                        lendableAmount = lendable ?: 0L,
                        nonLendableAmount = nonLendable ?: 0L
                    ),
                    canInitiateLoans = canInitiateLoans,
                    canConsultLoans = canConsultLoans,
                    hasRequiredRolesForLoans = hasRequiredRolesForLoans
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    fun setFilterOption(option: CaisseFilterOption) {
        if (option == CaisseFilterOption.CREATION_DATE_ASC || option == CaisseFilterOption.CREATION_DATE_DESC) {
            if (_filterOption.value == CaisseFilterOption.CREATION_DATE_ASC) {
                _filterOption.value = CaisseFilterOption.CREATION_DATE_DESC
            } else {
                _filterOption.value = CaisseFilterOption.CREATION_DATE_ASC
            }
        } else {
            _filterOption.value = option
        }
    }
    
    fun getCaisseSolde(caisseId: Long): Flow<Long?> = transactionDao.getSoldeGlobal(caisseId)

    fun addCaisseToTontine(tontineId: Long, draft: CaisseDraft) = viewModelScope.launch {
        repository.insertCaisse(tontineId, draft)
    }

    fun addMember(tontineId: Long, nom: String, phone: String, role: Role) = viewModelScope.launch {
        try {
            val member = TontineMemberEntity(
                tontineId = tontineId,
                userId = null,
                matricule = null,
                nomSaisi = nom,
                phone = phone,
                role = role
            )
            tontineMemberDao.insertMember(member)
        } catch (e: Exception) {
            // Log erreur
        }
    }
    
    private fun getDisplayMembers(
        currentUserId: String,
        tontine: TontineEntity,
        currentUserRole: Role,
        members: List<TontineMemberWithUser>
    ): List<MemberUiModel> {
        val canSeeNames = currentUserRole.permissions.contains(Permission.VIEW_MEMBER_NAMES)

        return members.map { item ->
            val isMe = item.user?.id == currentUserId
            val shouldHide = tontine.isAnonymous && !canSeeNames && !isMe

            val rawName = item.user?.nom ?: item.member.nomSaisi

            MemberUiModel(
                id = item.member.id.toString(),
                displayName = if (shouldHide) "Adhérent ${item.member.id}" else rawName,
                matricule = item.member.matricule ?: "",
                role = item.member.role,
                isMe = isMe,
                isInitiator = item.member.isInitiator,
                isLinked = item.member.userId != null,
                phoneNumber = if (shouldHide) "****" else item.member.phone
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
}

enum class CaisseFilterOption {
    ALPHABETICAL,
    CREATION_DATE_ASC,
    CREATION_DATE_DESC,
    GROUP_BY_TYPE,
    GROUP_BY_CREATOR
}

data class TontineFinancialSummary(
    val turnoverIn: Long = 0L,
    val turnoverOut: Long = 0L,
    val globalBalance: Long = 0L,
    val totalActiveLoans: Long = 0L,
    val forecastGains: Long = 0L,
    val lendableAmount: Long = 0L,
    val nonLendableAmount: Long = 0L
)

sealed class TontineDetailUiState {
    object Loading : TontineDetailUiState()
    data class Success(
        val tontineWithCaisses: TontineWithCaisses,
        val caissesWithCycles: List<CaisseWithActiveCycle>,
        val role: Role,
        val displayMembers: List<MemberUiModel>,
        val takenUniqueRoles: Set<Role> = emptySet(),
        val isInitiator: Boolean = false,
        val filterOption: CaisseFilterOption = CaisseFilterOption.ALPHABETICAL,
        val financialSummary: TontineFinancialSummary = TontineFinancialSummary(),
        val canInitiateLoans: Boolean = false, 
        val canConsultLoans: Boolean = false, 
        val hasRequiredRolesForLoans: Boolean = false 
    ) : TontineDetailUiState()
}
