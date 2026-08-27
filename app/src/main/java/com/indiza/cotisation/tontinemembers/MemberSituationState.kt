package com.indiza.cotisation.tontinemembers

import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.tontine.TontineMemberEntity

data class MemberSituationState(
    val member: TontineMemberEntity,
    val caisse: CaisseEntity,
    val participantInfo: CaisseParticipantEntity,
    // Cotisations
    val partsCotisees: Int = 0,
    val partsEncaissees: Int = 0,
    val totalCotise: Double = 0.0,
    val retardsCount: Int = 0,
    val echecsCount: Int = 0,
    // Prêts (Uniquement pour type PRET ou option possibilitePret)
    val montantEmprunte: Double = 0.0,
    val montantRembourse: Double = 0.0,
    val pretEnCours: Boolean = false
)