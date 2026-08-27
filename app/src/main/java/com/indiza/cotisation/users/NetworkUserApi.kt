package com.indiza.cotisation.users

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NetworkUserApi {

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