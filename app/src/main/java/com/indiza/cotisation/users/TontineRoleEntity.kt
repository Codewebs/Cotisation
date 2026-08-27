package com.indiza.cotisation.users

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tontine_roles",
    indices = [Index("tontineId", "role", unique = true)]
)
data class TontineRoleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val tontineId: Long,
    val userId: String?, // null = non attribué
    val role: Role,

    val isActive: Boolean = false,
    val assignedAt: Long? = null
)
