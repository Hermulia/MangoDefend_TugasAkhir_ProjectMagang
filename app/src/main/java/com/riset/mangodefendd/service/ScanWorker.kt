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
            // Initial progress to show we've started
            setProgressAsync(androidx.work.workDataOf("scanned" to 0, "total" to -1))

            val roots = mutableListOf<File>()
            
            // 1. Gather files from public directories or whole storage if permitted
            val external = Environment.getExternalStorageDirectory()
            if (external.exists()) {
                if (com.riset.mangodefendd.util.PermissionUtils.hasStoragePermission(applicationContext)) {
                    // If we have full access, scan the whole external storage
                    roots.add(external)
                } else {
                    // Fallback to specific public folders (though listing might still fail without MANAGE_EXTERNAL_STORAGE on 11+)
                    listOf("Download", "Documents", "DCIM", "Pictures", "Movies", "Music").forEach {
                        val folder = File(external, it)
                        if (folder.exists()) roots.add(folder)
                    }
                }
            }

            // 2. Gather files from those roots
            val filesToScan = ScanEngine.gatherFilesUnderRoots(roots).toMutableList()
            
            // 3. Add Installed APKs
            val apks = ScanEngine.gatherInstalledApkFiles(applicationContext)
            filesToScan.addAll(apks)

            // If still empty, add own APK as fallback
            if (filesToScan.isEmpty()) {
                val packageInfo = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
                packageInfo.applicationInfo?.sourceDir?.let {
                    filesToScan.add(File(it))
                }
            }

            if (filesToScan.isEmpty()) {
                setProgressAsync(androidx.work.workDataOf("scanned" to 0, "total" to 0))
                return Result.success()
            }

            // 4. Perform the scan
            repo.scanFiles(filesToScan) { scanned, total ->
                setProgressAsync(androidx.work.workDataOf("scanned" to scanned, "total" to total))
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}



