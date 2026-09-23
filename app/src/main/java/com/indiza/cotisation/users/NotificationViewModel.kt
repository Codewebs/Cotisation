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

data class NotificationUiState(
    val notifications: List<NotificationResponse> = emptyList(),
    val unreadCount: Int = 0
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val remoteApi: RemoteApi,
    private val sessionDao: SessionDao
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationUiState())
    val state = _state.asStateFlow()

    fun fetchNotifications() {
        viewModelScope.launch {
            try {
                val session = sessionDao.getCurrentSession() ?: return@launch
                val list = remoteApi.getUserNotifications(session.userId)
                _state.update { it.copy(
                    notifications = list,
                    unreadCount = list.count { !it.isRead }
                )}
            } catch (e: Exception) {
                // Silently fail or log
            }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            try {
                remoteApi.markNotificationAsRead(id)
                fetchNotifications()
            } catch (e: Exception) {}
        }
    }
}
