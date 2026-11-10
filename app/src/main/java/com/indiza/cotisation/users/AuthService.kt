package com.indiza.cotisation.users

import android.util.Log
import com.indiza.cotisation.network.PushTokenRegistrar
import com.indiza.cotisation.network.SupabaseConfiguration
import com.indiza.cotisation.sync.SyncStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RegistrationResult {
    data class SignedIn(val user: UserEntity) : RegistrationResult
    data class ConfirmationRequired(val email: String) : RegistrationResult
}

@Singleton
class AuthService @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionDao: SessionDao,
    private val supabase: SupabaseClient,
    private val configuration: SupabaseConfiguration,
    private val pushTokenRegistrar: PushTokenRegistrar,
) {
    suspend fun registerOnline(
        nom: String,
        matricule: String,
        phone: String,
        email: String,
        password: String,
    ): Result<RegistrationResult> = runCatching {
        println("AUTH_DEBUG: === REGISTER ONLINE STARTED ===")
        println("AUTH_DEBUG: Email: $email, Nom: $nom, Phone: $phone, Matricule: $matricule")
        configuration.requireConfigured()
        val trimmedEmail = email.trim()
        val trimmedNom = nom.trim()
        val cleanedPhone = phone.trim().ifBlank { null }
        val cleanedMatricule = matricule.trim().ifBlank { null }

        println("AUTH_DEBUG: Calling supabase.auth.signUpWith(Email)...")
        supabase.auth.signUpWith(Email) {
            this.email = trimmedEmail
            this.password = password
            data = kotlinx.serialization.json.buildJsonObject {
                put("nom", trimmedNom)
                cleanedMatricule?.let { put("matricule", it) }
                cleanedPhone?.let { put("phone", it) }
            }
        }
        println("AUTH_DEBUG: signUpWith call finished successfully.")

        val authUser = supabase.auth.currentUserOrNull()
            ?.takeIf { it.email.equals(trimmedEmail, ignoreCase = true) }

        println("AUTH_DEBUG: currentUserOrNull after signUp = ${authUser?.id}")

        if (authUser == null) {
            println("AUTH_DEBUG: Result -> ConfirmationRequired (Email confirmation is enabled in Supabase)")
            return@runCatching RegistrationResult.ConfirmationRequired(trimmedEmail)
        }

        var profile = loadProfileOrNull(authUser.id)
        if (profile == null) {
            println("AUTH_DEBUG: Profile row not found in public.profiles, upserting...")
            val newProfile = ProfileRecord(
                id = authUser.id,
                nom = trimmedNom,
                email = trimmedEmail,
                phone = cleanedPhone,
                matricule = cleanedMatricule,
            )
            supabase.from("profiles").upsert(newProfile)
            profile = loadProfileOrNull(authUser.id) ?: newProfile
        }

        val user = profile.toEntity(authUser.id, trimmedEmail)
        userRepository.saveUser(user)
        createLocalSession(authUser.id)
        registerPushTokenIfPossible()
        println("AUTH_DEBUG: === REGISTER SUCCESSFUL (SignedIn) ===")
        RegistrationResult.SignedIn(user)
    }.onFailure { e ->
        println("AUTH_DEBUG: === REGISTER FAILED: ${e.message} ===")
        e.printStackTrace()
    }

    suspend fun loginOnline(email: String, password: String): Result<SessionEntity> = runCatching {
        println("AUTH_DEBUG: === LOGIN ONLINE STARTED ===")
        println("AUTH_DEBUG: Email: $email")
        configuration.requireConfigured()
        val trimmedEmail = email.trim()
        supabase.auth.signInWith(Email) {
            this.email = trimmedEmail
            this.password = password
        }
        println("AUTH_DEBUG: signInWith call finished successfully.")
        val authUser = supabase.auth.currentUserOrNull()
            ?: throw IllegalStateException("Supabase authentication returned no user.")
        println("AUTH_DEBUG: Logged in authUser ID = ${authUser.id}")
        
        var profile = loadProfileOrNull(authUser.id)
        if (profile == null) {
            println("AUTH_DEBUG: Profile row not found on login, upserting...")
            val newProfile = ProfileRecord(
                id = authUser.id,
                nom = authUser.email ?: trimmedEmail,
                email = trimmedEmail,
            )
            supabase.from("profiles").upsert(newProfile)
            profile = loadProfileOrNull(authUser.id) ?: newProfile
        }

        userRepository.saveUser(profile.toEntity(authUser.id, trimmedEmail))
        val session = createLocalSession(authUser.id)
        registerPushTokenIfPossible()
        println("AUTH_DEBUG: === LOGIN SUCCESSFUL ===")
        session
    }.onFailure { e ->
        println("AUTH_DEBUG: === LOGIN FAILED: ${e.message} ===")
        e.printStackTrace()
    }

    private suspend fun loadProfileOrNull(userId: String): ProfileRecord? =
        supabase.from("profiles").select {
            filter { eq("id", userId) }
        }.decodeSingleOrNull()

    private fun ProfileRecord.toEntity(userId: String, fallbackEmail: String) = UserEntity(
        id = userId,
        nom = nom.orEmpty(),
        phone = phone.orEmpty(),
        matricule = matricule.orEmpty(),
        email = email ?: fallbackEmail,
        isActive = isActive,
        isVerified = isVerified,
        remoteId = userId,
        syncStatus = SyncStatus.SYNCED,
        updatedAt = System.currentTimeMillis(),
    )

    private suspend fun createLocalSession(userId: String): SessionEntity {
        val session = SessionEntity(userId = userId, tontineId = 0, role = Role.MEMBRE)
        sessionDao.saveSession(session)
        return session
    }

    private suspend fun registerPushTokenIfPossible() {
        runCatching { pushTokenRegistrar.registerCurrentDevice() }
            .onFailure { Log.w("CotisationPush", "Unable to register FCM token after sign-in.", it) }
    }

    suspend fun getCurrentSession(): SessionEntity? = sessionDao.getCurrentSession()
}

@Serializable
private data class ProfileRecord(
    val id: String,
    val nom: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val matricule: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_verified") val isVerified: Boolean = false,
)
