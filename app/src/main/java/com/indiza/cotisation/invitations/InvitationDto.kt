package com.indiza.cotisation.invitations

import com.indiza.cotisation.tontine.TontineApiDto

data class InvitationResponse(
    val code: String,
    val tontineId: String,
    val senderId: String,
    val Tontine: TontineApiDto,
    val sender: InviterInfo
)

data class InviterInfo(
    val nom: String
)

data class JoinRequestDto(
    val userId: String
)

data class JoinRequestResponse(
    val id: String,
    val tontineId: String,
    val userId: String,
    val inviteCode: String?,
    val createdAt: Long,
    val requester: UserProfileInfo
)

data class UserProfileInfo(
    val id: String,
    val nom: String,
    val email: String,
    val phone: String,
    val matricule: String
)

data class InviteGenerationRequest(
    val senderId: String
)

data class InviteGenerationResponse(
    val code: String
)

data class ApprovalRequest(
    val adminUserId: String,
    val targetMemberId: Long,
    val updateInfo: Boolean
)
