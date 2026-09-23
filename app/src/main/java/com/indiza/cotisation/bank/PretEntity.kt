package com.indiza.cotisation.bank

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.caisses.TypeInteret
import com.indiza.cotisation.tontine.TontineMemberEntity

@Entity(
    tableName = "prets",
    foreignKeys = [
        ForeignKey(entity = CaisseEntity::class, parentColumns = ["id"], childColumns = ["caisseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TontineMemberEntity::class, parentColumns = ["id"], childColumns = ["memberId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class PretEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val caisseId: Long,
    val memberId: Long? = null, // Peut être nul pour un emprunteur externe
    val montantPrincipal: Long,
    val montantInterets: Long,

    val tauxInteretApplique: Double,
    val typeInteret: TypeInteret,

    val datePret: Long = System.currentTimeMillis(),
    val dateEcheance: Long,
    val moisDiffere: Int = 0,
    val differeType: DiffereType = DiffereType.AUCUN,

    val statut: PretStatus = PretStatus.EN_COURS,
    val originCaisseId: Long? = null, // Caisse qui a fourni les fonds initialement

    // Pour les emprunteurs externes
    val nomEmprunteurExterne: String? = null,
    val cniEmprunteurExterne: String? = null,
    val guarantorMemberId: Long? = null // Membre qui avalise
)

enum class DiffereType { AUCUN, INTERETS_SEULS, TOTAL }

enum class PretStatus { EN_COURS, REMBOURSE, EN_RETARD }