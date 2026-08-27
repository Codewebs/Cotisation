package com.indiza.cotisation.bank

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity)

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
          AND type = 'COTISATION'
          AND montant > 0
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

    @Query("SELECT COUNT(*) FROM transactions WHERE caisseId = :caisseId AND type = 'TOUR_ENCAISSER'")
    fun getBeneficiairesCagnotte(caisseId: Long): Flow<List<Long>>

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
}