package com.indiza.cotisation.users

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey val id: Int = 1,
    val userId: String,
    val tontineId: Long,
    val role: Role
)
