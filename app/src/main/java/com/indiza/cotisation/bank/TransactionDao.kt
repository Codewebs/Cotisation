package com.indiza.cotisation.bank

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    // Calculer le solde actuel d'une caisse
    @Query("SELECT SUM(montant) FROM transactions WHERE caisseId = :caisseId")
    fun getCaisseBalance(caisseId: Long): Flow<Long?>

    // Historique d'un membre dans une caisse précise
    @Query("SELECT * FROM transactions WHERE memberId = :memberId AND caisseId = :caisseId ORDER BY dateTransaction DESC")
    fun getMemberHistoryInCaisse(memberId: Long, caisseId: Long): Flow<List<TransactionEntity>>

    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE caisseId = :caisseId 
        ORDER BY dateTransaction DESC
    """)
    fun getTransactionsByCaisse(caisseId: Long): Flow<List<TransactionEntity>>

    // Somme de toutes les transactions (Entrées - Sorties)
    @Query("SELECT SUM(montant) FROM transactions WHERE caisseId = :caisseId")
    fun getSoldeGlobal(caisseId: Long): Flow<Long?>

    @Query("""
        SELECT SUM(montant)
        FROM transactions
        WHERE caisseId = :caisseId
          AND cycleId = :cycleId
    """)
    fun getSoldeDuCycle(caisseId: Long, cycleId: Long): Flow<Long?>

    @Query("""
        SELECT SUM(t.montant)
        FROM transactions t
        JOIN caisse_cycles c ON t.cycleId = c.id
        WHERE t.caisseId = :caisseId
          AND c.numeroCycle < :numeroCycle
    """)
    fun getSoldeCyclesPrecedents(caisseId: Long, numeroCycle: Int): Flow<Long?>

    // Somme des montants de type PRET qui n'ont pas encore de REMBOURSEMENT total
    // Pour simplifier, on calcule la somme des transactions de type PRET + REMBOURSEMENT
    @Query("""
        SELECT SUM(montant) FROM transactions 
        WHERE caisseId = :caisseId 
        AND type IN ('PRET', 'REMBOURSEMENT', 'INTERET')
    """)
    fun getTotalPretsEnCours(caisseId: Long): Flow<Long?>

    @Query("""
        SELECT SUM(montant) FROM transactions 
        WHERE caisseId = :caisseId AND memberId = :memberId 
        AND type IN ('PRET', 'REMBOURSEMENT', 'INTERET')
    """)
    fun getEtatDetteMembre(caisseId: Long, memberId: Long): Flow<Long?>

    @Query("SELECT memberId FROM transactions WHERE caisseId = :caisseId AND type = 'TOUR_ENCAISSER'")
    fun getBeneficiairesCagnotteIds(caisseId: Long): Flow<List<Long>>

    @Transaction
    @Query("""
    SELECT * FROM transactions 
    WHERE caisseId = :caisseId 
    ORDER BY dateTransaction DESC
""")
    fun getTransactionsWithMemberByCaisse(caisseId: Long): Flow<List<TransactionWithMember>>

    @Query("""
    SELECT COUNT(*) FROM transactions 
    WHERE caisseId = :caisseId AND type = 'TOUR_ENCAISSER'
""")
    fun getCountToursCompletes(caisseId: Long): Flow<Int>

    @Query("""
    SELECT memberId FROM transactions 
    WHERE caisseId = :caisseId AND cycleId = :cycleId AND type = 'COTISATION'
""")
    fun getCotisantsDuCycle(caisseId: Long, cycleId: Long): Flow<List<Long>>

    @Query("""
    SELECT COUNT(DISTINCT memberId) FROM transactions 
    WHERE caisseId = :caisseId 
    AND cycleId = :cycleId 
    AND type = 'COTISATION'
""")
    suspend fun getNombreCotisationsEnregistrees(caisseId: Long, cycleId: Long): Int

    @Query("""
    SELECT COUNT(*) FROM transactions 
    WHERE caisseId = :caisseId AND cycleId = :cycleId AND type = 'TOUR_ENCAISSER'
""")
    fun getToursCompletesDuCycle(caisseId: Long, cycleId: Long): Flow<Int>

    @Query("""
    SELECT COUNT(*) FROM transactions 
    WHERE caisseId = :caisseId 
    AND cycleId = :cycleId 
    AND type = 'TOUR_ENCAISSER'
