package com.indiza.cotisation.tontinemembers

import com.indiza.cotisation.users.Role

data class MemberUiModel(
    val id: String,
    val displayName: String,
    val matricule: String,
    val role: Role,
    val isMe: Boolean,
    val isLinked: Boolean
)