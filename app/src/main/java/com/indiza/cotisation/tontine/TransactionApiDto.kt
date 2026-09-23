package com.indiza.cotisation.tontine

import com.indiza.cotisation.caisses.CaisseApiDto

data class TransactionApiDto(
    val id: String,
    val caisseId: String,
    val memberId: String,
    val cycleId: String,
    val montant: Long,
    val type: String,
    val motif: String?,
    val dateTransaction: Long,
    val validatedById: String? = null,
    val validator: com.indiza.cotisation.invitations.UserProfileInfo? = null,
    val Caisse: CaisseApiDto? = null
)
