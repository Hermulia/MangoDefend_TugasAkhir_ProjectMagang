package com.riset.mangodefendd

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MangoApp : Application(), Configuration.Provider {
	@Inject lateinit var workerFactory: HiltWorkerFactory

	override fun getWorkManagerConfiguration(): Configuration =
		Configuration.Builder()
			.setWorkerFactory(workerFactory)
			.build()
}



