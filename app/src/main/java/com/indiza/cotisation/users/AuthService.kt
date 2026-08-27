package com.indiza.cotisation.users

import com.indiza.cotisation.sync.SyncEngine
import com.indiza.cotisation.sync.SyncStatus
import com.indiza.cotisation.tools.CryptoUtils.generateSalt
import com.indiza.cotisation.tools.CryptoUtils.hashWithSalt
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionDao: SessionDao,
    private val syncEngine: SyncEngine
) {

    suspend fun registerOffline(nom: String, matricule:String, phone: String, pin: String): Result<UserEntity> {
        return try {
            println("*****register offline return********")
            // Générer un ID local temporaire
            val localId = "local_${UUID.randomUUID()}"

            // Hasher le PIN
            val (hash, salt) = hashPin(pin)

            val user = UserEntity(
                id = localId,
                nom = nom,
                phone = phone,
                pin = hash,
                pinSalt = salt,
                matricule = matricule,
                syncStatus = SyncStatus.PENDING_CREATE
            )

            userRepository.saveUser(user)
            println("Créer une session locale")
            println("--------------------------------------------------------------")
            // Créer une session locale
            createLocalSession(user.id)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginOffline(phone: String, pin: String): Result<SessionEntity> {
        return try {
            val user = userRepository.getUserByPhone(phone)
                ?: return Result.failure(Exception("Utilisateur non trouvé"))

            if (!verifyPin(pin, user.pin, user.pinSalt)) {
                return Result.failure(Exception("PIN incorrect"))
            }

            val session = createLocalSession(user.id)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    private suspend fun createLocalSession(userId: String): SessionEntity {
        println(" tohether --- 540 000")
        val session = SessionEntity(
            userId = userId,
            tontineId = 0, // Pas encore de tontine
            role = Role.MEMBRE
        )
        sessionDao.saveSession(session)
        println("XXXXXXXXXXXXXX tohether --- 540 000")
        return session
    }

    private fun hashPin(pin: String): Pair<String, String> {
        val salt = generateSalt()
        val hash = hashWithSalt(pin, salt)
        return Pair(hash, salt)
    }

    private fun verifyPin(pin: String, storedHash: String?, salt: String?): Boolean {
        if (storedHash == null || salt == null) return false
        val computedHash = hashWithSalt(pin, salt)
        return computedHash == storedHash
    }

    suspend fun getCurrentSession(): SessionEntity? {
        return sessionDao.getCurrentSession()
    }
}