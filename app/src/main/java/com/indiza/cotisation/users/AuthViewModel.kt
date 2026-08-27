package com.indiza.cotisation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    fun login(phone: String, pin: String, callback: (Result<SessionEntity>) -> Unit) {
        viewModelScope.launch {
            val result = authService.loginOffline(phone, pin)
            callback(result)
        }
    }

    fun register(nom: String, matricule: String, phone: String, pin: String, callback: (Result<UserEntity>) -> Unit) {
        viewModelScope.launch {
            println("************************************")
            val result = authService.registerOffline(nom, matricule, phone, pin)
            callback(result)
        }
    }


}