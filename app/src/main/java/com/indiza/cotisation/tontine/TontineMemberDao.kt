package com.indiza.cotisation.tontine

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.indiza.cotisation.tontinemembers.ParticipantWithParts
import com.indiza.cotisation.users.Role
import com.indiza.cotisation.users.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TontineMemberDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: TontineMemberEntity)

    @Query("DELETE FROM tontine_members WHERE tontineId = :tontineId AND userId = :userId")
    suspend fun removeMember(tontineId: Long, userId: String)

    @Query("SELECT * FROM tontine_members WHERE userId = :userId AND isActive = 1")
    suspend fun getMemberTontines(userId: String): List<TontineMemberEntity>

    @Query("SELECT * FROM tontine_members WHERE tontineId = :tontineId AND isActive = 1")
    suspend fun getTontineMembers(tontineId: Long): List<TontineMemberEntity>

    @Query("SELECT * FROM tontine_members WHERE tontineId = :tontineId AND phone = :phone LIMIT 1")
    suspend fun getMemberByPhone(tontineId: Long, phone: String): TontineMemberEntity?

    @Query("SELECT COUNT(*) FROM tontine_members WHERE tontineId = :tontineId AND isActive = 1")
    suspend fun getMemberCount(tontineId: Long): Int

    @Query("SELECT role FROM tontine_members WHERE tontineId = :tontineId AND userId = :userId")
    suspend fun getUserRoleInTontine(tontineId: Long, userId: String): Role?

    @Query("""
        SELECT t.* FROM tontines t
        INNER JOIN tontine_members m ON t.id = m.tontineId
        WHERE m.userId = :userId AND m.isActive = 1
    """)
    fun getTontinesForUser(userId: String): Flow<List<TontineEntity>>

    @Transaction
    @Query("""
        SELECT t.* FROM tontines t
        INNER JOIN tontine_members m ON t.id = m.tontineId
        WHERE m.userId = :userId AND m.isActive = 1
        ORDER BY t.dateDebut DESC
    """)
    fun getUserTontinesWithCaisses(userId: String): Flow<List<TontineWithCaisses>>

    @Transaction
    @Query("SELECT * FROM tontine_members WHERE tontineId = :tontineId AND isActive = 1")
    fun getTontineMembersWithUsers(tontineId: Long): Flow<List<TontineMemberWithUser>>

    @Query("""
        SELECT DISTINCT u.* FROM users u
        INNER JOIN tontine_members m ON u.id = m.userId
        WHERE u.isActive = 1
    """)
    suspend fun getAllKnownMembers(): List<UserEntity>

    @Transaction
    @Query("""
    SELECT tm.*, cp.nombreParts FROM tontine_members tm
    INNER JOIN caisse_participants cp ON tm.id = cp.memberId
    WHERE cp.caisseId = :caisseId
    ORDER BY cp.ordrePassage ASC
""")
    fun getParticipantsByCaisse(caisseId: Long): Flow<List<ParticipantWithParts>>

    @Transaction
    @Query("""
    SELECT tm.*, cp.nombreParts FROM tontine_members tm
    INNER JOIN caisse_participants cp ON tm.id = cp.memberId
    WHERE cp.caisseId = :caisseId
    ORDER BY cp.ordrePassage ASC
""")
    suspend fun getParticipantsByCaisseSnapshot(caisseId: Long): List<ParticipantWithParts>

    @Query("UPDATE caisse_participants SET ordrePassage = :nouveauOrdre WHERE caisseId = :caisseId AND memberId = :memberId")
    suspend fun updateParticipantOrder(caisseId: Long, memberId: Long, nouveauOrdre: Int)

    @Transaction
    @Query("SELECT * FROM tontine_members WHERE id = :memberId")
    suspend fun getMemberWithUser(memberId: Long): TontineMemberWithUser?

    @Query("""
    UPDATE tontine_members 
    SET nomSaisi = :nom, phone = :phone, role = :role 
    WHERE id = :memberId
""")
    suspend fun updateMemberInfo(memberId: Long, nom: String, phone: String, role: Role)

    @Query("SELECT COUNT(*) FROM caisse_participants WHERE caisseId = :caisseId")
    suspend fun getParticipantsCount(caisseId: Long): Int

    @Transaction
    @Query("""
    SELECT tm.*, cp.nombreParts AS nombreParts 
    FROM tontine_members tm
    INNER JOIN caisse_participants cp ON tm.id = cp.memberId
    WHERE cp.caisseId = :caisseId
    ORDER BY cp.ordrePassage ASC
""")
    fun getParticipantsWithPartsByCaisse(caisseId: Long): Flow<List<ParticipantWithParts>>


    @Transaction
    @Query("""
    SELECT tm.*, cp.nombreParts 
    FROM tontine_members tm
    INNER JOIN caisse_participants cp ON tm.id = cp.memberId
    WHERE cp.memberId = :memberId AND cp.caisseId = :caisseId
""")
    suspend fun getParticipantWithParts(memberId: Long, caisseId: Long): ParticipantWithParts?

    @Query("SELECT * FROM tontine_members WHERE tontineId = :tontineId AND userId = :userId")
    suspend fun getMemberEntity(tontineId: Long, userId: String): TontineMemberEntity?

    @Query("SELECT * FROM tontine_members WHERE id = :memberId")
    suspend fun getMemberById(memberId: Long): TontineMemberEntity?

    @Query("SELECT * FROM tontine_members WHERE remoteId = :remoteId")
    suspend fun getMemberByRemoteId(remoteId: String): TontineMemberEntity?

    @Query("SELECT * FROM tontine_members WHERE tontineId = :tontineId AND remoteId = :remoteId")
    suspend fun getMemberByRemoteId(tontineId: Long, remoteId: String): TontineMemberEntity?
}
