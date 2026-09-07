package com.indiza.cotisation.tontine

import androidx.room.Transaction
import com.indiza.cotisation.bank.CycleDao
import com.indiza.cotisation.bank.CycleEntity
import com.indiza.cotisation.bank.TransactionDao
import com.indiza.cotisation.bank.TransactionEntity
import com.indiza.cotisation.bank.TransactionType
import com.indiza.cotisation.caisses.CaisseDraft
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.caisses.CaisseType
import com.indiza.cotisation.caisses.FrequenceCotisation
import com.indiza.cotisation.caisses.CaisseDao
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
    private val caisseDao: CaisseDao,
    private val tontineMemberDao: TontineMemberDao,
    private val userDao: UserDao,
    private val cycleDao: CycleDao,
    private val transactionDao: TransactionDao,
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

    @Transaction
    suspend fun setupBasicLoanFund(tontineId: Long, amount: Long, sourceCaisseId: Long): Long {
        val allCaisses = caisseDao.getCaissesByTontine(tontineId).first()
        var caisseBasique = allCaisses.find { it.isBasique }
        
        val basiqueId = if (caisseBasique == null) {
            val newCaisse = CaisseEntity(
                tontineId = tontineId,
                type = CaisseType.PRET,
                libelle = "Caisse de Prêt Basique",
                frequence = FrequenceCotisation.MENSUEL,
                isBasique = true,
                possibilitePret = true,
                plafondPret = amount
            )
            val id = caisseDao.insertCaisse(newCaisse)
            cycleDao.insertCycle(CycleEntity(caisseId = id, numeroCycle = 1))
            caisseBasique = newCaisse.copy(id = id)
            id
        } else {
            caisseDao.updateCaisse(caisseBasique.copy(plafondPret = (caisseBasique.plafondPret ?: 0L) + amount))
            caisseBasique.id
        }

        val sourceCycle = cycleDao.getCycleActifSnapshot(sourceCaisseId)
        val targetCycle = cycleDao.getCycleActifSnapshot(basiqueId)
        
        val currentUserId = tontineMemberDao.getTontineMembers(tontineId).find { it.role == Role.PRESIDENT || it.role == Role.TRESORIER }?.id ?: 0L

        transactionDao.insertTransaction(
            TransactionEntity(
                caisseId = sourceCaisseId,
                memberId = currentUserId,
                cycleId = sourceCycle?.id ?: 0L,
                montant = -amount,
                type = TransactionType.TRANSFERT_SORTIE,
                motif = "Transfert vers Caisse de Prêt Basique"
            )
        )

        transactionDao.insertTransaction(
            TransactionEntity(
                caisseId = basiqueId,
                memberId = currentUserId,
                cycleId = targetCycle?.id ?: 0L,
                montant = amount,
                type = TransactionType.TRANSFERT_ENTREE,
                motif = "Dotation initiale/augmentation depuis ${allCaisses.find { it.id == sourceCaisseId }?.libelle}"
            )
        )

        return basiqueId
    }

    suspend fun searchTontines(query: String): List<TontineEntity> {
        return try {
            val remoteResults = apiService.searchTontines(query)
            val entities = remoteResults.map { dto ->
                val existing = dao.getByRemoteId(dto.id)
                dto.toEntity().copy(id = existing?.id ?: 0L)
            }
            entities.forEach { entity ->
                dao.upsert(entity)
            }
            entities
        } catch (e: Exception) {
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
        val tontineId = dao.insertTontine(
            TontineEntity(
                libelle = libelle,
                exercice = exercice,
                dateDebut = dateDebut,
                dateFin = dateFin
            )
        )

        val userId = getCurrentUserId()
        userId?.let { id->
            val currentUser = userDao.getUserById(id)
            tontineMemberDao.insertMember(
                TontineMemberEntity(
                    tontineId = tontineId,
                    userId = id,
                    nomSaisi = currentUser?.nom ?: "Administrateur",
                    phone = currentUser?.phone ?: "",
                    matricule = currentUser?.matricule ?: "",
                    role = Role.INITIATEUR,
                    isInitiator = true
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
            val memberCount = tontineMemberDao.getMemberCount(tontineId)
            val tontineWithCaisses = dao.getTontineWithCaisses(tontineId).first()
            if (memberCount > 1) {
                return Result.failure(Exception("Impossible de supprimer : des membres sont déjà inscrits."))
            }
            if (tontineWithCaisses.caisses.isNotEmpty()) {
                return Result.failure(Exception("Impossible de supprimer : des caisses sont déjà configurées."))
            }
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
