package com.indiza.cotisation.bank

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.indiza.cotisation.caisses.MemberPartsCount
import com.indiza.cotisation.tontinemembers.ParticipantWithParts
import kotlinx.coroutines.flow.Flow

@Dao
interface TourDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTour(tour: TourEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeneficiaire(beneficiaire: TourBeneficiaireEntity)

    @Transaction
    @Query("""
        SELECT * FROM caisse_tours 
        WHERE cycleId = :cycleId
        ORDER BY numeroTour ASC
    """)
    fun getToursWithBeneficiairesParCycle(cycleId: Long): Flow<List<TourWithBeneficiaires>>

    @Transaction
    @Query("""
        SELECT * FROM caisse_tours 
        WHERE cycleId = :cycleId
    """)
    fun getToursParCycle(cycleId: Long): Flow<List<TourEntity>>

    @Query("""
        SELECT SUM(nombrePartsAttribuees) 
        FROM tour_beneficiaires tb
        INNER JOIN caisse_tours ct ON tb.tourId = ct.id
        WHERE tb.memberId = :memberId AND ct.cycleId = :cycleId
    """)
    suspend fun getPartsDejaEncaisseesParMembre(memberId: Long, cycleId: Long): Int?

    @Transaction
    suspend fun enregistrerRemiseCagnotte(tour: TourEntity, beneficiaires: List<TourBeneficiaireEntity>) {
        val tourId = insertTour(tour)
        beneficiaires.forEach {
            insertBeneficiaire(it.copy(tourId = tourId))
        }
    }

    @Query("""
    SELECT tb.memberId as memberId, CAST(IFNULL(SUM(tb.nombrePartsAttribuees), 0) AS INTEGER) as count
    FROM tour_beneficiaires tb
    INNER JOIN caisse_tours ct ON tb.tourId = ct.id
    WHERE ct.cycleId = :cycleId AND ct.estTermine = 1
    GROUP BY tb.memberId
""")
    fun getPartsEncaisseesParMembreDuCycle(cycleId: Long): Flow<List<MemberPartsCount>>

    @Query("""
    SELECT tb.memberId as memberId, CAST(IFNULL(SUM(tb.nombrePartsAttribuees), 0) AS INTEGER) as count
    FROM tour_beneficiaires tb
    INNER JOIN caisse_tours ct ON tb.tourId = ct.id
    WHERE ct.cycleId = :cycleId AND ct.estTermine = 1
    GROUP BY tb.memberId
""")
    suspend fun getPartsEncaisseesParMembreDuCycleSnapshot(cycleId: Long): List<MemberPartsCount>

    @Query("""
    SELECT tb.memberId as memberId, CAST(IFNULL(SUM(tb.nombrePartsAttribuees), 0) AS INTEGER) as count
    FROM tour_beneficiaires tb
    INNER JOIN caisse_tours ct ON tb.tourId = ct.id
    WHERE ct.cycleId = :cycleId AND ct.estTermine = 0
    GROUP BY tb.memberId
""")
    suspend fun getPartsPlanifieesParMembreDuCycleSnapshot(cycleId: Long): List<MemberPartsCount>

    @Transaction
    @Query("""
    SELECT tm.*, SUM(tb.nombrePartsAttribuees) as nombreParts
    FROM tontine_members tm
    INNER JOIN tour_beneficiaires tb ON tm.id = tb.memberId
    INNER JOIN caisse_tours ct ON tb.tourId = ct.id
    WHERE ct.cycleId = :cycleId AND ct.estTermine = 0
    GROUP BY tm.id
""")
    fun getParticipantsPlanifiesDuCycle(cycleId: Long): Flow<List<ParticipantWithParts>>

    @Query("DELETE FROM tour_beneficiaires WHERE tourId = :tourId AND memberId = :memberId")
    suspend fun deleteBeneficiaire(tourId: Long, memberId: Long)

    @Query("""
        SELECT ct.id 
        FROM caisse_tours ct
        INNER JOIN tour_beneficiaires tb ON ct.id = tb.tourId
        WHERE tb.memberId = :memberId AND ct.cycleId = :cycleId AND ct.estTermine = :estTermine
        LIMIT 1
    """)
    suspend fun getTourIdForMemberInCycleSnapshot(memberId: Long, cycleId: Long, estTermine: Int): Long?

    @Query("SELECT tourId FROM tour_beneficiaires WHERE memberId = :memberId LIMIT 1")
    suspend fun getTourIdForMember(memberId: Long): Long?

    @Query("SELECT * FROM caisse_tours WHERE id = :tourId")
    suspend fun getTourByIdSnapshot(tourId: Long): TourEntity?

    @Query("SELECT estTermine FROM caisse_tours WHERE id = :tourId")
    suspend fun isTourTermine(tourId: Long): Boolean

    @Query("SELECT COUNT(*) FROM caisse_tours WHERE cycleId = :cycleId")
    suspend fun countToursByCycle(cycleId: Long): Int

    @Query("""
        SELECT SUM(tb.nombrePartsAttribuees)
        FROM tour_beneficiaires tb
        JOIN caisse_tours ct ON tb.tourId = ct.id
        JOIN caisse_cycles cc ON ct.cycleId = cc.id
        WHERE cc.caisseId = :caisseId
    """)
    fun getTotalPartsDistribueesGlobal(caisseId: Long): Flow<Int?>

    @Query("UPDATE caisse_tours SET estTermine = 1 WHERE id = :tourId")
    suspend fun markTourAsTermine(tourId: Long)

    @Query("UPDATE caisse_tours SET montantTotalDistribue = :montant WHERE id = :tourId")
    suspend fun updateMontantTour(tourId: Long, montant: Long)

    @Query("UPDATE tour_beneficiaires SET montantRecu = :montant WHERE tourId = :tourId AND memberId = :memberId")
    suspend fun updateMontantBeneficiaire(tourId: Long, memberId: Long, montant: Long)

    @Query("""
        SELECT tb.memberId as memberId, SUM(tb.nombrePartsAttribuees) as count
        FROM tour_beneficiaires tb
        JOIN caisse_tours ct ON tb.tourId = ct.id
        JOIN caisse_cycles cc ON ct.cycleId = cc.id
        WHERE cc.caisseId = :caisseId AND ct.estTermine = 1
        GROUP BY tb.memberId
    """)
    fun getPartsEncaisseesGlobal(caisseId: Long): Flow<List<MemberPartsCount>>

    @Query("""
        SELECT tb.memberId as memberId, CAST(IFNULL(SUM(tb.nombrePartsAttribuees), 0) AS INTEGER) as count
        FROM tour_beneficiaires tb
        JOIN caisse_tours ct ON tb.tourId = ct.id
        JOIN caisse_cycles cc ON ct.cycleId = cc.id
        WHERE cc.caisseId = :caisseId AND cc.numeroCycle <= :cycleNumber AND ct.estTermine = 1
        GROUP BY tb.memberId
    """)
    fun getPartsEncaisseesJusquauCycle(caisseId: Long, cycleNumber: Int): Flow<List<MemberPartsCount>>

    @Query("""
        SELECT tb.memberId as memberId, CAST(IFNULL(SUM(tb.nombrePartsAttribuees), 0) AS INTEGER) as count
        FROM tour_beneficiaires tb
        JOIN caisse_tours ct ON tb.tourId = ct.id
        JOIN caisse_cycles cc ON ct.cycleId = cc.id
        WHERE cc.caisseId = :caisseId AND cc.numeroCycle <= :cycleNumber AND ct.estTermine = 1
        GROUP BY tb.memberId
    """)
    suspend fun getPartsEncaisseesJusquauCycleSnapshot(caisseId: Long, cycleNumber: Int): List<MemberPartsCount>

    @Query("""
        SELECT tb.memberId as memberId, CAST(IFNULL(SUM(tb.nombrePartsAttribuees), 0) AS INTEGER) as count
        FROM tour_beneficiaires tb
        JOIN caisse_tours ct ON tb.tourId = ct.id
        JOIN caisse_cycles cc ON ct.cycleId = cc.id
        WHERE cc.caisseId = :caisseId AND ct.estTermine = 1
        GROUP BY tb.memberId
    """)
    suspend fun getPartsEncaisseesGlobalSnapshot(caisseId: Long): List<MemberPartsCount>

    @Query("""
        SELECT tb.nombrePartsAttribuees 
        FROM tour_beneficiaires tb
        INNER JOIN caisse_tours ct ON tb.tourId = ct.id
        WHERE tb.memberId = :memberId AND ct.cycleId = :cycleId AND ct.estTermine = 0
        LIMIT 1
    """)
    suspend fun getPartsPlanifieesParMembre(memberId: Long, cycleId: Long): Int?

    @Query("""
        DELETE FROM caisse_tours 
        WHERE id IN (
            SELECT tourId FROM tour_beneficiaires 
            WHERE memberId = :memberId
        ) AND cycleId = :cycleId
    """)
    suspend fun deleteToursByMemberAndCycle(memberId: Long, cycleId: Long)
}
