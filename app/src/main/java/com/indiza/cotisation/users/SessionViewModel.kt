package com.indiza.cotisation.users

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun toggleSync(enabled: Boolean) {
        prefs.edit().putBoolean("sync_enabled", enabled).apply()
        _syncEnabled.value = enabled
    }

    fun logout() {
        viewModelScope.launch {
            sessionDao.clearSession()
        }
    }
}