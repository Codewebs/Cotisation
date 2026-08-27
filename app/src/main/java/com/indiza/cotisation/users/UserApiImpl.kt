package com.indiza.cotisation.users

import com.indiza.cotisation.network.RemoteApi
import javax.inject.Inject

class UserApiImpl @Inject constructor(
    private val api: RemoteApi
) : UserApi {

    override suspend fun registerUser(userDto: UserDto): UserResponse {
        return api.registerUser(userDto)
    }

    override suspend fun login(loginDto: LoginDto): UserResponse {
        return api.login(loginDto)
    }


    override suspend fun searchUserByPhone(phone: String): UserResponse? {
        return api.searchUserByPhone(phone)
    }

    override suspend fun updateUser(remoteId: String, userDto: UserDto) {
        api.updateUser(remoteId, userDto)
    }

    override suspend fun deleteUser(remoteId: String) {
        api.deleteUser(remoteId)
    }

    override suspend fun getCurrentUser(): UserResponse {
        TODO("Not yet implemented")
    }
}
