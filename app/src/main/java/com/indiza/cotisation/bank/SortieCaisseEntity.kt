package com.indiza.cotisation.bank

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MotifSortie {
    DEPART_TONTINE,
    DECES_PARENT,
    AIDE_SOCIALE,
    DECES_MEMBRE,
    AUTRE
}

@Entity(tableName = "sorties_caisse")
data class SortieCaisseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val caisseId: Long,
    val memberId: Long, // Bénéficiaire
    val montant: Long,
    val motif: MotifSortie,
    val description: String, // Détails (ex: "Aide pour décès parent")
    val dateSortie: Long = System.currentTimeMillis()
)