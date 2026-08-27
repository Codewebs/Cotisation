package com.indiza.cotisation.tontine

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.indiza.cotisation.sync.SyncStatus
import com.indiza.cotisation.sync.Syncable
import java.time.LocalDate

@Entity(tableName = "tontines")
data class TontineEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0L,

    val libelle: String,
    val exercice: String? = null,
    val dateDebut: LocalDate,
    val dateFin: LocalDate,
    val code: String? = null,
    val isAnonymous: Boolean = false,
    val closed: Boolean = false,
    override val remoteId: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    
    override val updatedAt: Long = System.currentTimeMillis()
) : Syncable<Long>

data class TontineSyncDto(
    val remoteId: String?,
    val libelle: String,
    val dateDebut: String,
    val dateFin: String,
    val updatedAt: Long
)
fun TontineSyncDto.toApiDto() = TontineApiDto(
    id = remoteId,
    libelle = libelle,
    dateDebut = dateDebut,
    dateFin = dateFin,
    updatedAt = updatedAt
)
fun TontineEntity.toSyncDto() = TontineSyncDto(
    remoteId = remoteId,
    libelle = libelle,
    dateDebut = dateDebut.toString(),
    dateFin = dateFin.toString(),
    updatedAt = updatedAt
)
