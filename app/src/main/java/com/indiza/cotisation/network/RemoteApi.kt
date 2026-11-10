package com.indiza.cotisation.network

import com.indiza.cotisation.caisses.CaisseSyncDto
import com.indiza.cotisation.tontine.TontineApiDto
import com.indiza.cotisation.tontine.TontineSyncDto
import com.indiza.cotisation.tontine.TransactionApiDto
import com.indiza.cotisation.tontine.TontineMemberApiDto
import com.indiza.cotisation.invitations.JoinRequestResponse
import com.indiza.cotisation.users.AdminSignatureStats
import com.indiza.cotisation.users.NotificationResponse

interface RemoteApi {
    suspend fun createTontine(tontine: TontineSyncDto): String
    suspend fun updateTontine(remoteId: String, tontine: TontineSyncDto)
    suspend fun deleteTontine(remoteId: String)
    suspend fun getMyTontines(userId: String): List<TontineApiDto>
    suspend fun createCaisse(caisse: CaisseSyncDto): String
    suspend fun updateCaisse(remoteId: String, caisse: CaisseSyncDto)
    suspend fun deleteCaisse(remoteId: String)
    suspend fun addMember(tontineId: String, nom: String, phone: String, role: String): TontineMemberApiDto
    suspend fun generateInviteLink(tontineId: String, senderId: String): String
    suspend fun getJoinRequests(tontineId: String): List<JoinRequestResponse>
    suspend fun approveJoinRequest(requestId: String, targetMemberRemoteId: String?)
    suspend fun rejectJoinRequest(requestId: String)
    suspend fun getUserNotifications(userId: String): List<NotificationResponse>
    suspend fun markNotificationAsRead(notificationId: String)
    suspend fun getTransactionDetails(transactionId: String): TransactionApiDto
    suspend fun getTransactionsByCaisse(caisseRemoteId: String): List<TransactionApiDto>
    suspend fun createTransaction(transaction: TransactionApiDto, cycleNumber: Int): TransactionApiDto
    suspend fun getAdminSignatureStats(userId: String): AdminSignatureStats
    suspend fun broadcastReport(tontineId: String, senderId: String)
}
