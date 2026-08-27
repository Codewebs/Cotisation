package com.indiza.cotisation.users

import com.indiza.cotisation.sync.SyncAction
import com.indiza.cotisation.sync.SyncQueueDao
import com.indiza.cotisation.sync.SyncQueueEntity
import com.indiza.cotisation.sync.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val userApi: UserApi,
    private val syncQueueDao: SyncQueueDao
) {

    suspend fun saveUser(user: UserEntity) {
        println("*****0000 saveUser********")
        // Enregistrer localement
        userDao.upsertUser(user)
    println(" 2222 000000000000 VXVXCXVXCXVXC")
        // Mettre en queue pour synchronisation
       /* if (user.syncStatus == SyncStatus.PENDING_CREATE ||
            user.syncStatus == SyncStatus.PENDING_UPDATE) {
            println("*****0001 register saveuser********")

            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = "USER",
                    entityLocalId = user.id.toLong(),
                    action = if (user.syncStatus == SyncStatus.PENDING_CREATE)
                        SyncAction.CREATE else SyncAction.UPDATE
                )
            )
        }*/

    }

    suspend fun getUserByPhone(phone: String): UserEntity? {
        val localUser = userDao.getUserByPhone(phone)

        if (localUser != null && localUser.isVerified) {
            return localUser
        }

        return try {
            val remoteResponse: UserResponse? = userApi.searchUserByPhone(phone)

            remoteResponse?.let {
                val userEntity = it.toUserEntity()
                userDao.upsertUser(userEntity)
                userEntity
            } ?: localUser

        } catch (e: Exception) {
            localUser
        }
    }

    suspend fun getUserById(id: String): UserEntity? {
        return userDao.getUserById(id)
    }
}