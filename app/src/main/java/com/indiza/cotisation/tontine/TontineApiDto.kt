package com.indiza.cotisation.tontine

import com.indiza.cotisation.sync.SyncStatus
import java.time.LocalDate

data class TontineApiDto(
    val id: String?,
    val libelle: String,
    val exercice: String? = null,
    val dateDebut: String,
    val dateFin: String,
    val creatorId: String? = null,
    val updatedAt: Long,
    val caisses: List<com.indiza.cotisation.caisses.CaisseApiDto>? = null,
    val members: List<TontineMemberApiDto>? = null,
    val summary: TontineSummaryDto? = null
)

data class TontineMemberApiDto(
    val id: String,
    val userId: String?,
    val nomSaisi: String,
    val phone: String,
    val role: com.indiza.cotisation.users.Role,
    val isInitiator: Boolean = false
)

data class TontineSummaryDto(
    val globalBalance: Long,
    val turnoverIn: Long,
    val turnoverOut: Long,
    val totalActiveLoans: Long,
    val forecastGains: Long,
    val lendableAmount: Long,
    val nonLendableAmount: Long
)

fun TontineApiDto.toEntity() = TontineEntity(
    remoteId = id,
    libelle = libelle,
    exercice = exercice,
    dateDebut = LocalDate.parse(dateDebut),
    dateFin = LocalDate.parse(dateFin),
    creatorId = creatorId,
    syncStatus = SyncStatus.SYNCED,
    updatedAt = updatedAt,
    lastGlobalBalance = summary?.globalBalance ?: 0L,
    lastTotalActiveLoans = summary?.totalActiveLoans ?: 0L,
    lastLendableAmount = summary?.lendableAmount ?: 0L
)
