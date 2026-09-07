package com.indiza.cotisation.bank

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PretDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPret(pret: PretEntity): Long

    @Update
    suspend fun updatePret(pret: PretEntity)

    @Query("SELECT * FROM prets WHERE caisseId = :caisseId")
    fun getPretsByCaisse(caisseId: Long): Flow<List<PretEntity>>

    @Query("SELECT * FROM prets WHERE memberId = :memberId")
    fun getPretsByMember(memberId: Long): Flow<List<PretEntity>>

    @Query("SELECT * FROM prets WHERE id = :id")
    suspend fun getPretById(id: Long): PretEntity?
}
