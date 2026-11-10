package com.indiza.cotisation.sync

import com.indiza.cotisation.caisses.CaisseDao
import com.indiza.cotisation.caisses.toSyncDto
import com.indiza.cotisation.bank.CycleDao
import com.indiza.cotisation.bank.TransactionDao
import com.indiza.cotisation.network.RemoteApi
import com.indiza.cotisation.tontine.TontineDao
import com.indiza.cotisation.tontine.TontineMemberDao
import com.indiza.cotisation.tontine.TransactionApiDto
import com.indiza.cotisation.tontine.transactionRemoteId
import com.indiza.cotisation.tontine.toSyncDto
import io.github.jan.supabase.gotrue.auth
import javax.inject.Inject

class SyncEngine @Inject constructor (
    private val tontineDao: TontineDao,
    private val caisseDao: CaisseDao,
    private val transactionDao: TransactionDao,
    private val cycleDao: CycleDao,
    private val tontineMemberDao: TontineMemberDao,
    private val sessionDao: com.indiza.cotisation.users.SessionDao,
    private val syncQueueDao: SyncQueueDao,
    private val remoteApi: RemoteApi,
    private val supabase: io.github.jan.supabase.SupabaseClient,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {

    suspend fun sync() {
        val actions = syncQueueDao.getPendingActions()
        if (actions.isEmpty()) return

        println("DEBUG_SYNC: Démarrage de la synchronisation (${actions.size} actions)")

        actions.forEach { action ->
            try {
                println("DEBUG_SYNC: Traitement action ${action.entityType} [${action.action}] LocalID: ${action.entityLocalId}")
                when (action.entityType) {
                    "TONTINE" -> syncTontine(action)
                    "CAISSE" -> syncCaisse(action)
                    "MEMBER" -> syncMember(action)
                    "TRANSACTION" -> syncTransaction(action)
                }
                syncQueueDao.remove(action)
                println("DEBUG_SYNC: Action ${action.entityType} terminée avec succès")
            } catch (e: Exception) {
                println("DEBUG_SYNC: ERREUR CRITIQUE lors de la synchro ${action.entityType}: ${e.message}")
                // Marquer l'entité en erreur pour l'UI
                try {
                    when(action.entityType) {
                        "TONTINE" -> tontineDao.updateSyncStatus(action.entityLocalId, SyncStatus.ERROR)
                        "CAISSE" -> caisseDao.updateSyncStatus(action.entityLocalId, SyncStatus.ERROR)
                    }
                } catch (ex: Exception) {}
                e.printStackTrace()
            }
        }

        // Mettre à jour le timestamp de dernière synchro
        val prefs = context.getSharedPreferences("sync_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()
    }


    private suspend fun syncTontine(action: SyncQueueEntity) {
        val tontine = tontineDao.getById(action.entityLocalId) ?: return
        supabase.auth.awaitInitialization()
        val currentUserId = supabase.auth.currentUserOrNull()?.id ?: sessionDao.getCurrentSession()?.userId ?: error("No authenticated user for sync")
        println("DEBUG_SYNC: Sync TONTINE: ${tontine.libelle} (RemoteID: ${tontine.remoteId})")

        when (action.action) {
            SyncAction.CREATE -> {
                println("DEBUG_SYNC: Appel API CREATE TONTINE pour ${tontine.libelle}")
                val remoteId = remoteApi.createTontine(tontine.toSyncDto().copy(creatorId = currentUserId))
                println("DEBUG_SYNC: Tontine créée sur serveur. RemoteID: $remoteId")
                tontineDao.updateRemoteInfo(
                    id = tontine.id,
                    remoteId = remoteId,
                    syncStatus = SyncStatus.SYNCED
                )
            }

            SyncAction.UPDATE -> {
                tontine.remoteId?.let {
                    println("DEBUG_SYNC: Appel API UPDATE TONTINE pour $it")
                    remoteApi.updateTontine(it, tontine.toSyncDto())
                    tontineDao.updateSyncStatus(tontine.id, SyncStatus.SYNCED)
                }
            }

            SyncAction.DELETE -> {
                tontine.remoteId?.let {
                    println("DEBUG_SYNC: Appel API DELETE TONTINE pour $it")
                    remoteApi.deleteTontine(it)
                }
            }
        }
    }

    private suspend fun syncCaisse(action: SyncQueueEntity) {
        val caisse = caisseDao.getById(action.entityLocalId) ?: return
        val tontineRemoteId = caisseDao.getTontineRemoteId(caisse.tontineId) ?: return
        println("DEBUG_SYNC: Sync CAISSE: ${caisse.libelle} (RemoteID: ${caisse.remoteId})")

        when (action.action) {
            SyncAction.CREATE -> {
                println("DEBUG_SYNC: Appel API CREATE CAISSE pour ${caisse.libelle}")
                val remoteId = remoteApi.createCaisse(caisse.toSyncDto(tontineRemoteId))
                println("DEBUG_SYNC: Caisse créée sur serveur. RemoteID: $remoteId")
                caisseDao.updateRemoteInfo(
                    id = caisse.id,
                    remoteId = remoteId,
                    syncStatus = SyncStatus.SYNCED
                )
            }
            SyncAction.UPDATE -> {
                caisse.remoteId?.let {
                    println("DEBUG_SYNC: Appel API UPDATE CAISSE pour $it")
                    remoteApi.updateCaisse(it, caisse.toSyncDto(tontineRemoteId))
                    caisseDao.updateSyncStatus(caisse.id, SyncStatus.SYNCED)
                }
            }
            SyncAction.DELETE -> {
                caisse.remoteId?.let {
                    println("DEBUG_SYNC: Appel API DELETE CAISSE pour $it")
                    remoteApi.deleteCaisse(it)
                }
            }
        }
    }

    private suspend fun syncTransaction(action: SyncQueueEntity) {
        val transaction = transactionDao.getTransactionById(action.entityLocalId) ?: return
        val caisse = caisseDao.getById(transaction.caisseId)
            ?: error("Caisse locale introuvable pour la transaction ${transaction.id}")
        val remoteCaisseId = caisse.remoteId
            ?: error("Caisse non synchronisée pour la transaction ${transaction.id}")
        val member = tontineMemberDao.getMemberById(transaction.memberId)
            ?: error("Membre local introuvable pour la transaction ${transaction.id}")
        val remoteMemberId = member.remoteId ?: member.userId
            ?: error("Membre non synchronisé pour la transaction ${transaction.id}")
        val cycle = cycleDao.getCycleById(transaction.cycleId)
            ?: error("Cycle local introuvable pour la transaction ${transaction.id}")

        remoteApi.createTransaction(
            TransactionApiDto(
                id = transactionRemoteId(transaction.id),
                caisseId = remoteCaisseId,
                memberId = remoteMemberId,
                cycleId = cycle.id.toString(),
                montant = transaction.montant,
                type = transaction.type.name,
                motif = transaction.motif,
                dateTransaction = transaction.dateTransaction,
                validatedById = transaction.validatedById
            ),
            cycle.numeroCycle
        )
    }

    private suspend fun syncMember(action: SyncQueueEntity) {
        val member = tontineMemberDao.getMemberById(action.entityLocalId) ?: return
        if (member.remoteId != null) return
        val remoteTontineId = tontineDao.getById(member.tontineId)?.remoteId
            ?: error("Tontine non synchronisée pour le membre ${member.id}")
        val remoteMember = remoteApi.addMember(
            tontineId = remoteTontineId,
            nom = member.nomSaisi,
            phone = member.phone,
            role = member.role.name
        )
        tontineMemberDao.updateMemberRemoteId(member.id, remoteMember.id)
    }
}
