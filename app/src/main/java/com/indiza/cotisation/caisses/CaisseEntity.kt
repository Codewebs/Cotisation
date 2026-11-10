package com.indiza.cotisation.caisses

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.indiza.cotisation.sync.SyncStatus
import com.indiza.cotisation.sync.Syncable
import com.indiza.cotisation.tontine.TontineEntity

@Entity(
    tableName = "caisses",
    foreignKeys = [
        ForeignKey(
            entity = TontineEntity::class,
            parentColumns = ["id"],
            childColumns = ["tontineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("tontineId"),
        Index("remoteId", unique = true)
    ]
)
data class CaisseEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0L,

    val tontineId: Long,

    val type: CaisseType,
    val libelle: String,

    val montantFixe: Long? = null,
    val montantMinimum: Long? = null,
    val frequence: FrequenceCotisation,

    val regulariteImposee: Boolean = false,
    val redistribution: Boolean = false,
    val ordreRedistribution: Int? = null,

    // Paramètres spécifiques au Prêt
    val tauxInteret: Double? = null,
    val typeInteret: TypeInteret? = null, // SIMPLE, COMPOSE

    // Paramètres de ponctualité
    val penaliteRetardFixe: Long? = null,
    val penaliteEchecFixe: Long? = null,
    val dateLimiteJour: Int? = null,
    val jourCotisation: String? = null,
    val heureLimite: Int? = null,
    val fraisCaisse: Long? = null,
    val ordreFige: Boolean = false,

    val possibilitePret: Boolean = false,
    val autoriserPretSurAmendes: Boolean = false,
    val dureeCycles: Int? = null,
    val plafondPret: Long? = null, // Pour la caisse de prêt basique
    val isBasique: Boolean = false, // True pour la caisse de prêt automatique
    val statut: CaisseStatus = CaisseStatus.ACTIVE,
    val respecterFrequenceStrictement: Boolean = false,

    override val remoteId: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    override val updatedAt: Long = System.currentTimeMillis()
) : Syncable<Long>

enum class TypeInteret { SIMPLE, COMPOSE }

enum class CaisseStatus { ACTIVE, CLOTUREE }

