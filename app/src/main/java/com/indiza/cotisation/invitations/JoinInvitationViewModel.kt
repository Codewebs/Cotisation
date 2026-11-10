package com.indiza.cotisation.invitations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.network.SupabaseDiscoveryService
import com.indiza.cotisation.users.SessionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JoinInvitationUiState(
    val isLoading: Boolean = true,
    val tontineName: String? = null,
    val inviterName: String? = null,
    val isAuthenticated: Boolean = false,
    val isJoining: Boolean = false,
    val joinSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class JoinInvitationViewModel @Inject constructor(
    private val discoveryService: SupabaseDiscoveryService,
    private val sessionDao: SessionDao
) : ViewModel() {

    private val _state = MutableStateFlow(JoinInvitationUiState())
    val state = _state.asStateFlow()

    private var currentInviteCode: String? = null

    fun loadInvitation(code: String) {
        currentInviteCode = code
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val isAuthenticated = sessionDao.getCurrentSession() != null
            _state.update { it.copy(isAuthenticated = isAuthenticated) }
            try {
                val details = discoveryService.getTontineInvite(code)
                _state.update { it.copy(
                    isLoading = false,
                    isAuthenticated = isAuthenticated,
                    tontineName = details.tontineName,
                    inviterName = null
                )}
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = isAuthenticated,
                        error = if (isAuthenticated) "Invitation invalide ou expiré" else "Connectez-vous pour consulter cette invitation"
                    )
                }
            }
        }
    }

    fun join() {
        val code = currentInviteCode ?: return
        viewModelScope.launch {
            _state.update { it.copy(isJoining = true, error = null) }
            try {
                val session = sessionDao.getCurrentSession() ?: throw Exception("Session requise")
                discoveryService.joinTontineWithInvite(code)
                _state.update { it.copy(isJoining = false, joinSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isJoining = false, error = "Erreur: ${e.message}") }
            }
        }
    }
}
