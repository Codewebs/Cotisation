package com.indiza.cotisation.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val entityType: String,
    val entityLocalId: Long,
    val action: SyncAction,

    val createdAt: Long = System.currentTimeMillis()
)
