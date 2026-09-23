package com.indiza.cotisation.bank

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.tontine.TontineMemberEntity

enum class MotifSortie {
    DEPART_TONTINE,
    DECES_PARENT,
    AIDE_SOCIALE,
    DECES_MEMBRE,
    DEPENSE_DIVERS,
    AUTRE
}

@Entity(
    tableName = "sorties_caisse",
    foreignKeys = [
        ForeignKey(entity = CaisseEntity::class, parentColumns = ["id"], childColumns = ["caisseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TontineMemberEntity::class, parentColumns = ["id"], childColumns = ["memberId"], onDelete = ForeignKey.CASCADE)
    ]
)
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