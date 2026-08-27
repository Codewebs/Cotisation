package com.indiza.cotisation.tontinemembers

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.tontine.TontineMemberEntity
import com.indiza.cotisation.tontine.TontineMemberWithUser

@Entity(
    tableName = "caisse_participants",
    primaryKeys = ["caisseId", "memberId"],
    foreignKeys = [
        ForeignKey(entity = CaisseEntity::class, parentColumns = ["id"], childColumns = ["caisseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TontineMemberEntity::class, parentColumns = ["id"], childColumns = ["memberId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class CaisseParticipantEntity(
    val caisseId: Long,
    val memberId: Long,
    val ordrePassage: Int? = null,
    val estBeneficiaire: Boolean = false,
    val nombreParts: Int = 1
)

data class ParticipantWithParts(
    @Embedded
    val memberWithUser: TontineMemberWithUser,
    @ColumnInfo(name = "nombreParts")
    val nombreParts: Int
)
