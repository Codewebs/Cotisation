package com.indiza.cotisation.bank

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.tontine.TontineMemberEntity
import com.indiza.cotisation.tontinemembers.ParticipantWithParts


@Entity(
    tableName = "caisse_tours",
    foreignKeys = [
        ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class TourEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val cycleId: Long,
    val numeroTour: Int,
    val dateTour: Long = System.currentTimeMillis(),
    val montantTotalDistribue: Long = 0L,
    val estTermine: Boolean = false
)

@Entity(
    tableName = "tour_beneficiaires",
    primaryKeys = ["tourId", "memberId"],
    foreignKeys = [
        ForeignKey(entity = TourEntity::class, parentColumns = ["id"], childColumns = ["tourId"]),
        ForeignKey(entity = TontineMemberEntity::class, parentColumns = ["id"], childColumns = ["memberId"])
    ]
)
data class TourBeneficiaireEntity(
    val tourId: Long,
    val memberId: Long,
    val nombrePartsAttribuees: Int = 1,
    val montantRecu: Long
)

data class BeneficiaireSelection(
    val participant: ParticipantWithParts,
    var partsToLiquider: Int = 1
)

data class CreateTourUiState(
    val caisse: CaisseEntity? = null,
    val participants: List<ParticipantWithParts> = emptyList()
)