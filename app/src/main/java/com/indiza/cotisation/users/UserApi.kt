package com.indiza.cotisation.users

import com.indiza.cotisation.sync.SyncStatus
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID

interface UserApi {

    @POST("users/register")
    suspend fun registerUser(@Body userDto: UserDto): UserResponse

    @POST("users/login")
    suspend fun login(@Body loginDto: LoginDto): UserResponse

    @GET("users/search")
    suspend fun searchUserByPhone(@Query("phone") phone: String): UserResponse?

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body userDto: UserDto)

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: String)


    @GET("users/me")
    suspend fun getCurrentUser(): UserResponse
}

data class UserDto(
    val nom: String,
    val phone: String,
    val pin: String
)

data class LoginDto(
    val phone: String,
    val pin: String
)
// Extension pour convertir UserResponse en UserEntity
fun UserResponse.toUserEntity(): UserEntity {
    return UserEntity(
        id = this.id ?: "local_${UUID.randomUUID()}",
        nom = this.nom,
        phone = this.phone,
        matricule = this.matricule,
        email = this.email,
        pin = this.pinHash,
        pinSalt = this.pinSalt,
        isActive = this.isActive,
        isVerified = this.isVerified,
        remoteId = this.id,
        syncStatus = SyncStatus.SYNCED,
        updatedAt = System.currentTimeMillis()
    )
}