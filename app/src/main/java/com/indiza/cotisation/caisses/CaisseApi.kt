package com.indiza.cotisation.caisses

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CaisseApi {

    @POST("caisses")
    suspend fun createCaisse(
        @Body caisse: CaisseApiDto
    ): CaisseApiDto

    @PUT("caisses/{id}")
    suspend fun updateCaisse(
        @Path("id") id: String,
        @Body caisse: CaisseApiDto
    )
    @POST("caisses/bulk")
    suspend fun bulkCreateCaisses(@Body caisses: List<CaisseApiDto>): List<CaisseApiDto>

    @DELETE("caisses/{id}")
    suspend fun deleteCaisse(
        @Path("id") id: String
    )
}
