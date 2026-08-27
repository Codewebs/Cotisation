package com.indiza.cotisation.tontine

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.users.Role
import com.indiza.cotisation.users.UserEntity

@Entity(
    tableName = "tontine_members",
    indices = [
        Index("tontineId", "userId", unique = true),
        Index("tontineId", "phone", unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = TontineEntity::class,
            parentColumns = ["id"],
            childColumns = ["tontineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TontineMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val tontineId: Long,
    val userId: String? = null,
    val matricule: String? = null,
    val nomSaisi: String,
    val phone: String,

    val role: Role,
    val joinedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

data class TontineWithCaissesAndRole(
    val tontine: TontineEntity,
    val caisses: List<CaisseEntity>,
    val role: Role
)

data class TontineMemberWithUser(
    @Embedded
    val member: TontineMemberEntity,

    @Relation(
        parentColumn = "userId",
        entityColumn = "id"
    )
    val user: UserEntity?
)