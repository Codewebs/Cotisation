package com.indiza.cotisation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.tontine.TontineRepository
import com.indiza.cotisation.tontine.TontineWithCaissesAndRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: UserEntity? = null,
    val myCreatedTontines: List<TontineWithCaissesAndRole> = emptyList(),
    val myMemberTontines: List<TontineWithCaissesAndRole> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionDao: SessionDao,
    private val userDao: UserDao,
    private val tontineRepository: TontineRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        try {
            val session = sessionDao.getCurrentSession()
            if (session != null) {
                val user = userDao.getUserById(session.userId)
                _state.update { it.copy(user = user) }
                
                val allMyTontines = tontineRepository.fetchMyOnlineTontines()
                
                val created = allMyTontines.filter { it.tontine.creatorId == session.userId }
                val memberOnly = allMyTontines.filter { it.tontine.creatorId != session.userId }
                
                _state.update { it.copy(
                    myCreatedTontines = created,
                    myMemberTontines = memberOnly,
                    isLoading = false
                ) }
            } else {
                _state.update { it.copy(error = "Non connecté", isLoading = false) }
            }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message, isLoading = false) }
        }
    }

    fun updateProfile(nom: String, phone: String) = viewModelScope.launch {
        // Implementation for profile update if needed
    }
}
