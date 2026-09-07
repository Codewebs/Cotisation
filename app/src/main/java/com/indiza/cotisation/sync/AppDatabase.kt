package com.indiza.cotisation.sync

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.indiza.cotisation.bank.CycleDao
import com.indiza.cotisation.bank.CycleEntity
import com.indiza.cotisation.bank.PretEntity
import com.indiza.cotisation.bank.SortieCaisseEntity
import com.indiza.cotisation.bank.TourBeneficiaireEntity
import com.indiza.cotisation.bank.TourDao
import com.indiza.cotisation.bank.TourEntity
import com.indiza.cotisation.bank.TransactionDao
import com.indiza.cotisation.bank.TransactionEntity
import com.indiza.cotisation.caisses.CaisseDao
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.tontine.TontineDao
import com.indiza.cotisation.tontine.TontineEntity
import com.indiza.cotisation.tontine.TontineMemberDao
import com.indiza.cotisation.tontine.TontineMemberEntity
import com.indiza.cotisation.tontinemembers.CaisseParticipantEntity
import com.indiza.cotisation.tools.DateConverter
import com.indiza.cotisation.tools.RoleConverter
import com.indiza.cotisation.users.SessionDao
import com.indiza.cotisation.users.SessionEntity
import com.indiza.cotisation.users.TontineRoleEntity
import com.indiza.cotisation.users.UserDao
import com.indiza.cotisation.users.UserEntity

@Database(
    entities = [
        TontineEntity::class,
        CaisseEntity::class,
        UserEntity::class,
        SessionEntity::class,
        SyncQueueEntity::class,
        TontineMemberEntity::class,
        TontineRoleEntity::class,
        CaisseParticipantEntity::class,
        TransactionEntity::class,
        PretEntity::class,
        CycleEntity::class,
        SortieCaisseEntity::class,
        TourEntity::class,
        TourBeneficiaireEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(DateConverter::class, RoleConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tontineDao(): TontineDao
    abstract fun caisseDao(): CaisseDao
    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun tontineMemberDao(): TontineMemberDao
    abstract fun transactionDao(): TransactionDao
    abstract fun cycleDao(): CycleDao
    abstract fun tourDao(): TourDao
    abstract fun pretDao(): com.indiza.cotisation.bank.PretDao
}