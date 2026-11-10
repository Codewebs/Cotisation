package com.indiza.cotisation.caisses

import java.util.UUID

data class CaisseDraft(
    val id: String = UUID.randomUUID().toString(),
    val type: CaisseType,
    var libelle: String = "",
    var montantFixe: Long? = null,
    var montantMinimum: Long? = null,
    var frequence: FrequenceCotisation = FrequenceCotisation.MENSUEL,
    var regulariteImposee: Boolean = false,
    var redistribution: Boolean = false,
    var possibilitePret: Boolean = false,
    var autoriserPretSurAmendes: Boolean = false,
    var tauxInteret: Double? = null,
    var typeInteret: TypeInteret? = null,
    var penaliteRetardFixe: Long? = null,
    var penaliteEchecFixe: Long? = null,
    var fraisCaisse: Long? = null,
    val dureeCycles: Int? = null,
    var dateLimiteJour: Int? = null,
    var jourCotisation: String? = null,
    var heureLimite: Int? = null,
    var respecterFrequenceStrictement: Boolean = false
)

data class CaisseWithActiveCycle(
    val caisse: CaisseEntity,
    val activeCycleId: Long
)
data class MemberPartsCount(
    val memberId: Long,
    val count: Int
)

fun FrequenceCotisation.getConversionLabel(duree: Int): String {
    if (duree <= 0) return ""
    return when (this) {
        FrequenceCotisation.JOURNALIER -> "${duree / 7} semaine(s) et ${duree % 7} jour(s)"
        FrequenceCotisation.HEBDOMADAIRE -> "${duree / 4} mois et ${duree % 4} semaine(s)"
        FrequenceCotisation.MENSUEL -> "${duree / 12} an(s) et ${duree % 12} mois"
        else -> ""
    }
}