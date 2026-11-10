package com.indiza.cotisation.bank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.caisses.CaisseDao
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.caisses.TypeInteret
import com.indiza.cotisation.tontine.TontineMemberDao
import com.indiza.cotisation.tontine.TontineRepository
import com.indiza.cotisation.tontinemembers.ParticipantWithParts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoanManagementViewModel @Inject constructor(
    private val tontineRepository: TontineRepository,
    private val caisseDao: CaisseDao,
    private val tontineMemberDao: TontineMemberDao,
    private val transactionDao: TransactionDao,
    private val cycleDao: CycleDao,
    private val pretDao: PretDao
) : ViewModel() {

    private val _lendableCaisses = MutableStateFlow<List<CaisseEntity>>(emptyList())
    val lendableCaisses = _lendableCaisses.asStateFlow()

    private val _basicLoanFund = MutableStateFlow<CaisseEntity?>(null)
    val basicLoanFund = _basicLoanFund.asStateFlow()

    fun loadData(tontineId: Long) = viewModelScope.launch {
        caisseDao.getCaissesByTontine(tontineId).collect { caisses ->
            _lendableCaisses.value = caisses.filter { it.possibilitePret && !it.isBasique }
            _basicLoanFund.value = caisses.find { it.isBasique }
        }
    }

    fun setupBasicFund(tontineId: Long, amount: Long, sourceCaisseId: Long) = viewModelScope.launch {
        tontineRepository.setupBasicLoanFund(tontineId, amount, sourceCaisseId)
    }

    fun grantLoan(
        caisseId: Long,
        memberId: Long?,
        montant: Long,
        cycleId: Long,
        taux: Double,
        typeInteret: TypeInteret,
        nomExterne: String? = null,
        cniExterne: String? = null,
        guarantorId: Long? = null,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        val interets = (montant * taux / 100).toLong() 
        val pret = PretEntity(
            caisseId = caisseId,
            memberId = memberId,
            montantPrincipal = montant,
            montantInterets = interets,
            tauxInteretApplique = taux,
            typeInteret = typeInteret,
            dateEcheance = System.currentTimeMillis() + (30L * 24 * 3600 * 1000),
            nomEmprunteurExterne = nomExterne,
            cniEmprunteurExterne = cniExterne,
            guarantorMemberId = guarantorId
        )
        
        pretDao.insertPret(pret)

        // Enregistrer la transaction financière
        tontineRepository.recordTransaction(
            TransactionEntity(
                caisseId = caisseId,
                memberId = memberId ?: guarantorId ?: 0L,
                cycleId = cycleId,
                montant = -montant,
                type = TransactionType.PRET,
                motif = if (nomExterne != null) "Prêt externe ($nomExterne) avalisé par ID $guarantorId" else "Prêt membre"
            )
        )
        
        onSuccess()
    }
}
