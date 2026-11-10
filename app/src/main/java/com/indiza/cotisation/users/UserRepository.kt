package com.indiza.cotisation.users

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {

    suspend fun saveUser(user: UserEntity) {
        userDao.upsertUser(user)
    }

    suspend fun getUserByPhone(phone: String): UserEntity? = userDao.getUserByPhone(phone)

    suspend fun getUserById(id: String): UserEntity? {
        return userDao.getUserById(id)
    }
}