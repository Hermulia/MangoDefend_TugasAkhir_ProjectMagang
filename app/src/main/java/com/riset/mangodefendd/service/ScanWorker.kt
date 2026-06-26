package com.riset.mangodefendd.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.os.Environment
import com.riset.mangodefendd.ml.MalwareRepository
import com.riset.mangodefendd.ml.ScanEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker
import java.io.File

@HiltWorker
class ScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: MalwareRepository
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val roots = mutableListOf<File>()
            
            // Only scan user-relevant directories, avoid scanning app's own internal data/cache
            val external = Environment.getExternalStorageDirectory()
            if (external.exists()) {
                roots.add(File(external, "Download"))
                roots.add(File(external, "Documents"))
                roots.add(File(external, "DCIM"))
            }

            val files = ScanEngine.gatherFilesUnderRoots(roots)
            repo.scanFiles(files) { scanned, total ->
                setProgressAsync(androidx.work.workDataOf("scanned" to scanned, "total" to total))
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}



