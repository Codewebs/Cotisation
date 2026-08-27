package com.indiza.cotisation.sync

import com.indiza.cotisation.caisses.CaisseDao
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
    private val syncQueueDao: SyncQueueDao,
    private val remoteApi: RemoteApi
) {

    suspend fun sync() {
        val actions = syncQueueDao.getPendingActions()

        actions.forEach { action ->
            try {
                when (action.entityType) {
                    "TONTINE" -> syncTontine(action)
                    "CAISSE" -> syncCaisse(action)
                    //"USER" -> syncUser(action) // ← Ajoutez USER
                }
                syncQueueDao.remove(action)
            } catch (e: Exception) {
                // On laisse en queue pour retry
            }
        }
    }


    private suspend fun syncTontine(action: SyncQueueEntity) {
        val tontine = tontineDao.getById(action.entityLocalId) ?: return

        when (action.action) {
            SyncAction.CREATE -> {
                val remoteId = remoteApi.createTontine(tontine.toSyncDto())
                tontineDao.updateRemoteInfo(
                    id = tontine.id,
                    remoteId = remoteId,
                    syncStatus = SyncStatus.SYNCED
                )
            }

            SyncAction.UPDATE -> {
                tontine.remoteId?.let {
                    remoteApi.updateTontine(it, tontine.toSyncDto())
                    tontineDao.updateSyncStatus(tontine.id, SyncStatus.SYNCED)
                }
            }

            SyncAction.DELETE -> {
                tontine.remoteId?.let {
                    remoteApi.deleteTontine(it)
                }
            }
        }
    }

    private suspend fun syncCaisse(action: SyncQueueEntity) {

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


