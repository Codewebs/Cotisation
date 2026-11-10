package com.indiza.cotisation.users

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.network.PushTokenRegistrar
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionDao: SessionDao,
    private val tontineRepository: com.indiza.cotisation.tontine.TontineRepository,
    private val pushTokenRegistrar: PushTokenRegistrar,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val hasActiveSession: Flow<Boolean> = flow {
        while (true) {
            emit(sessionDao.hasActiveSession())
            delay(1000)
        }
    }

    private val prefs = context.getSharedPreferences("cotisation_prefs", Context.MODE_PRIVATE)

    private val _syncEnabled = MutableStateFlow(prefs.getBoolean("sync_enabled", true))
    val syncEnabled = _syncEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            if (sessionDao.hasActiveSession()) {
                Log.i("CotisationPush", "SessionViewModel init: Active session found. Registering FCM token...")
                pushTokenRegistrar.registerCurrentDevice()
            }
        }
    }

    fun toggleSync(enabled: Boolean) {
        prefs.edit().putBoolean("sync_enabled", enabled).apply()
        _syncEnabled.value = enabled
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { pushTokenRegistrar.unregisterCurrentDevice() }
                .onFailure { Log.w("CotisationPush", "Unable to unregister device token on logout.", it) }
            tontineRepository.clearLocalData()
            sessionDao.clearSession()
        }
    }
}