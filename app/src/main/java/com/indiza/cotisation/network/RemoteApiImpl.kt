package com.indiza.cotisation.network

import com.indiza.cotisation.caisses.CaisseApi
import com.indiza.cotisation.caisses.CaisseSyncDto
import com.indiza.cotisation.caisses.toApiDto
import com.indiza.cotisation.sync.SyncStatus
import com.indiza.cotisation.tontine.TontineApi
import com.indiza.cotisation.tontine.TontineSyncDto
import com.indiza.cotisation.tontine.toApiDto
import com.indiza.cotisation.users.LoginDto
import com.indiza.cotisation.users.NetworkUserApi
import com.indiza.cotisation.users.UserApi
import com.indiza.cotisation.users.UserDto
import com.indiza.cotisation.users.UserEntity
import com.indiza.cotisation.users.UserResponse
import com.indiza.cotisation.users.toUserEntity
import java.util.UUID

class RemoteApiImpl(
    private val tontineApi: TontineApi,
    private val caisseApi: CaisseApi,
    private val networkUserApi: NetworkUserApi
) : RemoteApi {

    override suspend fun createTontine(tontine: TontineSyncDto): String {
        val response = tontineApi.createTontine(tontine.toApiDto())
        return response.id!!
    }

    override suspend fun updateTontine(remoteId: String, tontine: TontineSyncDto) {
        tontineApi.updateTontine(remoteId, tontine.toApiDto())
    }

    override suspend fun deleteTontine(remoteId: String) {
        tontineApi.deleteTontine(remoteId)
    }

    override suspend fun createCaisse(caisse: CaisseSyncDto): String {
        val response = caisseApi.createCaisse(caisse.toApiDto())
        return response.id!!
    }
    override suspend fun searchUserByPhone(phone: String): UserResponse? {
        return networkUserApi.searchUserByPhone(phone)
    }
    override suspend fun updateCaisse(remoteId: String, caisse: CaisseSyncDto) {
        caisseApi.updateCaisse(remoteId, caisse.toApiDto())
    }

    override suspend fun deleteCaisse(remoteId: String) {
        caisseApi.deleteCaisse(remoteId)
    }

    override suspend fun registerUser(userDto: UserDto): UserResponse {
        return networkUserApi.registerUser(userDto)
    }


    override suspend fun updateUser(remoteId: String, userDto: UserDto) {
        try {
            networkUserApi.updateUser(remoteId, userDto)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun deleteUser(remoteId: String) {
        try {
            networkUserApi.deleteUser(remoteId)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun login(loginDto: LoginDto): UserResponse {
        return networkUserApi.login(loginDto)
    }
    override suspend fun getCurrentUser(): UserResponse {
        return networkUserApi.getCurrentUser()
    }
}
