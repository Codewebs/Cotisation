package com.indiza.cotisation.bank

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.indiza.cotisation.caisses.TypeInteret

@Entity(tableName = "prets")
data class PretEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val caisseId: Long,
    val memberId: Long,
    val montantPrincipal: Long,
    val montantInterets: Long,

    val tauxInteretApplique: Double,
    val typeInteret: TypeInteret,

    val datePret: Long = System.currentTimeMillis(),
    val dateEcheance: Long,
    val moisDiffere: Int = 0, //
    val differeType: DiffereType = DiffereType.AUCUN,

    val statut: PretStatus = PretStatus.EN_COURS
)

enum class DiffereType { AUCUN, INTERETS_SEULS, TOTAL }

enum class PretStatus { EN_COURS, REMBOURSE, EN_RETARD }