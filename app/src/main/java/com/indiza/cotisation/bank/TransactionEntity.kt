package com.indiza.cotisation.bank

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.tontine.TontineMemberEntity

enum class TransactionType {
    COTISATION,      // Versement régulier
    PRET,            // Sortie d'argent (Caisse Prêt)
    REMBOURSEMENT,   // Entrée d'argent (Caisse Prêt)
    SORTIE_SECOURS,  // Sortie (Caisse Secours/Aide)
    PENALITE_RETARD,        // Amende pour retard
    PENALITE_ECHEC,        // Amende pour retard
    INTERET,          // Intérêts générés par un prêt
    TOUR_ENCAISSER,        // Quand un membre bouffe la cotisation à son tour
}
@Entity(
    tableName = "caisse_cycles",
    foreignKeys = [ForeignKey(entity = CaisseEntity::class, parentColumns = ["id"], childColumns = ["caisseId"])]
)
data class CycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val caisseId: Long,
    val numeroCycle: Int = 1,
    val dateDebut: Long = System.currentTimeMillis(),
    val dateFin: Long? = null,
    val estCloture: Boolean = false
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = CaisseEntity::class, parentColumns = ["id"], childColumns = ["caisseId"]),
        ForeignKey(entity = TontineMemberEntity::class, parentColumns = ["id"], childColumns = ["memberId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val caisseId: Long,
    val memberId: Long,
    val cycleId: Long,
    val montant: Long, // Positif pour entrées, négatif pour sorties
    val type: TransactionType,
    val dateTransaction: Long = System.currentTimeMillis(),
    val motif: String? = null,
    val a_cotise: Boolean = true,
    val referencePretId: Long? = null // Lie un remboursement à un prêt spécifique
)