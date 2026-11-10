package com.indiza.cotisation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

import com.indiza.cotisation.ui.theme.CotisationTheme
import com.indiza.cotisation.sync.ConnectivityObserver
import dagger.hilt.android.AndroidEntryPoint

import javax.inject.Inject

@AndroidEntryPoint // Nécessaire si tu utilises Hilt
class MainActivity : ComponentActivity() {
    private var notificationReceiptId by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.i(
                NOTIFICATION_TAG,
                if (granted) "POST_NOTIFICATIONS permission granted."
                else "POST_NOTIFICATIONS permission denied."
            )
        }

    @Inject
    lateinit var connectivityObserver: ConnectivityObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        notificationReceiptId = receiptIdFrom(intent)

        setContent {
            CotisationTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppEntryPoint(
                        connectivityObserver = connectivityObserver,
                        notificationReceiptId = notificationReceiptId,
                        onNotificationReceiptHandled = { notificationReceiptId = null }
                    )
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(NOTIFICATION_TAG, "Requesting POST_NOTIFICATIONS permission.")
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "cotisation_push_v2",
                "Notifications Cotisation",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            getSystemService(android.app.NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationReceiptId = receiptIdFrom(intent)
    }

    private fun receiptIdFrom(intent: android.content.Intent?): String? {
        intent ?: return null
        if (intent.getStringExtra(EXTRA_NOTIFICATION_TYPE) !in RECEIPT_NOTIFICATION_TYPES) {
            return null
        }
        return intent.getStringExtra(EXTRA_NOTIFICATION_REFERENCE_ID)
            ?.takeIf(String::isNotBlank)
    }

    private companion object {
        const val NOTIFICATION_TAG = "CotisationPush"
        const val EXTRA_NOTIFICATION_REFERENCE_ID = "notification_reference_id"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        val RECEIPT_NOTIFICATION_TYPES = setOf("RECEIPT", "COTISATION_RECEIPT")
    }
}
