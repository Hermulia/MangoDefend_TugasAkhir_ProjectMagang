package com.riset.mangodefendd.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.riset.mangodefendd.ml.MalwareRepository
import com.riset.mangodefendd.ml.ScanEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker

@HiltWorker
class ScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: MalwareRepository
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val root = applicationContext.cacheDir
            val files = ScanEngine.gatherFilesUnderRoots(listOf(root))
            repo.scanFiles(files) { scanned, total ->
                // Progress update via coroutine - safe to call
                setProgressAsync(androidx.work.workDataOf("scanned" to scanned, "total" to total))
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}



