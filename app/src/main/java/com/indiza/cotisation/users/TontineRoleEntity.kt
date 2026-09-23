package com.indiza.cotisation.users

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.indiza.cotisation.tontine.TontineEntity

@Entity(
    tableName = "tontine_roles",
    indices = [Index("tontineId", "role", unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = TontineEntity::class,
            parentColumns = ["id"],
            childColumns = ["tontineId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
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
