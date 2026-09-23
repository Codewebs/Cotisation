package com.indiza.cotisation.tontinemembers

import com.indiza.cotisation.tontine.TontineMemberEntity
import com.indiza.cotisation.users.Role

data class CaisseParticipantApiDto(
    val caisseId: String,
    val memberId: String,
    val ordrePassage: Int? = null,
    val nombreParts: Int = 1,
    val member: MemberShortDto? = null
)

data class MemberShortDto(
    val id: String,
    val userId: String?,
    val nomSaisi: String,
    val phone: String,
    val role: Role
)
