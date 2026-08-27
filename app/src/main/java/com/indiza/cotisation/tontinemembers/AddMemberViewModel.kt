package com.indiza.cotisation.tontinemembers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.tontine.TontineMemberDao
import com.indiza.cotisation.tontine.TontineMemberEntity
import com.indiza.cotisation.tontine.TontineRepository
import com.indiza.cotisation.users.Role
import com.indiza.cotisation.users.UserDao
import com.indiza.cotisation.users.UserEntity
import dagger.hilt.android.lifecycle.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddMemberViewModel @Inject constructor(
    private val tontineMemberDao: TontineMemberDao,
    private val userDao: UserDao,
    private val repository: TontineRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddMemberUiState())
    val state = _state.asStateFlow()

    private val _addSuccess = MutableSharedFlow<Unit>()
    val addSuccess = _addSuccess.asSharedFlow()

    fun onNameChange(name: String) = _state.update { it.copy(nom = name) }
    fun onPhoneChange(phone: String) = _state.update { it.copy(phone = phone) }
    fun onRoleChange(role: Role) = _state.update { it.copy(selectedRole = role) }

    // Charge les membres d'une autre tontine pour duplication
    fun loadPreviousMembers(currentTontineId: Long) = viewModelScope.launch {
        // Logique pour trouver la tontine précédente (même libellé, date antérieure)
        // Pour l'instant, on peut charger tous les membres connus de l'utilisateur
        val members = tontineMemberDao.getAllKnownMembers()
        _state.update { it.copy(previousMembers = members) }
    }

    fun saveMember(tontineId: Long) = viewModelScope.launch {
        val currentState = _state.value
        _state.update { it.copy(isSaving = true) }

        try {
            // 1. Créer ou récupérer l'utilisateur
            val userId = UUID.randomUUID().toString()
            val user = UserEntity(
                id = userId,
                nom = currentState.nom,
                phone = currentState.phone,
                matricule = currentState.matricule,
                isVerified = false
            )
            userDao.upsertUser(user)

            // 2. Créer le lien tontine-membre (Adhésion locale)
            val membership = TontineMemberEntity(
                tontineId = tontineId,
                userId = userId,
                nomSaisi = currentState.nom,
                phone = currentState.phone,
                role = currentState.selectedRole,
                matricule = currentState.matricule,
                isActive = true
            )

            tontineMemberDao.insertMember(membership)
            _addSuccess.emit(Unit)

        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
        } finally {
            _state.update { it.copy(isSaving = false) }
        }
    }
}

data class AddMemberUiState(
    val nom: String = "",
    val phone: String = "",
    val matricule: String = "",
    val selectedRole: Role = Role.MEMBRE,
    val previousMembers: List<UserEntity> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null
)