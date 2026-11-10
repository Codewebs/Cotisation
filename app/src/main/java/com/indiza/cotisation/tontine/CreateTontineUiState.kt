package com.indiza.cotisation.tontine

import com.indiza.cotisation.caisses.CaisseDraft
import java.time.LocalDate

data class CreateTontineUiState(
    val libelle: String = "",
    val exercice: String = "",
    val dateDebut: LocalDate = LocalDate.now(),
    val dateFin: LocalDate = LocalDate.now().plusYears(1),
    val caisses: List<CaisseDraft> = emptyList(),
    val isSaving: Boolean = false,
    val isAnonymous: Boolean = false,
    val isOnline: Boolean = true,
    val hideInfoForSimpleMembers: Boolean = false,
    val error: String? = null
)
