package com.indiza.cotisation.tontinemembers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.bank.PretDao
import com.indiza.cotisation.bank.TransactionDao
import com.indiza.cotisation.bank.TransactionEntity
import com.indiza.cotisation.caisses.CaisseDao
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.tontine.TontineMemberDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberActivityState(
    val member: MemberUiModel? = null,
    val totalCotise: Long = 0,
    val totalDettes: Long = 0,
    val totalInteretsPayes: Long = 0,
    val activeCaisses: List<MemberCaisseActivity> = emptyList(),
    val loans: List<MemberLoanActivity> = emptyList(),
    val recentTransactions: List<MemberTransactionActivity> = emptyList(),
    val isLoading: Boolean = false
)

data class MemberCaisseActivity(
    val caisseName: String,
    val nombreParts: Int,
    val totalVerse: Long,
    val estBeneficiaire: Boolean
)

data class MemberLoanActivity(
    val id: Long,
    val date: Long,
    val montantTotal: Long,
    val montantRembourse: Long,
    val statut: String
)

data class MemberTransactionActivity(
    val id: Long,
    val date: Long,
    val type: String,
    val montant: Long,
    val caisseName: String
)

@HiltViewModel
class MemberActivityViewModel @Inject constructor(
    private val tontineMemberDao: TontineMemberDao,
    private val transactionDao: TransactionDao,
    private val pretDao: PretDao,
    private val caisseDao: CaisseDao
) : ViewModel() {

    private val _state = MutableStateFlow(MemberActivityState())
    val state = _state.asStateFlow()

    fun loadMemberActivity(tontineId: Long, memberId: Long) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }

        val memberWithUser = tontineMemberDao.getMemberWithUser(memberId)
        if (memberWithUser == null) return@launch

        val uiModel = MemberUiModel(
            id = memberWithUser.member.id.toString(),
            displayName = memberWithUser.user?.nom ?: memberWithUser.member.nomSaisi,
            matricule = memberWithUser.member.matricule ?: "",
            role = memberWithUser.member.role,
            isMe = false,
            isLinked = memberWithUser.member.userId != null,
            phoneNumber = memberWithUser.member.phone
        )

        // Charger toutes les caisses de la tontine
        val caisses = caisseDao.getCaissesByTontine(tontineId).first()
        
        val activeCaissesActivity = mutableListOf<MemberCaisseActivity>()
        var totalCotiseSum = 0L
        var totalDettesSum = 0L
        var totalInteretsSum = 0L

        for (caisse in caisses) {
            val participant = tontineMemberDao.getParticipantWithParts(memberId, caisse.id)
            if (participant != null) {
                val cotise = transactionDao.getTotalCotiseReel(caisse.id, memberId) ?: 0L
                val dette = transactionDao.getEtatDetteMembre(caisse.id, memberId).first() ?: 0L
                val interets = transactionDao.getSommePenalitesMembre(memberId, caisse.id).first() ?: 0L

                totalCotiseSum += cotise
                totalDettesSum += if (dette < 0) -dette else 0
                totalInteretsSum += interets

                activeCaissesActivity.add(MemberCaisseActivity(
                    caisseName = caisse.libelle,
                    nombreParts = participant.nombreParts,
                    totalVerse = cotise,
                    estBeneficiaire = false
                ))
            }
        }

        // Charger les prêts
        val prets = pretDao.getPretsByMember(memberId).first()
        val loanActivities = prets.map { p ->
            val rembourse = transactionDao.getTotalRemboursementPret(p.id) ?: 0L
            MemberLoanActivity(
                id = p.id,
                date = p.datePret,
                montantTotal = p.montantPrincipal + p.montantInterets,
                montantRembourse = rembourse,
                statut = p.statut.name
            )
        }

        // Charger l'historique récent
        val transactions = transactionDao.getTransactionsByCaisse(caisses.firstOrNull()?.id ?: 0L).first() // Simplification
        val recentTx = transactions.filter { it.memberId == memberId }.take(20).map { t ->
            MemberTransactionActivity(
                id = t.id,
                date = t.dateTransaction,
                type = t.type.name,
                montant = t.montant,
                caisseName = ""
            )
        }

        _state.update { it.copy(
            member = uiModel,
            totalCotise = totalCotiseSum,
            totalDettes = totalDettesSum,
            totalInteretsPayes = totalInteretsSum,
            activeCaisses = activeCaissesActivity,
            loans = loanActivities,
            recentTransactions = recentTx,
            isLoading = false
        ) }
    }
}
