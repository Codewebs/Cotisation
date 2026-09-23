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

    override suspend fun getMyTontines(userId: String): List<com.indiza.cotisation.tontine.TontineApiDto> {
        return tontineApi.getMyTontines(userId)
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

    override suspend fun getInvitationDetails(code: String): com.indiza.cotisation.invitations.InvitationResponse {
        return tontineApi.getInvitationDetails(code)
    }

    override suspend fun joinTontine(code: String, userId: String) {
        tontineApi.joinTontine(code, com.indiza.cotisation.invitations.JoinRequestDto(userId))
    }

    override suspend fun generateInviteLink(tontineId: String, senderId: String): String {
        return tontineApi.generateInvite(tontineId, com.indiza.cotisation.invitations.InviteGenerationRequest(senderId)).code
    }

    override suspend fun getJoinRequests(tontineId: String): List<com.indiza.cotisation.invitations.JoinRequestResponse> {
        return tontineApi.getJoinRequests(tontineId)
    }

    override suspend fun approveJoinRequest(requestId: String, adminUserId: String, targetMemberId: Long, updateInfo: Boolean) {
        tontineApi.approveRequest(requestId, com.indiza.cotisation.invitations.ApprovalRequest(adminUserId, targetMemberId, updateInfo))
    }

    override suspend fun getUserNotifications(userId: String): List<com.indiza.cotisation.users.NotificationResponse> {
        return tontineApi.getNotifications(userId)
    }

    override suspend fun markNotificationAsRead(notificationId: String) {
        tontineApi.markAsRead(notificationId)
    }

    override suspend fun getTransactionDetails(transactionId: String): com.indiza.cotisation.tontine.TransactionApiDto {
        return tontineApi.getTransaction(transactionId)
    }

    override suspend fun getAdminSignatureStats(userId: String): com.indiza.cotisation.users.AdminSignatureStats {
        return tontineApi.getAdminSignatureStats(userId)
    }

    override suspend fun broadcastReport(tontineId: String, senderId: String) {
        tontineApi.broadcastReport(tontineId, mapOf("senderId" to senderId))
    }
}
