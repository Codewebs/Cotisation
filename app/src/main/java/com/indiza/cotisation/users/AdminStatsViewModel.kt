package com.indiza.cotisation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.network.RemoteApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminStatsUiState(
    val stats: AdminSignatureStats? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class AdminStatsViewModel @Inject constructor(
    private val remoteApi: RemoteApi,
    private val sessionDao: SessionDao
) : ViewModel() {

    private val _state = MutableStateFlow(AdminStatsUiState())
    val state = _state.asStateFlow()

    fun fetchStats() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val session = sessionDao.getCurrentSession()
                if (session != null) {
                    val stats = remoteApi.getAdminSignatureStats(session.userId)
                    _state.update { it.copy(stats = stats, isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
