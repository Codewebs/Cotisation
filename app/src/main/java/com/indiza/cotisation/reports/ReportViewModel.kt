package com.indiza.cotisation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.bank.CycleDao
import com.indiza.cotisation.bank.TransactionDao
import com.indiza.cotisation.bank.TransactionType
import com.indiza.cotisation.caisses.CaisseDao
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.bank.TransactionEntity
import com.indiza.cotisation.tontine.TontineDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

data class ReportHeaderState(
    val tontineName: String = "",
    val caisseName: String = "",
    val cycleNumber: Int = 0,
    val exercice: String = "",
    val dateDebut: Long? = null,
    val dateFin: Long? = null,
    val allCycles: List<com.indiza.cotisation.bank.CycleEntity> = emptyList(),
    val currentCycleId: Long = 0L
)

data class FinanceReportState(
    val soldeGlobal: Long = 0,
    val soldeCycle: Long = 0,
    val soldeJusqueIci: Long = 0,
    val ratioSolvabilite: Float = 0f,
    val performanceCycle: Float = 0f,
    val totalCotisationsCycle: Long = 0,
    val totalCotisationsJusqueIci: Long = 0,
    val totalSortiesCycle: Long = 0,
    val totalSortiesJusqueIci: Long = 0
)

data class AttendanceReportState(
    val retardsCountCycle: Int = 0,
    val retardsCountJusqueIci: Int = 0,
    val topCotisants: List<Pair<String, Long>> = emptyList(),
    val totalAmendesCycle: Long = 0,
    val totalAmendesJusqueIci: Long = 0
)

