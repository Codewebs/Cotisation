package com.indiza.cotisation.users

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val sessionDao: SessionDao
) {
    /**
     * Récupère le rôle de l'utilisateur connecté pour la session actuelle.
     * Si aucune session, retourne MEMBRE par défaut.
     */
    suspend fun getCurrentRole(): Role {
        return try {
            val session = sessionDao.getCurrentSession()
            session?.role ?: Role.MEMBRE
        } catch (e: Exception) {
            Role.MEMBRE
        }
    }
}