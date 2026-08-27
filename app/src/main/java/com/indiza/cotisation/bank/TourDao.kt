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
    SELECT tb.memberId as memberId, SUM(tb.nombrePartsAttribuees) as count
    FROM tour_beneficiaires tb
    INNER JOIN caisse_tours ct ON tb.tourId = ct.id
    WHERE ct.cycleId = :cycleId
    GROUP BY tb.memberId
""")
    fun getPartsEncaisseesParMembreDuCycle(cycleId: Long): Flow<List<MemberPartsCount>>

    @Transaction
    @Query("""
    SELECT tm.*, tb.nombrePartsAttribuees as nombreParts
    FROM tontine_members tm
    INNER JOIN tour_beneficiaires tb ON tm.id = tb.memberId
    INNER JOIN caisse_tours ct ON tb.tourId = ct.id
    WHERE ct.cycleId = :cycleId
""")
    fun getParticipantsPlanifiesDuCycle(cycleId: Long): Flow<List<ParticipantWithParts>>

    @Query("DELETE FROM tour_beneficiaires WHERE tourId = :tourId AND memberId = :memberId")
    suspend fun deleteBeneficiaire(tourId: Long, memberId: Long)

    @Query("SELECT tourId FROM tour_beneficiaires WHERE memberId = :memberId LIMIT 1")
    suspend fun getTourIdForMember(memberId: Long): Long?

    // Pour vérifier si le tour est déjà clôturé (estTermine)
    @Query("SELECT estTermine FROM caisse_tours WHERE id = :tourId")
    suspend fun isTourTermine(tourId: Long): Boolean

}