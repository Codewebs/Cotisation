package com.indiza.cotisation.network

import com.indiza.cotisation.caisses.CaisseSyncDto
import com.indiza.cotisation.tontine.TontineSyncDto
import com.indiza.cotisation.users.LoginDto
import com.indiza.cotisation.users.UserDto
import com.indiza.cotisation.users.UserEntity
import com.indiza.cotisation.users.UserResponse
import dagger.Provides
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface RemoteApi {

    // TONTINE
    suspend fun createTontine(tontine: TontineSyncDto): String
    suspend fun updateTontine(remoteId: String, tontine: TontineSyncDto)
    suspend fun deleteTontine(remoteId: String)

    // CAISSE
    suspend fun createCaisse(caisse: CaisseSyncDto): String
    suspend fun updateCaisse(remoteId: String, caisse: CaisseSyncDto)
    suspend fun deleteCaisse(remoteId: String)

    // USERS ✅
    suspend fun registerUser(userDto: UserDto): UserResponse
    suspend fun searchUserByPhone(phone: String): UserResponse?
    suspend fun login(loginDto: LoginDto): UserResponse
    suspend fun updateUser(remoteId: String, userDto: UserDto)
    suspend fun deleteUser(remoteId: String)

    suspend fun getCurrentUser(): UserResponse

}
