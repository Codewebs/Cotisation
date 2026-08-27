package com.indiza.cotisation.users

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: SessionEntity)

    @Query("SELECT * FROM session WHERE id = 1")
    suspend fun getCurrentSession(): SessionEntity?

    @Query("SELECT COUNT(*) FROM session")
    suspend fun hasActiveSession(): Boolean

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("DELETE FROM session")
    suspend fun clearSession()
}