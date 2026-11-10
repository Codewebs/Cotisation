package com.indiza.cotisation.bank

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {
    @Query("SELECT * FROM caisse_cycles WHERE caisseId = :caisseId AND estCloture = 0 LIMIT 1")
    fun getCycleActif(caisseId: Long): Flow<CycleEntity?>

    @Query("SELECT * FROM caisse_cycles WHERE id = :id")
    suspend fun getCycleById(id: Long): CycleEntity?

    @Insert
    suspend fun insertCycle(cycle: CycleEntity) : Long

    @Query("SELECT * FROM caisse_cycles WHERE id = :cycleId LIMIT 1")
    fun getCycleParId(cycleId: Long): Flow<CycleEntity?>


    @Update
    suspend fun updateCycle(cycle: CycleEntity)

    @Query("""
        UPDATE caisse_cycles
        SET estCloture = 1, dateFin = :dateFin
        WHERE id = :cycleId
    """)
    suspend fun closeCycle(cycleId: Long, dateFin: Long = System.currentTimeMillis())

    @Query("SELECT * FROM caisse_cycles WHERE caisseId = :caisseId ORDER BY numeroCycle DESC")
    fun getAllCycles(caisseId: Long): Flow<List<CycleEntity>>

    @Query("SELECT * FROM caisse_cycles WHERE caisseId = :caisseId AND estCloture = 0 LIMIT 1")
    suspend fun getCycleActifSnapshot(caisseId: Long): CycleEntity?

    @Query("SELECT MAX(numeroCycle) FROM caisse_cycles WHERE caisseId = :caisseId")
    suspend fun getMaxNumeroCycle(caisseId: Long): Int?

    @Query("SELECT * FROM caisse_cycles WHERE caisseId = :caisseId AND numeroCycle = :numeroCycle LIMIT 1")
    suspend fun getCycleByNumero(caisseId: Long, numeroCycle: Int): CycleEntity?
}