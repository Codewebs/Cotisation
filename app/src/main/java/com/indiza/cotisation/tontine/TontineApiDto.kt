package com.indiza.cotisation.tontine

import com.indiza.cotisation.sync.SyncStatus
import java.time.LocalDate

data class TontineApiDto(
    val id: String?,
    val libelle: String,
    val exercice: String? = null,
    val dateDebut: String,
    val dateFin: String,
    val updatedAt: Long
)

fun TontineApiDto.toEntity() = TontineEntity(
    remoteId = id,
    libelle = libelle,
    exercice = exercice,
    dateDebut = LocalDate.parse(dateDebut),
    dateFin = LocalDate.parse(dateFin),
    syncStatus = SyncStatus.SYNCED,
    updatedAt = updatedAt
)
