package com.indiza.cotisation.tontine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.users.SessionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

data class ChatUiState(
    val comments: List<CommentResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val selectedImageBase64: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: TontineRepository,
    private val sessionDao: SessionDao
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    fun loadComments(tontineId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val list = repository.getComments(tontineId)
                _state.update { it.copy(comments = list, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onImageSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            val base64 = uriToBase64(context, uri)
            _state.update { it.copy(selectedImageBase64 = base64) }
        }
    }

    fun clearImage() {
        _state.update { it.copy(selectedImageBase64 = null) }
    }

    fun sendComment(tontineId: String, content: String) {
        viewModelScope.launch {
            val session = sessionDao.getCurrentSession() ?: return@launch
            _state.update { it.copy(isSending = true) }
            try {
                repository.postComment(tontineId, session.userId, content, _state.value.selectedImageBase64)
                _state.update { it.copy(isSending = false, selectedImageBase64 = null) }
                loadComments(tontineId)
            } catch (e: Exception) {
                _state.update { it.copy(isSending = false) }
            }
        }
    }

    private fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            
            // Redimensionner si nécessaire pour éviter les dépassements de mémoire
            val scaledBitmap = if (bitmap.width > 800) {
                val ratio = 800f / bitmap.width
                Bitmap.createScaledBitmap(bitmap, 800, (bitmap.height * ratio).toInt(), true)
            } else bitmap

            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }
}
