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

    @GET("tontines/{id}")
    suspend fun getTontineDetails(
        @Path("id") id: String,
        @Query("userId") userId: String?
    ): TontineApiDto

    @GET("tontines/search")
    suspend fun searchTontines(
        @Query("query") query: String,
        @Query("userId") userId: String?
    ): List<TontineSearchApiDto>

    @POST("tontines/{id}/join")
    suspend fun requestJoin(
        @Path("id") tontineId: String,
        @Body body: Map<String, String>
    ): com.indiza.cotisation.invitations.JoinRequestResponse

    @POST("caisses")
    suspend fun createCaisse(@Body caisse: com.indiza.cotisation.caisses.CaisseApiDto): com.indiza.cotisation.caisses.CaisseApiDto

    @GET("caisses/{id}/participants")
    suspend fun getCaisseParticipants(
        @Path("id") caisseId: String,
        @Query("userId") userId: String?
    ): List<com.indiza.cotisation.tontinemembers.CaisseParticipantApiDto>

    @POST("caisses/{id}/participants")
    suspend fun addCaisseParticipant(@Path("id") caisseId: String, @Body body: Map<String, String>): com.indiza.cotisation.tontinemembers.CaisseParticipantApiDto

    @POST("tontines/{id}/members")
    suspend fun addMember(@Path("id") tontineId: String, @Body body: Map<String, String>): TontineMemberApiDto

    @POST("transactions")
    suspend fun createTransaction(@Body transaction: TransactionApiDto): TransactionApiDto

    @PUT("caisses/{caisseId}/participants/{memberId}/order")
    suspend fun updateParticipantOrder(@Path("caisseId") caisseId: String, @Path("memberId") memberId: String, @Body body: Map<String, String>)

    @GET("users/{id}/tontines")
    suspend fun getMyTontines(@Path("id") userId: String): List<TontineApiDto>

    @GET("invites/{code}")
    suspend fun getInvitationDetails(@Path("code") code: String): com.indiza.cotisation.invitations.InvitationResponse

    @POST("invites/{code}/join")
    suspend fun joinTontine(@Path("code") code: String, @Body body: com.indiza.cotisation.invitations.JoinRequestDto)

    @POST("tontines/{id}/invites")
    suspend fun generateInvite(@Path("id") tontineId: String, @Body body: com.indiza.cotisation.invitations.InviteGenerationRequest): com.indiza.cotisation.invitations.InviteGenerationResponse

    @GET("tontines/{id}/requests")
    suspend fun getJoinRequests(@Path("id") tontineId: String): List<com.indiza.cotisation.invitations.JoinRequestResponse>

    @POST("requests/{id}/approve")
    suspend fun approveRequest(@Path("id") requestId: String, @Body body: com.indiza.cotisation.invitations.ApprovalRequest)

    // NOTIFS
    @GET("users/{id}/notifications")
    suspend fun getNotifications(@Path("id") userId: String): List<com.indiza.cotisation.users.NotificationResponse>

    @POST("notifications/{id}/read")
    suspend fun markAsRead(@Path("id") notificationId: String)

    @GET("transactions/{id}")
    suspend fun getTransaction(@Path("id") transactionId: String): TransactionApiDto

    @GET("users/{id}/signature-stats")
    suspend fun getAdminSignatureStats(@Path("id") userId: String): com.indiza.cotisation.users.AdminSignatureStats

    @POST("tontines/{id}/broadcast-report")
    suspend fun broadcastReport(@Path("id") tontineId: String, @Body body: Map<String, String>)

    // CHAT
    @GET("tontines/{id}/comments")
    suspend fun getComments(
        @Path("id") tontineId: String,
        @Query("userId") userId: String?
    ): List<CommentResponse>

    @POST("tontines/{id}/comments")
    suspend fun postComment(@Path("id") tontineId: String, @Body body: CommentRequest): CommentResponse
}
