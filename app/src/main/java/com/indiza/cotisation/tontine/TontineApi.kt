package com.indiza.cotisation.tontine

import com.indiza.cotisation.caisses.CaisseApiDto
import com.indiza.cotisation.invitations.InviteGenerationResponse
import com.indiza.cotisation.tontinemembers.CaisseParticipantApiDto

interface TontineApi {
    suspend fun createTontine(tontine: TontineApiDto): TontineApiDto
    suspend fun createCaisse(caisse: CaisseApiDto): CaisseApiDto
    suspend fun getCaisseParticipants(caisseId: String, userId: String?): List<CaisseParticipantApiDto>
    suspend fun addCaisseParticipant(caisseId: String, memberId: String, nombreParts: Int): CaisseParticipantApiDto
    suspend fun removeCaisseParticipant(caisseId: String, memberId: String)
    suspend fun addMember(tontineId: String, nom: String, phone: String, role: String): TontineMemberApiDto
    suspend fun createTransaction(transaction: TransactionApiDto, cycleNumber: Int): TransactionApiDto
    suspend fun getTransactionsByCaisse(caisseId: String): List<TransactionApiDto>
    suspend fun updateParticipantOrder(caisseId: String, memberId: String, order: Int)
    suspend fun getMyTontines(userId: String): List<TontineApiDto>
    suspend fun getTontineDetails(id: String, userId: String?): TontineApiDto
    suspend fun generateInvite(tontineId: String, senderId: String): InviteGenerationResponse
    suspend fun broadcastReport(tontineId: String, senderId: String)
}