""")
    suspend fun getToursCount(caisseId: Long, cycleId: Long): Int

    @Query("""
        SELECT SUM(montant) FROM transactions 
        WHERE caisseId = :caisseId 
        AND cycleId = :cycleId 
        AND type = 'COTISATION' 
        AND a_cotise = 1
    """)
    suspend fun getSommeCotisationsCycle(caisseId: Long, cycleId: Long): Long?

    @Query("SELECT SUM(montant) FROM transactions WHERE caisseId = :caisseId")
    suspend fun getSoldeActuel(caisseId: Long): Long?

    @Query("SELECT COUNT(*) FROM transactions WHERE caisseId = :caisseId")
    suspend fun getTransactionsCount(caisseId: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE caisseId = :caisseId AND memberId = :memberId AND type = :type AND dateTransaction = :dateTransaction")
    suspend fun existsTransaction(caisseId: Long, memberId: Long, type: TransactionType, dateTransaction: Long): Int

    @Query("""
    SELECT 
        (SELECT COUNT(id) FROM caisse_cycles WHERE caisseId = :caisseId AND estCloture = 1) + 1 
    AS nbCyclesTotal
""")
    suspend fun getNombreCyclesEcoules(caisseId: Long): Int

    @Query("""
    SELECT SUM(montant) FROM transactions 
    WHERE caisseId = :caisseId AND memberId = :memberId AND type = 'COTISATION'
""")
    suspend fun getTotalCotiseReel(caisseId: Long, memberId: Long): Long?

    @Query("""
    SELECT SUM(montant) FROM transactions 
    WHERE caisseId = :caisseId AND cycleId = :cycleId AND type = 'COTISATION' AND memberId = :memberId
""")
    fun getSommeCotiseeParMembreDuCycle(caisseId: Long, cycleId: Long, memberId: Long): Flow<Long?>

    @Query("""
    SELECT SUM(ABS(montant)) FROM transactions 
    WHERE caisseId = :caisseId AND cycleId = :cycleId AND type = 'TOUR_ENCAISSER'
""")
    fun getMontantDistribueCycle(caisseId: Long, cycleId: Long): Flow<Long?>

    @Query("""
    SELECT SUM(ABS(montant)) FROM transactions 
    WHERE caisseId = :caisseId AND cycleId = :cycleId AND type = :type
