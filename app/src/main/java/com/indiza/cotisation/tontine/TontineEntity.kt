package com.indiza.cotisation.tontine

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.indiza.cotisation.sync.SyncStatus
import com.indiza.cotisation.sync.Syncable
import java.time.LocalDate

@Entity(
    tableName = "tontines",
    indices = [Index("remoteId", unique = true)]
)
data class TontineEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0L,

    val libelle: String,
    val exercice: String? = null,
    val dateDebut: LocalDate,
    val dateFin: LocalDate,
    val code: String? = null,
    val isAnonymous: Boolean = false,
    val isOnline: Boolean = true,
    val closed: Boolean = false,
    val creatorId: String? = null, // ID de l'initiateur
    override val remoteId: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    
    override val updatedAt: Long = System.currentTimeMillis(),

    // Résumé financier (mis à jour par la synchro si en ligne)
    val lastGlobalBalance: Long = 0,
    val lastTotalActiveLoans: Long = 0,
    val lastLendableAmount: Long = 0
) : Syncable<Long>

data class TontineSyncDto(
    val remoteId: String?,
    val libelle: String,
    val exercice: String?,
    val dateDebut: String,
    val dateFin: String,
    val creatorId: String? = null,
    val updatedAt: Long
)
fun TontineSyncDto.toApiDto() = TontineApiDto(
    id = remoteId,
    libelle = libelle,
    exercice = exercice,
    dateDebut = dateDebut,
    dateFin = dateFin,
    creatorId = creatorId,
    updatedAt = updatedAt
)
fun TontineEntity.toSyncDto() = TontineSyncDto(
    remoteId = remoteId,
    libelle = libelle,
    exercice = exercice,
    dateDebut = dateDebut.toString(),
    dateFin = dateFin.toString(),
    updatedAt = updatedAt
)
