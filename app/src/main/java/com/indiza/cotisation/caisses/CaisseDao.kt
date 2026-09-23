package com.indiza.cotisation.caisses

import androidx.room.*
import com.indiza.cotisation.sync.SyncStatus
import com.indiza.cotisation.tontinemembers.CaisseParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaisseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaisse(caisse: CaisseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaisses(caisses: List<CaisseEntity>)

    @Update
    suspend fun updateCaisse(caisse: CaisseEntity)

    @Delete
    suspend fun deleteCaisse(caisse: CaisseEntity)

    // Récupérer toutes les caisses d'une tontine spécifique
    @Query("SELECT * FROM caisses WHERE tontineId = :tontineId ORDER BY libelle ASC")
    fun getCaissesByTontine(tontineId: Long): Flow<List<CaisseEntity>>

    // Récupérer une caisse précise par son ID
    @Query("SELECT * FROM caisses WHERE id = :caisseId")
    suspend fun getCaisseById(caisseId: Long): CaisseEntity?

    // Récupérer les caisses par type (ex: toutes les caisses de secours)
    @Query("SELECT * FROM caisses WHERE tontineId = :tontineId AND type = :type")
    fun getCaissesByType(tontineId: Long, type: CaisseType): Flow<List<CaisseEntity>>

    // Utile pour la synchronisation : récupérer les caisses non synchronisées
    @Query("SELECT * FROM caisses WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedCaisses(): List<CaisseEntity>

    // Correction Unresolved reference 'getById'
    @Query("SELECT * FROM caisses WHERE id = :id")
    suspend fun getById(id: Long): CaisseEntity?

    // Correction Unresolved reference 'getTontineRemoteId'
    @Query("SELECT remoteId FROM tontines WHERE id = :tontineId")
    suspend fun getTontineRemoteId(tontineId: Long): String?

    // Correction Unresolved reference 'updateSyncStatus'
    @Query("UPDATE caisses SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus)

    @Query("UPDATE caisses SET remoteId = :remoteId, syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateRemoteInfo(id: Long, remoteId: String, syncStatus: SyncStatus)

    @Upsert
    suspend fun upsert(caisse: CaisseEntity)

    @Query("SELECT * FROM caisses WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String?): CaisseEntity?

    @Query("DELETE FROM caisses WHERE id = :id")
    suspend fun deleteCaisseById(id: Long)

    @Query("SELECT * FROM tontines WHERE id = (SELECT tontineId FROM caisses WHERE id = :caisseId)")
    suspend fun getTontineByCaisse(caisseId: Long): com.indiza.cotisation.tontine.TontineEntity?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addParticipantToCaisse(participant: CaisseParticipantEntity)

    @Query("DELETE FROM caisse_participants WHERE caisseId = :caisseId AND memberId = :memberId")
    suspend fun removeParticipantFromCaisse(caisseId: Long, memberId: Long)

    @Query("SELECT * FROM caisse_participants WHERE caisseId = :caisseId")
    suspend fun getParticipantsByCaisseRaw(caisseId: Long): List<CaisseParticipantEntity>

    @Transaction
    suspend fun syncCaisseParticipants(caisseId: Long, selectedMemberIds: List<Long>) {
        // 1. Supprimer ceux qui ne sont plus sélectionnés

        // 2. Ajouter les nouveaux
        selectedMemberIds.forEachIndexed { index, memberId ->
            addParticipantToCaisse(CaisseParticipantEntity(
                caisseId = caisseId,
                memberId = memberId,
                ordrePassage = index + 1
            ))
        }
    }

}
