package com.indiza.cotisation.tontine

data class TontineSearchResult(
    val tontine: TontineEntity,
    val membershipStatus: String, // NONE, PENDING, MEMBER
    val memberCount: Long = 0
)
