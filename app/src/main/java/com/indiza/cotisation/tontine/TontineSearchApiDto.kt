package com.indiza.cotisation.tontine

data class TontineSearchApiDto(
    val id: String,
    val libelle: String,
    val exercice: String?,
    val dateDebut: String,
    val dateFin: String,
    val membershipStatus: String,
    val updatedAt: Long
)
