package com.indiza.cotisation.caisses

data class CaisseSyncDto(
    val remoteId: String?,
    val tontineRemoteId: String?,
    val type: String,
    val libelle: String,
    val montantFixe: Long?,
    val montantMinimum: Long?,
    val frequence: String?,
    val regulariteImposee: Boolean,
    val redistribution: Boolean,
    val ordreRedistribution: Int?,
    val updatedAt: Long
)
fun CaisseSyncDto.toApiDto() = CaisseApiDto(
    id = remoteId,
    tontineId = tontineRemoteId!!,
    type = type,
    libelle = libelle,
    montantFixe = montantFixe,
    montantMinimum = montantMinimum,
    frequence = frequence,
    regulariteImposee = regulariteImposee,
    redistribution = redistribution,
    ordreRedistribution = ordreRedistribution,
    updatedAt = updatedAt
)
fun CaisseEntity.toSyncDto(tontineRemoteId: String?) = CaisseSyncDto(
    remoteId = remoteId,
    tontineRemoteId = tontineRemoteId,
    type = type.name,
    libelle = libelle,
    montantFixe = montantFixe,
    montantMinimum = montantMinimum,
    frequence = frequence.name,
    regulariteImposee = regulariteImposee,
    redistribution = redistribution,
    ordreRedistribution = ordreRedistribution,
    updatedAt = updatedAt
)