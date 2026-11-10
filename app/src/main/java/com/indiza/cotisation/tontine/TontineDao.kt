package com.indiza.cotisation.tontine

import androidx.room.*
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.sync.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TontineDao {

    // 🔹 Tontines
    @Insert
    suspend fun insertTontine(tontine: TontineEntity): Long

    @Query("SELECT * FROM tontines ")
    fun getAllTontines(): Flow<List<TontineEntity>>

    @Transaction
    @Query("SELECT * FROM tontines WHERE id = :tontineId")
    fun getTontineWithCaisses(tontineId: Long): Flow<TontineWithCaisses>

    // 🔹 Caisses
    @Insert
    suspend fun insertCaisse(caisse: CaisseEntity) : Long

    @Insert
    suspend fun insertCaisses(caisses: List<CaisseEntity>)

    @Query("SELECT * FROM tontines WHERE id = :id")
    suspend fun getById(id: Long): TontineEntity?

    @Transaction
    @Query("SELECT * FROM tontines WHERE isOnline = 0 ORDER BY dateDebut DESC")
    fun getLocalTontinesWithCaisses(): Flow<List<TontineWithCaisses>>

    @Transaction
    @Query("SELECT * FROM tontines ORDER BY dateDebut DESC")
    fun getAllTontinesWithCaisses(): Flow<List<TontineWithCaisses>>


    @Query("""
    UPDATE tontines SET remoteId = :remoteId, syncStatus = :syncStatus WHERE id = :id """)
    suspend fun updateRemoteInfo(
        id: Long,
        remoteId: String,
        syncStatus: SyncStatus
    )

    @Query("""
    UPDATE tontines 
    SET libelle = :libelle, exercice = :exercice, dateDebut = :dateDebut, dateFin = :dateFin,
        isAnonymous = :isAnonymous, hideInfoForSimpleMembers = :hideInfoForSimpleMembers,
        updatedAt = :updatedAt
    WHERE id = :id
""")
    suspend fun updateTontineInfo(
        id: Long,
        libelle: String,
        exercice: String,
        dateDebut: LocalDate,
        dateFin: LocalDate,
        isAnonymous: Boolean,
        hideInfoForSimpleMembers: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE tontines SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus)

    @Query("UPDATE tontines SET isOnline = :isOnline WHERE id = :id")
    suspend fun updateOnlineStatus(id: Long, isOnline: Boolean)

    @Query("""SELECT * FROM tontines WHERE code = :query OR libelle LIKE '%' || :query || '%' ORDER BY id DESC LIMIT 50 """)
    suspend fun searchTontines(query: String): List<TontineEntity>

    @Upsert
    suspend fun upsert(tontine: TontineEntity)

    @Query("SELECT * FROM tontines WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String?): TontineEntity?

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM tontines t
            INNER JOIN tontine_members m ON t.id = m.tontineId
            WHERE lower(trim(t.libelle)) = lower(trim(:libelle))
              AND m.userId = :userId AND m.isInitiator = 1
        )
    """)
    suspend fun existsByNameAndInitiator(libelle: String, userId: String): Boolean

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM tontines t
            INNER JOIN tontine_members m ON t.id = m.tontineId
            WHERE lower(trim(t.libelle)) = lower(trim(:libelle))
              AND m.userId = :userId AND m.isInitiator = 1
              AND t.id != :excludingTontineId
        )
    """)
    suspend fun existsOtherNameForInitiator(
        libelle: String,
        userId: String,
        excludingTontineId: Long
    ): Boolean

    @Query("DELETE FROM tontines WHERE id = :tontineId")
    suspend fun deleteTontineById(tontineId: Long)

    @Query("DELETE FROM tontines")
    suspend fun deleteAllTontines()

}
