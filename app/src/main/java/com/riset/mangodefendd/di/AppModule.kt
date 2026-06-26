package com.riset.mangodefendd.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.riset.mangodefendd.data.scan.ScanDatabase
import com.riset.mangodefendd.data.scan.ScanResultDao
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScanDatabase {
        return Room.databaseBuilder(context, ScanDatabase::class.java, "scan-db").build()
    }

    @Provides
    fun provideScanResultDao(db: ScanDatabase): ScanResultDao = db.scanResultDao()
}

