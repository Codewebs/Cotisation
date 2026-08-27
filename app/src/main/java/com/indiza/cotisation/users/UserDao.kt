package com.indiza.cotisation.users

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.indiza.cotisation.sync.SyncStatus

@Dao
interface UserDao {

    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE phone = :phone")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE remoteId = :remoteId")
    suspend fun getUserByRemoteId(remoteId: String): UserEntity?

    @Query("UPDATE users SET remoteId = :remoteId, syncStatus = :syncStatus, isVerified = :isVerified WHERE id = :id")
    suspend fun updateRemoteInfo(
        id: String,
        remoteId: String?,
        syncStatus: SyncStatus,
        isVerified: Boolean = true
    )

    @Query("UPDATE users SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: SyncStatus)
}