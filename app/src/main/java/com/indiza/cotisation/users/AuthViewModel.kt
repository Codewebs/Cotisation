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

    fun login(email: String, password: String, callback: (Result<SessionEntity>) -> Unit) {
        viewModelScope.launch {
            val result = authService.loginOnline(email, password)
            callback(result)
        }
    }

    fun register(
        nom: String,
        matricule: String,
        phone: String,
        email: String,
        password: String,
        callback: (Result<RegistrationResult>) -> Unit
    ) {
        viewModelScope.launch {
            val result = authService.registerOnline(nom, matricule, phone, email, password)
            callback(result)
        }
    }


}