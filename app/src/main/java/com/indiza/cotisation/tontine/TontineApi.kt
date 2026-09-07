package com.indiza.cotisation.tontine

import retrofit2.http.*

interface TontineApi {

    @POST("tontines")
    suspend fun createTontine(
        @Body tontine: TontineApiDto
    ): TontineApiDto

    @PUT("tontines/{id}")
    suspend fun updateTontine(
        @Path("id") id: String,
        @Body tontine: TontineApiDto
    )

    @DELETE("tontines/{id}")
    suspend fun deleteTontine(
        @Path("id") id: String
    )

    @GET("tontines/search")
    suspend fun searchTontines(@Query("query") query: String): List<TontineApiDto>
}
