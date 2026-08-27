package com.indiza.cotisation.tontine

import androidx.room.Embedded
import androidx.room.Relation
import com.indiza.cotisation.caisses.CaisseEntity

data class TontineWithCaisses(
    @Embedded
    val tontine: TontineEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "tontineId"
    )
    val caisses: List<CaisseEntity>
)
