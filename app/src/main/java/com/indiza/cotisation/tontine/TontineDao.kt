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
    SET libelle = :libelle, exercice = :exercice, dateDebut = :dateDebut, dateFin = :dateFin, updatedAt = :updatedAt
    WHERE id = :id
""")
    suspend fun updateTontineInfo(
        id: Long,
        libelle: String,
        exercice: String,
        dateDebut: LocalDate,
        dateFin: LocalDate,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE tontines SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus)

    @Query("""SELECT * FROM tontines WHERE code = :query OR libelle LIKE '%' || :query || '%' ORDER BY id DESC LIMIT 50 """)
    suspend fun searchTontines(query: String): List<TontineEntity>

    @Upsert
    suspend fun upsert(tontine: TontineEntity)

    @Query("SELECT * FROM tontines WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String?): TontineEntity?

    @Query("DELETE FROM tontines WHERE id = :tontineId")
    suspend fun deleteTontineById(tontineId: Long)

    @Query("""
    UPDATE tontines 
    SET libelle = :libelle, exercice = :exercice, dateDebut = :dateDebut, dateFin = :dateFin 
    WHERE id = :id
""")
    suspend fun updateTontineInfo(id: Long, libelle: String, exercice: String, dateDebut: LocalDate, dateFin: LocalDate)

}
