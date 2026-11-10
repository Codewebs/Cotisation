package com.indiza.cotisation.network

import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject
import javax.inject.Singleton

import android.util.Log

@Singleton
class PushTokenRegistrar @Inject constructor(
    private val supabase: SupabaseClient,
    private val configuration: SupabaseConfiguration
) {
    suspend fun registerCurrentDevice(): Boolean {
        return try {
            configuration.requireConfigured()
            supabase.auth.awaitInitialization()
            val user = supabase.auth.currentUserOrNull()
            if (user == null) {
                Log.w("CotisationPush", "PushTokenRegistrar.registerCurrentDevice: No authenticated user found.")
                return false
            }

            val token = awaitFcmToken()
            Log.i("CotisationPush", "PushTokenRegistrar: FCM token retrieved: ${token.take(15)}...")
            registerToken(token)
        } catch (e: Exception) {
            Log.e("CotisationPush", "PushTokenRegistrar.registerCurrentDevice failed: ${e.message}", e)
            false
        }
    }

    suspend fun registerToken(token: String): Boolean {
        return try {
            configuration.requireConfigured()
            supabase.auth.awaitInitialization()
            val user = supabase.auth.currentUserOrNull()
            if (user == null) {
                Log.w("CotisationPush", "PushTokenRegistrar.registerToken: No authenticated user in Supabase.")
                return false
            }
            check(token.isNotBlank()) { "Firebase returned an empty FCM token." }

            Log.i("CotisationPush", "PushTokenRegistrar: Registering FCM token for user ${user.id} in user_devices...")
            supabase.postgrest.rpc(
                function = "register_push_device",
                parameters = buildJsonObject { put("p_fcm_token", token) }
            )
            Log.i("CotisationPush", "PushTokenRegistrar: FCM token successfully registered in Supabase user_devices!")
            true
        } catch (e: Exception) {
            Log.e("CotisationPush", "PushTokenRegistrar.registerToken error: ${e.message}", e)
            false
        }
    }

    suspend fun unregisterCurrentDevice() {
        configuration.requireConfigured()
        supabase.auth.awaitInitialization()
        if (supabase.auth.currentUserOrNull() == null) return

        val token = awaitFcmToken()
        supabase.from("user_devices").delete {
            filter { eq("fcm_token", token) }
        }
    }

    private suspend fun awaitFcmToken(): String = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Unable to retrieve the FCM token.")
                )
            }
        }
    }
}
