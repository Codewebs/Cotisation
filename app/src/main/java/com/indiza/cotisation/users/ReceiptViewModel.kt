package com.indiza.cotisation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.network.RemoteApi
import com.indiza.cotisation.tontine.TransactionApiDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiptUiState(
    val isLoading: Boolean = false,
    val transaction: TransactionApiDto? = null,
    val error: String? = null
)

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val remoteApi: RemoteApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptUiState())
    val uiState = _uiState.asStateFlow()

    fun loadTransaction(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val tx = remoteApi.getTransactionDetails(id)
                _uiState.update { it.copy(isLoading = false, transaction = tx) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Impossible de charger le reçu") }
            }
        }
    }
}
