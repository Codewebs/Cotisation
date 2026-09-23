package com.indiza.cotisation.caisses

import com.indiza.cotisation.sync.SyncStatus

data class CaisseApiDto(
    val id: String?,
    val tontineId: String,
    val type: String,
    val libelle: String,
    val montantFixe: Long?,
    val montantMinimum: Long?,
    val frequence: String? = null,
    val regulariteImposee: Boolean,
    val redistribution: Boolean,
    val possibilitePret: Boolean? = false,
    val autoriserPretSurAmendes: Boolean? = false,
    val tauxInteret: Double? = null,
    val typeInteret: String? = null,
    val penaliteRetardFixe: Long? = null,
    val penaliteEchecFixe: Long? = null,
    val dateLimiteJour: Int? = null,
    val fraisCaisse: Long? = null,
    val respecterFrequenceStrictement: Boolean? = false,
    val ordreRedistribution: Int?,
    val updatedAt: Long
)

fun CaisseApiDto.toEntity(localTontineId: Long) = CaisseEntity(
    tontineId = localTontineId,
    type = CaisseType.valueOf(type),
    libelle = libelle,
    montantFixe = montantFixe,
    montantMinimum = montantMinimum,
    frequence = frequence?.let { FrequenceCotisation.valueOf(it) } ?: FrequenceCotisation.MENSUEL,
    regulariteImposee = regulariteImposee,
    redistribution = redistribution,
    possibilitePret = possibilitePret ?: false,
    autoriserPretSurAmendes = autoriserPretSurAmendes ?: false,
    tauxInteret = tauxInteret,
    typeInteret = typeInteret?.let { TypeInteret.valueOf(it) },
    penaliteRetardFixe = penaliteRetardFixe,
    penaliteEchecFixe = penaliteEchecFixe,
    dateLimiteJour = dateLimiteJour,
    fraisCaisse = fraisCaisse,
    respecterFrequenceStrictement = respecterFrequenceStrictement ?: false,
    ordreRedistribution = ordreRedistribution,
    remoteId = id,
    syncStatus = SyncStatus.SYNCED,
    updatedAt = updatedAt
)
