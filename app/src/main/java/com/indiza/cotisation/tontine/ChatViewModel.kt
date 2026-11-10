package com.indiza.cotisation.tontine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.users.SessionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

data class ChatUiState(
    val comments: List<CommentResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val selectedImageUri: Uri? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: TontineRepository,
    private val sessionDao: SessionDao
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()
    private var realtimeJob: Job? = null

    fun loadComments(tontineId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val list = repository.getComments(tontineId)
                _state.update {
                    it.copy(comments = list, isLoading = false, errorMessage = null)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Impossible de charger les messages."
                    )
                }
            }
        }
    }

    fun observeComments(tontineId: String) {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            try {
                repository.observeCommentChanges(tontineId).collect {
                    loadComments(tontineId)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        errorMessage = e.message
                            ?: "La connexion en direct au chat a échoué."
                    )
                }
            }
        }
    }

    fun onImageSelected(context: Context, uri: Uri) {
        _state.update { it.copy(selectedImageUri = uri) }
    }

    fun clearImage() {
        _state.update { it.copy(selectedImageUri = null) }
    }

    fun sendComment(context: Context, tontineId: String, content: String) {
        viewModelScope.launch {
            if (sessionDao.getCurrentSession() == null) {
                _state.update { it.copy(errorMessage = "Connectez-vous pour envoyer un message.") }
                return@launch
            }
            _state.update { it.copy(isSending = true, errorMessage = null) }
            try {
                val imageUri = _state.value.selectedImageUri
                val imageBytes = imageUri?.let { uriToJpegBytes(context, it) }
                if (imageUri != null && imageBytes == null) {
                    throw IllegalStateException("Impossible de lire l’image sélectionnée.")
                }
                repository.postComment(tontineId, content, imageBytes)
                _state.update { it.copy(isSending = false, selectedImageUri = null) }
                loadComments(tontineId)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSending = false,
                        errorMessage = e.message ?: "Impossible d’envoyer le message."
                    )
                }
            }
        }
    }

    private fun uriToJpegBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                ?: return null
            val outputStream = ByteArrayOutputStream()
            
            // Redimensionner si nécessaire pour éviter les dépassements de mémoire
            val scaledBitmap = if (bitmap.width > 800) {
                val ratio = 800f / bitmap.width
                Bitmap.createScaledBitmap(bitmap, 800, (bitmap.height * ratio).toInt(), true)
            } else bitmap

            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}
