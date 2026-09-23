package com.indiza.cotisation.invitations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.network.RemoteApi
import com.indiza.cotisation.tontine.TontineMemberDao
import com.indiza.cotisation.tontine.TontineMemberWithUser
import com.indiza.cotisation.users.SessionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JoinRequestsUiState(
    val isLoading: Boolean = false,
    val requests: List<JoinRequestResponse> = emptyList(),
    val availableMembers: List<TontineMemberWithUser> = emptyList(),
    val currentUserRole: com.indiza.cotisation.users.Role? = null,
    val error: String? = null
)

@HiltViewModel
class JoinRequestsViewModel @Inject constructor(
    private val remoteApi: RemoteApi,
    private val memberDao: TontineMemberDao,
    private val sessionDao: SessionDao
) : ViewModel() {

    private val _state = MutableStateFlow(JoinRequestsUiState())
    val state = _state.asStateFlow()

    private var currentTontineId: Long? = null

    fun loadRequests(tontineId: Long) {
        currentTontineId = tontineId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val session = sessionDao.getCurrentSession()
                val role = if (session != null) memberDao.getUserRoleInTontine(tontineId, session.userId) else null
                
                val requests = remoteApi.getJoinRequests(tontineId.toString())
                val members = memberDao.getTontineMembersWithUsers(tontineId).first()
                _state.update { it.copy(
                    isLoading = false, 
                    requests = requests, 
                    availableMembers = members,
                    currentUserRole = role
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun approve(requestId: String, targetMemberId: Long, updateInfo: Boolean) {
        viewModelScope.launch {
            try {
                val session = sessionDao.getCurrentSession() ?: throw Exception("Session requise")
                val currentState = _state.value
                val request = currentState.requests.find { it.id == requestId } ?: throw Exception("Demande introuvable")
                
                val role = currentState.currentUserRole ?: throw Exception("Rôle non défini")
                
                // Logic hiérarchique
                val now = System.currentTimeMillis()
                val requestAgeMillis = now - request.createdAt
                val hoursPassed = requestAgeMillis / (1000 * 60 * 60)
                
                val canApprove = when (role) {
                    com.indiza.cotisation.users.Role.PRESIDENT -> true
                    com.indiza.cotisation.users.Role.INITIATEUR -> true // Initiateur a toujours le droit si pas de bureau
                    com.indiza.cotisation.users.Role.VICE_PRESIDENT -> hoursPassed >= 24
                    com.indiza.cotisation.users.Role.SECRETAIRE, 
                    com.indiza.cotisation.users.Role.SECRETAIRE_ADJOINT_A1,
                    com.indiza.cotisation.users.Role.SECRETAIRE_ADJOINT_A2 -> hoursPassed >= 48
                    else -> false
                }
                
                if (!canApprove) {
                    val remaining = if (role == com.indiza.cotisation.users.Role.VICE_PRESIDENT) 24 - hoursPassed else 48 - hoursPassed
                    throw Exception("Droit de validation hiérarchique : Attendez encore ${remaining}h pour valider.")
                }

                remoteApi.approveJoinRequest(requestId, session.userId, targetMemberId, updateInfo)
                currentTontineId?.let { loadRequests(it) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
}
