package com.indiza.cotisation.sync

import com.indiza.cotisation.caisses.CaisseDao
import com.indiza.cotisation.caisses.toSyncDto
import com.indiza.cotisation.network.RemoteApi
import com.indiza.cotisation.tontine.TontineDao
import com.indiza.cotisation.tontine.toSyncDto
import com.indiza.cotisation.users.UserDao
import com.indiza.cotisation.users.UserDto
import com.indiza.cotisation.users.UserEntity
import javax.inject.Inject

class SyncEngine @Inject constructor (
    private val tontineDao: TontineDao,
    private val caisseDao: CaisseDao,
    private val userDao: UserDao,
    private val sessionDao: com.indiza.cotisation.users.SessionDao,
    private val syncQueueDao: SyncQueueDao,
    private val remoteApi: RemoteApi,
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
        val currentUserId = sessionDao.getCurrentSession()?.userId
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



    // Extension pour convertir UserEntity en DTO
    private fun UserEntity.toUserDto(): UserDto {
        return UserDto(
            nom = this.nom,
            phone = this.phone,
            pin = this.pin ?: ""
        )
    }
}


