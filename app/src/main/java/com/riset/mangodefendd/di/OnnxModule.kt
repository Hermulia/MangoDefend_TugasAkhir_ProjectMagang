package com.riset.mangodefendd.di

import android.content.Context
import com.riset.mangodefendd.ml.BinaryImagePreprocessor
import com.riset.mangodefendd.ml.MalwareRepository
import com.riset.mangodefendd.ml.OnnxMalwareClassifier
import com.riset.mangodefendd.data.scan.ScanResultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

import com.riset.mangodefendd.data.network.ApiService
import com.riset.mangodefendd.auth.TokenManager

@Module
@InstallIn(SingletonComponent::class)
object OnnxModule {
    @Provides
    @Singleton
    fun providePreprocessor(@ApplicationContext context: Context) = BinaryImagePreprocessor(context)

    @Provides
    @Singleton
    fun provideClassifier(@ApplicationContext context: Context) = OnnxMalwareClassifier(context)

    @Provides
    @Singleton
    fun provideRepository(
        @ApplicationContext context: Context,
        classifier: OnnxMalwareClassifier,
        preprocessor: BinaryImagePreprocessor,
        dao: ScanResultDao,
        apiService: ApiService,
        tokenManager: TokenManager
    ) = MalwareRepository(context, classifier, preprocessor, dao, apiService, tokenManager)
}

