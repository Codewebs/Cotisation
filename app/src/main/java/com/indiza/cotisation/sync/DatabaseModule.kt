package com.indiza.cotisation.sync

import android.content.Context
import androidx.room.Room
import com.indiza.cotisation.bank.CycleDao
import com.indiza.cotisation.bank.TourDao
import com.indiza.cotisation.bank.TransactionDao
import com.indiza.cotisation.caisses.CaisseDao
import com.indiza.cotisation.tontine.TontineDao
import com.indiza.cotisation.tontine.TontineMemberDao
import com.indiza.cotisation.users.SessionDao
import com.indiza.cotisation.users.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "cotisard.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTontineDao(db: AppDatabase): TontineDao {
        return db.tontineDao()
    }
    @Provides
    fun provideCaisseDao(db: AppDatabase): CaisseDao {
        return db.caisseDao()
    }
    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideSyncQueueDao(db: AppDatabase): SyncQueueDao = db.syncQueueDao()

    @Provides
    fun providetontineMemberDao(db: AppDatabase): TontineMemberDao = db.tontineMemberDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao {
        return db.transactionDao()
    }
    @Provides
    fun provideCycleDao(db: AppDatabase): CycleDao {
        return db.cycleDao()
    }
    @Provides
    fun provideTourDao(db: AppDatabase): TourDao {
        return db.tourDao()
    }
}