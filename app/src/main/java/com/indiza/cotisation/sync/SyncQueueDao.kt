package com.indiza.cotisation.sync

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SyncQueueDao {

    @Insert
    suspend fun enqueue(action: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getPendingActions(): List<SyncQueueEntity>

    @Delete
    suspend fun remove(action: SyncQueueEntity)
}
