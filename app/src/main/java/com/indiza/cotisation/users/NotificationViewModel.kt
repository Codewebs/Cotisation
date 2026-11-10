package com.indiza.cotisation.users

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.network.RemoteApi
import com.indiza.cotisation.network.SupabaseDataApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val notifications: List<NotificationResponse> = emptyList(),
    val unreadCount: Int = 0
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val remoteApi: RemoteApi,
    private val supabaseDataApi: SupabaseDataApi,
    private val sessionDao: SessionDao
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationUiState())
    val state = _state.asStateFlow()
    private var realtimeJob: Job? = null

    fun observeNotifications() {
        if (realtimeJob?.isActive == true) return
        realtimeJob = viewModelScope.launch {
            val session = sessionDao.getCurrentSession() ?: return@launch
            Log.i("CotisationPush", "NotificationViewModel: Subscribing to Supabase Realtime notifications for user ${session.userId}")
            supabaseDataApi.observeNotifications(session.userId).collect {
                Log.i("CotisationPush", "NotificationViewModel: Realtime notification event received! Fetching updated in-app notifications...")
                fetchNotifications()
            }
        }
    }

    fun fetchNotifications() {
        viewModelScope.launch {
            try {
                val session = sessionDao.getCurrentSession() ?: return@launch
                val list = remoteApi.getUserNotifications(session.userId)
                val unread = list.count { !it.isRead }
                Log.i("CotisationPush", "NotificationViewModel: In-app notifications fetched successfully. Total: ${list.size}, Unread: $unread")
                _state.update { it.copy(
                    notifications = list,
                    unreadCount = unread
                )}
            } catch (e: Exception) {
                Log.e("CotisationPush", "NotificationViewModel: Error fetching in-app notifications: ${e.message}", e)
            }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            try {
                remoteApi.markNotificationAsRead(id)
                fetchNotifications()
            } catch (e: Exception) {
                Log.e("CotisationPush", "NotificationViewModel: Error marking notification $id as read: ${e.message}", e)
            }
        }
    }
}
