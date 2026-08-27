package com.indiza.cotisation.tontine

data class TontineApiDto(
    val id: String?,
    val libelle: String,
    val dateDebut: String,
    val dateFin: String,
    val updatedAt: Long
)
