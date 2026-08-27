package com.indiza.cotisation.caisses

data class CaisseApiDto(
    val id: String?,
    val tontineId: String,
    val type: String,
    val libelle: String,
    val montantFixe: Long?,
    val montantMinimum: Long?,
    val regulariteImposee: Boolean,
    val redistribution: Boolean,
    val ordreRedistribution: Int?,
    val updatedAt: Long
)
