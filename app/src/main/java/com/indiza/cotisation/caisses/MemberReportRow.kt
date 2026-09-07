package com.indiza.cotisation.caisses

data class MemberReportRow(
    val memberName: String,
    val totalACotiser: Long,
    val dejaCotise: Long,
    val manquant: Long,
    val cotiseCeCycle: Long,
    val status: String
)