""")
    fun getSommeParTypeEtCycle(caisseId: Long, cycleId: Long, type: TransactionType): Flow<Long?>

    @Query("""
        DELETE FROM transactions 
        WHERE caisseId = :caisseId 
        AND memberId = :memberId 
        AND cycleId = :cycleId 
        AND type = 'TOUR_ENCAISSER'
    """)
    suspend fun deleteTransactionCagnotte(caisseId: Long, memberId: Long, cycleId: Long)

    @Query("""
        SELECT SUM(montant) FROM transactions 
        WHERE memberId = :memberId AND caisseId = :caisseId 
        AND type IN ('PENALITE_RETARD', 'PENALITE_ECHEC')
    """)
    fun getSommePenalitesMembre(memberId: Long, caisseId: Long): Flow<Long?>

    @Query("""
        SELECT COUNT(*) > 0 FROM transactions 
        WHERE memberId = :memberId AND caisseId = :caisseId 
        AND cycleId = :cycleId AND type = :type AND montant < 0
    """)
    suspend fun aDejaPenaliteTypeCycle(memberId: Long, caisseId: Long, cycleId: Long, type: TransactionType): Boolean

    @Query("""
        SELECT SUM(nombreParts) FROM tontine_members m
        JOIN caisse_participants cp ON m.id = cp.memberId
        WHERE cp.caisseId = :caisseId AND m.isActive = 1
    """)
    suspend fun getTotalPartsCaisse(caisseId: Long): Int?

    @Query("""
        SELECT SUM(montant) FROM transactions 
        WHERE memberId = :memberId AND caisseId = :caisseId 
        AND cycleId = :cycleId AND type = 'COTISATION'
    """)
    suspend fun getSommeCotiseeMembreCycle(memberId: Long, caisseId: Long, cycleId: Long): Long?

    @Query("""
        SELECT SUM(t.montant) FROM transactions t
        JOIN caisse_cycles c ON t.cycleId = c.id
        WHERE t.caisseId = :caisseId AND t.memberId = :memberId AND t.type = 'COTISATION'
        AND c.numeroCycle <= :targetNumeroCycle
    """)
    suspend fun getSommeCotisJusqueCycle(caisseId: Long, memberId: Long, targetNumeroCycle: Int): Long?
    @Query("""
        SELECT SUM(t.montant) FROM transactions t
        JOIN caisses c ON t.caisseId = c.id
        WHERE c.tontineId = :tontineId
    """)
    fun getTontineGlobalBalance(tontineId: Long): Flow<Long?>

    @Query("""
        SELECT SUM(ABS(t.montant)) FROM transactions t
        JOIN caisses c ON t.caisseId = c.id
        WHERE c.tontineId = :tontineId AND t.montant < 0
    """)
    fun getTontineTotalOutflows(tontineId: Long): Flow<Long?>

    @Query("""
        SELECT SUM(t.montant) FROM transactions t
        JOIN caisses c ON t.caisseId = c.id
        WHERE c.tontineId = :tontineId AND t.montant > 0
    """)
    fun getTontineTotalInflows(tontineId: Long): Flow<Long?>

    @Query("""
        SELECT SUM(montantPrincipal) FROM prets p
        JOIN caisses c ON p.caisseId = c.id
        WHERE c.tontineId = :tontineId AND p.statut != 'REMBOURSE'
    """)
    fun getTontineTotalActiveLoans(tontineId: Long): Flow<Long?>

    @Query("""
        SELECT SUM(montantInterets) FROM prets p
        JOIN caisses c ON p.caisseId = c.id
        WHERE c.tontineId = :tontineId AND p.statut != 'REMBOURSE'
    """)
    fun getTontineForecastGains(tontineId: Long): Flow<Long?>

    @Query("""
        SELECT SUM(t.montant) FROM transactions t
        JOIN caisses c ON t.caisseId = c.id
        WHERE c.tontineId = :tontineId AND c.possibilitePret = 1
    """)
    fun getTontineLendableBalance(tontineId: Long): Flow<Long?>

    @Query("""
        SELECT SUM(t.montant) FROM transactions t
        JOIN caisses c ON t.caisseId = c.id
        WHERE c.tontineId = :tontineId AND c.possibilitePret = 0
    """)
    fun getTontineNonLendableBalance(tontineId: Long): Flow<Long?>

    @Query("""
        SELECT SUM(montant) FROM transactions 
        WHERE referencePretId = :pretId 
        AND type = 'REMBOURSEMENT'
    """)
    suspend fun getTotalRemboursementPret(pretId: Long): Long?

    @Query("""
        SELECT SUM(montant) FROM transactions 
        WHERE caisseId = :caisseId 
        AND type IN ('PENALITE_RETARD', 'PENALITE_ECHEC')
    """)
    fun getSoldeAmendes(caisseId: Long): Flow<Long?>

    @Query("""
        SELECT SUM(montant) FROM transactions 
        WHERE caisseId = :caisseId 
        AND type = 'FRAIS_CAISSE'
    """)
    fun getSoldeFraisCaisse(caisseId: Long): Flow<Long?>

    @Query("""
        SELECT SUM(montant) FROM transactions 
        WHERE caisseId = :caisseId 
        AND type = 'COTISATION'
    """)
    fun getSoldeCotisations(caisseId: Long): Flow<Long?>
    
    @Query("""
        SELECT type, SUM(montant) as total 
        FROM transactions 
        WHERE caisseId = :caisseId 
        GROUP BY type
    """)
    fun getCaisseBreakdown(caisseId: Long): Flow<List<CaisseBreakdown>>

    @Query("SELECT * FROM transactions WHERE cycleId IN (:cycleIds)")
    fun getTransactionsByCycles(cycleIds: List<Long>): Flow<List<TransactionEntity>>

    @Query("""
        SELECT MAX(dateTransaction) FROM transactions 
        WHERE caisseId = :caisseId AND memberId = :memberId AND type = 'COTISATION'
    """)
    suspend fun getLastContributionDate(caisseId: Long, memberId: Long): Long?

    @Query("""
        SELECT dateTransaction, montant 
        FROM transactions t
        JOIN caisses c ON t.caisseId = c.id
        WHERE c.tontineId = :tontineId
        ORDER BY dateTransaction ASC
    """)
    fun getTontineTransactionsHistory(tontineId: Long): Flow<List<SimpleTransaction>>

    @Query("""
        SELECT t.type as type, SUM(ABS(t.montant)) as total
        FROM transactions t
        JOIN caisses c ON t.caisseId = c.id
        WHERE c.tontineId = :tontineId AND t.montant > 0
        GROUP BY t.type
    """)
    fun getTontineIncomeDistribution(tontineId: Long): Flow<List<IncomeStats>>
}

data class CaisseBreakdown(val type: TransactionType, val total: Long)
data class SimpleTransaction(val dateTransaction: Long, val montant: Long)
data class IncomeStats(val type: TransactionType, val total: Long)
