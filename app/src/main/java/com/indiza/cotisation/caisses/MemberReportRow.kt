package com.indiza.cotisation.caisses

data class MemberReportRow(
    val memberName: String,
    val totalACotiser: Long,
    val dejaCotise: Long,
    val manquant: Long,
    val cotiseCeCycle: Long,
    val dateCotisation: String? = null,
    val isBeneficiaire: Boolean = false,
    val soldeCaisseMoment: Long = 0L,
    val status: String
)