data class LoanReportState(
    val totalDettesJusqueIci: Long = 0,
    val rendementInteretsCycle: Long = 0,
    val rendementInteretsJusqueIci: Long = 0,
    val previsionnelTresorerie: List<Pair<String, Long>> = emptyList()
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val caisseDao: CaisseDao,
    private val tontineDao: TontineDao,
    private val cycleDao: CycleDao
) : ViewModel() {

    private val _headerState = MutableStateFlow(ReportHeaderState())
    val headerState = _headerState.asStateFlow()

    private val _financeState = MutableStateFlow(FinanceReportState())
    val financeState = _financeState.asStateFlow()

    private val _attendanceState = MutableStateFlow(AttendanceReportState())
    val attendanceState = _attendanceState.asStateFlow()

    private val _loanState = MutableStateFlow(LoanReportState())
    val loanState = _loanState.asStateFlow()

    fun loadFinanceReport(caisseId: Long, cycleId: Long) {
        loadHeaderInfo(caisseId, cycleId)
        viewModelScope.launch {
            val caisse = caisseDao.getCaisseById(caisseId)
            val totalParts = transactionDao.getTotalPartsCaisse(caisseId) ?: 1
            val objectifTheorique = (caisse?.montantFixe ?: 0L) * totalParts

            combine(
                transactionDao.getSoldeGlobal(caisseId),
                cycleDao.getAllCycles(caisseId),
                transactionDao.getTransactionsByCaisse(caisseId),
                transactionDao.getTotalPretsEnCours(caisseId)
            ) { soldeActuel, allCycles, transactions, pretsActuels ->
                val targetCycle = allCycles.find { it.id == cycleId } ?: return@combine FinanceReportState()
                val targetNumero = targetCycle.numeroCycle
                
                // IDs des cycles jusqu'au cycle choisi
                val cycleIdsJusqueIci = allCycles.filter { it.numeroCycle <= targetNumero }.map { it.id }.toSet()

                // Filtrage des transactions
                val txCycle = transactions.filter { it.cycleId == cycleId }
                val txJusqueIci = transactions.filter { it.cycleId in cycleIdsJusqueIci }

                // Calculs Cycle
                val cotisCycle = txCycle.filter { it.type == TransactionType.COTISATION }.sumOf { it.montant }
                val sortiesCycle = txCycle.filter { 
                    it.type in listOf(TransactionType.SORTIE_SECOURS, TransactionType.TOUR_ENCAISSER, TransactionType.PRET, TransactionType.SORTIE)
                }.sumOf { abs(it.montant) }
                val soldeCycle = txCycle.sumOf { it.montant }

                // Calculs Cumulés
                val cotisJusqueIci = txJusqueIci.filter { it.type == TransactionType.COTISATION }.sumOf { it.montant }
                val sortiesJusqueIci = txJusqueIci.filter { 
                    it.type in listOf(TransactionType.SORTIE_SECOURS, TransactionType.TOUR_ENCAISSER, TransactionType.PRET, TransactionType.SORTIE)
                }.sumOf { abs(it.montant) }
                val soldeJusqueIci = txJusqueIci.sumOf { it.montant }

                // Ratio & Performance (Basé sur le cumul pour le ratio, le cycle pour la perf)
                val detteAbs = abs(pretsActuels ?: 0L) // Note: Les dettes sont souvent globales ou par cycle, ici on prend le global actuel pour la sécurité

                FinanceReportState(
                    soldeGlobal = soldeActuel ?: 0,
                    soldeCycle = soldeCycle,
                    soldeJusqueIci = soldeJusqueIci,
                    ratioSolvabilite = if (detteAbs > 0) (soldeActuel ?: 0).toFloat() / detteAbs else 1f,
                    performanceCycle = if (objectifTheorique > 0) cotisCycle.toFloat() / objectifTheorique else 1f,
                    totalCotisationsCycle = cotisCycle,
                    totalCotisationsJusqueIci = cotisJusqueIci,
                    totalSortiesCycle = sortiesCycle,
                    totalSortiesJusqueIci = sortiesJusqueIci
                )
            }.collect { _financeState.value = it }
        }
    }

    fun loadAttendanceReport(caisseId: Long, cycleId: Long) {
        loadHeaderInfo(caisseId, cycleId)
        viewModelScope.launch {
            combine(
                cycleDao.getAllCycles(caisseId),
                transactionDao.getTransactionsWithMemberByCaisse(caisseId)
            ) { allCycles, transactions ->
                val targetCycle = allCycles.find { it.id == cycleId } ?: return@combine AttendanceReportState()
                val targetNumero = targetCycle.numeroCycle
                val cycleIdsJusqueIci = allCycles.filter { it.numeroCycle <= targetNumero }.map { it.id }.toSet()

                val txCycle = transactions.filter { it.transaction.cycleId == cycleId }
                val txJusqueIci = transactions.filter { it.transaction.cycleId in cycleIdsJusqueIci }

                val retardsCycle = txCycle.count { it.transaction.type == TransactionType.PENALITE_RETARD }
                val retardsJusqueIci = txJusqueIci.count { it.transaction.type == TransactionType.PENALITE_RETARD }
                
                val amendesCycle = txCycle.filter { 
                    it.transaction.type in listOf(TransactionType.PENALITE_RETARD, TransactionType.PENALITE_ECHEC) 
                }.sumOf { abs(it.transaction.montant) }
                
                val amendesJusqueIci = txJusqueIci.filter { 
                    it.transaction.type in listOf(TransactionType.PENALITE_RETARD, TransactionType.PENALITE_ECHEC) 
                }.sumOf { abs(it.transaction.montant) }

                val topCotisants = txCycle.filter { it.transaction.type == TransactionType.COTISATION }
                    .groupBy { it.member.nomSaisi }
                    .mapValues { it.value.sumOf { t -> t.transaction.montant } }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(5)

                AttendanceReportState(
                    retardsCountCycle = retardsCycle,
                    retardsCountJusqueIci = retardsJusqueIci,
                    topCotisants = topCotisants,
                    totalAmendesCycle = amendesCycle,
                    totalAmendesJusqueIci = amendesJusqueIci
                )
            }.collect { _attendanceState.value = it }
        }
    }

    fun loadLoanReport(caisseId: Long, cycleId: Long) {
        loadHeaderInfo(caisseId, cycleId)
        viewModelScope.launch {
            combine(
                cycleDao.getAllCycles(caisseId),
                transactionDao.getTransactionsByCaisse(caisseId),
                transactionDao.getTotalPretsEnCours(caisseId)
            ) { allCycles, transactions, pretsActuels ->
                val targetCycle = allCycles.find { it.id == cycleId } ?: return@combine LoanReportState()
                val targetNumero = targetCycle.numeroCycle
                val cycleIdsJusqueIci = allCycles.filter { it.numeroCycle <= targetNumero }.map { it.id }.toSet()

                val txCycle = transactions.filter { it.cycleId == cycleId }
                val txJusqueIci = transactions.filter { it.cycleId in cycleIdsJusqueIci }

                val interetsCycle = txCycle.filter { it.type == TransactionType.INTERET }.sumOf { it.montant }
                val interetsJusqueIci = txJusqueIci.filter { it.type == TransactionType.INTERET }.sumOf { it.montant }

                LoanReportState(
                    totalDettesJusqueIci = abs(pretsActuels ?: 0),
                    rendementInteretsCycle = interetsCycle,
                    rendementInteretsJusqueIci = interetsJusqueIci,
                    previsionnelTresorerie = emptyList()
                )
            }.collect { _loanState.value = it }
        }
    }

    private fun loadHeaderInfo(caisseId: Long, cycleId: Long) {
        viewModelScope.launch {
            val caisse = caisseDao.getCaisseById(caisseId)
            val cycle = cycleDao.getCycleById(cycleId)
            val cycles = cycleDao.getAllCycles(caisseId).first()
            if (caisse != null) {
                val tontine = tontineDao.getById(caisse.tontineId)
                _headerState.value = ReportHeaderState(
                    tontineName = tontine?.libelle ?: "",
                    caisseName = caisse.libelle,
                    cycleNumber = cycle?.numeroCycle ?: 0,
                    exercice = tontine?.exercice ?: "",
                    dateDebut = cycle?.dateDebut,
                    dateFin = cycle?.dateFin,
                    allCycles = cycles,
                    currentCycleId = cycleId
                )
            }
        }
    }
}
