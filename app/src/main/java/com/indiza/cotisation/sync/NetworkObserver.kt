package com.indiza.cotisation.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

interface ConnectivityObserver {
    val status: Flow<Status>

    enum class Status {
        Available, Unavailable, Losing, Lost
    }
}

class NetworkConnectivityObserver(context: Context) : ConnectivityObserver {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val status: Flow<ConnectivityObserver.Status> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                launch { send(ConnectivityObserver.Status.Available) }
            }

            override fun onLost(network: Network) {
                launch { send(ConnectivityObserver.Status.Lost) }
            }

            override fun onUnavailable() {
                launch { send(ConnectivityObserver.Status.Unavailable) }
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}

@Composable
fun OfflineBanner(status: ConnectivityObserver.Status) {
    var showSuccess by remember { mutableStateOf(false) }
    val isOffline = status != ConnectivityObserver.Status.Available

    // Détecter le passage de Offline à Online
    LaunchedEffect(status) {
        if (status == ConnectivityObserver.Status.Available) {
            showSuccess = true
            delay(3000) // Afficher le message de succès pendant 3s
            showSuccess = false
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Bannière Erreur (Hors-ligne)
        AnimatedVisibility(
            visible = isOffline,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            BannerContent(
                message = "Mode Hors-ligne - Synchro en attente",
                icon = Icons.Default.CloudOff,
                backgroundColor = Color(0xFFE57373)
            )
        }

        // Bannière Succès (Retour en ligne)
        AnimatedVisibility(
            visible = !isOffline && showSuccess,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            BannerContent(
                message = "Connexion rétablie ! Synchronisation...",
                icon = Icons.Default.CloudDone,
                backgroundColor = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
private fun BannerContent(message: String, icon: ImageVector, backgroundColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Text(message, color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}