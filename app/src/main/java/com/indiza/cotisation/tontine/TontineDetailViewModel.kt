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
    private val sessionManager: SessionManager, // Pour récupérer le rôle actuel
    private val transactionDao: TransactionDao
) : ViewModel()
{

    private val _state = MutableStateFlow<TontineDetailUiState>(TontineDetailUiState.Loading)
    val state = _state.asStateFlow()

    private var loadJob: Job? = null

    fun loadTontine(id: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val currentSession = sessionDao.getCurrentSession()
            val userId = currentSession?.userId ?: return@launch

            // On écoute les changements de la tontine et des membres en parallèle
            combine(
                repository.getTontineWithCaisses(id),
                tontineMemberDao.getTontineMembersWithUsers(id)
            ) { tontineData, memberList ->

                val caissesWithCycles = mutableListOf<CaisseWithActiveCycle>()
                for (caisse in tontineData.caisses) {
                    val activeCycleId = cycleDao.getCycleActifSnapshot(caisse.id)?.id ?: 0L
                    caissesWithCycles.add(CaisseWithActiveCycle(caisse, activeCycleId))
                }

                val role = tontineMemberDao.getUserRoleInTontine(id, userId) ?: Role.MEMBRE

                val displayMembers = getDisplayMembers(
                    currentUserId = userId,
                    tontine = tontineData.tontine,
                    currentUserRole = role,
                    members = memberList
                )

                TontineDetailUiState.Success(
                    tontineWithCaisses = tontineData,
                    caissesWithCycles = caissesWithCycles,
                    role = role,
                    displayMembers = displayMembers
                )
            }.collect { newState ->
                _state.value = newState
            }
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

            // On priorise le nom de l'utilisateur réel s'il existe, sinon celui saisi par l'admin
            val rawName = item.user?.nom ?: item.member.nomSaisi

            MemberUiModel(
                id = item.member.id.toString(),
                displayName = if (shouldHide) "Adhérent ${item.member.id}" else rawName,
                matricule = item.member.matricule.toString(),
                role = item.member.role,
                isMe = isMe,
                isLinked = item.member.userId != null // True si un utilisateur réel est mappé
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

sealed class TontineDetailUiState {
    object Loading : TontineDetailUiState()
    data class Success(
        val tontineWithCaisses: TontineWithCaisses,
        val caissesWithCycles: List<CaisseWithActiveCycle>,
        val role: Role,
        val displayMembers: List<MemberUiModel> // Ajouté ici
    ) : TontineDetailUiState()
}