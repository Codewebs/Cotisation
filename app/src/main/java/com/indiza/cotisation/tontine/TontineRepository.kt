package com.indiza.cotisation.tontine

import androidx.room.Transaction
import com.indiza.cotisation.bank.CycleDao
import com.indiza.cotisation.bank.CycleEntity
import com.indiza.cotisation.caisses.CaisseDraft
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.users.Role
import com.indiza.cotisation.users.SessionDao
import com.indiza.cotisation.users.UserDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class TontineRepository @Inject constructor(
    private val dao: TontineDao,
    private val tontineMemberDao: TontineMemberDao,
    private val userDao: UserDao,
    private val cycleDao: CycleDao,
    private val sessionDao: SessionDao,
    private val apiService: TontineApi
)
{

    suspend fun getUserTontinesFlow(userId: String): Flow<List<TontineWithCaisses>> {
        return tontineMemberDao.getUserTontinesWithCaisses(userId)
    }

    fun getTontineWithCaisses(tontineId: Long): Flow<TontineWithCaisses> {
        return dao.getTontineWithCaisses(tontineId)
    }

    suspend fun insertCaisse(tontineId: Long, caisseDraft: CaisseDraft) {
        val caisseEntity = CaisseEntity(
            tontineId = tontineId,
            type = caisseDraft.type,
            libelle = caisseDraft.libelle,
            montantFixe = caisseDraft.montantFixe,
            montantMinimum = caisseDraft.montantMinimum,
            frequence = caisseDraft.frequence,
            dureeCycles = caisseDraft.dureeCycles,
            regulariteImposee = caisseDraft.regulariteImposee,
            redistribution = caisseDraft.redistribution,
            penaliteRetardFixe = caisseDraft.penaliteRetardFixe,
            penaliteEchecFixe = caisseDraft.penaliteEchecFixe,
            dateLimiteJour = caisseDraft.dateLimiteJour
        )
        val newCaisseId = dao.insertCaisse(caisseEntity)

        val premierCycle = CycleEntity(
            caisseId = newCaisseId,
            numeroCycle = 1,
            dateDebut = System.currentTimeMillis(),
            estCloture = false
        )

        cycleDao.insertCycle(premierCycle)

    }

    suspend fun searchTontines(query: String): List<TontineEntity> {
        return try {
            // D'abord essayer l'API
            val remoteResults = apiService.searchTontines(query)

            // Sauvegarder localement les résultats
            remoteResults.forEach { remoteTontine ->
                dao.upsert(remoteTontine)
            }

            remoteResults

        } catch (e: Exception) {
            // Fallback: recherche locale
            dao.searchTontines(query)
        }
    }

    suspend fun getCurrentUserId(): String? {
        return sessionDao.getCurrentSession()?.userId
    }

    suspend fun getUserTontines(): List<TontineWithCaisses> {
        val userId = getCurrentUserId() ?: return emptyList()
        return tontineMemberDao.getUserTontinesWithCaisses(userId).first()
    }

    @Transaction
    suspend fun insertTontine(
        libelle: String,
        exercice: String,
        dateDebut: LocalDate,
        dateFin: LocalDate
    ): Long {
        // Créer la tontine
        val tontineId = dao.insertTontine(
            TontineEntity(
                libelle = libelle,
                exercice = exercice,
                dateDebut = dateDebut,
                dateFin = dateFin
            )
        )

        // Ajouter le créateur comme INITIATEUR
        val userId = getCurrentUserId()
        userId?.let { id->
            val currentUser = userDao.getUserById(id)
            tontineMemberDao.insertMember(
                TontineMemberEntity(
                    tontineId = tontineId,
                    userId = id,
                    nomSaisi = currentUser?.nom ?: "Administrateur", // ✅ AJOUTÉ
                    phone = currentUser?.phone ?: "",
                    matricule = currentUser?.matricule ?: "",
                    role = Role.INITIATEUR
                )
            )
        }

        return tontineId
    }
    suspend fun getUserTontinesWithRole(userId: String): Flow<List<TontineWithCaissesAndRole>> {
        return tontineMemberDao.getUserTontinesWithCaisses(userId).map { list ->
            list.map { item ->
                val role = tontineMemberDao
                    .getUserRoleInTontine(item.tontine.id, userId)
                    ?: Role.MEMBRE

                TontineWithCaissesAndRole(
                    tontine = item.tontine,
                    caisses = item.caisses,
                    role = role
                )
            }
        }
    }

    suspend fun deleteTontineIfEmpty(tontineId: Long): Result<Unit> {


        return try {
            // 1. Vérifier le nombre de membres (On exclut l'initiateur ?
            // Ou on vérifie s'il n'y a personne d'autre)
            val memberCount = tontineMemberDao.getMemberCount(tontineId)

            // 2. Récupérer la tontine avec ses caisses pour vérifier
            val tontineWithCaisses = dao.getTontineWithCaisses(tontineId).first()

            // Condition : Uniquement l'initiateur (count <= 1) et 0 caisses
            if (memberCount > 1) {

                return Result.failure(Exception("Impossible de supprimer : des membres sont déjà inscrits."))
            }

            if (tontineWithCaisses.caisses.isNotEmpty()) {
                println("******* °°°°°° Tentative de suppression de la tontine")
                return Result.failure(Exception("Impossible de supprimer : des caisses sont déjà configurées."))
            }
            println("°°° x °° Tentative de suppression de la tontine")

            // Si tout est vide, on supprime
            dao.deleteTontineById(tontineId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTontineBaseInfo(
        tontineId: Long,
        libelle: String,
        exercice: String,
        dateDebut: LocalDate,
        dateFin: LocalDate
    ) {
        dao.updateTontineInfo(tontineId, libelle, exercice, dateDebut, dateFin)
    }

}
