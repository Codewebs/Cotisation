package com.indiza.cotisation.users

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.indiza.cotisation.sync.SyncStatus
import com.indiza.cotisation.sync.Syncable

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey override val id: String, // T = String
    val nom: String,
    val phone: String,
    val matricule: String,
    val email: String? = null,
    val pin: String? = null,
    val pinSalt: String? = null,
    val pinHash: String? = null,
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
    override val remoteId: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    override val updatedAt: Long = System.currentTimeMillis()
) : Syncable<String> // ← Spécifiez String comme type

data class UserResponse(
    val id: String?,
    val nom: String,
    val matricule: String,
    val phone: String,
    val email: String?,
    val pinHash: String?,
    val pinSalt: String?,
    val isActive: Boolean,
    val isVerified: Boolean
)

