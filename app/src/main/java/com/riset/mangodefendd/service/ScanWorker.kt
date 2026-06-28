package com.riset.mangodefendd.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.riset.mangodefendd.ml.MalwareRepository
import com.riset.mangodefendd.ml.ScanEngine
import com.riset.mangodefendd.util.PermissionUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File

@HiltWorker
class ScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: MalwareRepository
) : CoroutineWorker(appContext, params) {

    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "scan_channel"
    private val notificationId = 202

    override suspend fun doWork(): Result {
        createNotificationChannel()
        setForeground(createForegroundInfo(0, -1))

        return try {
            setProgress(workDataOf("scanned" to 0, "total" to -1))

            val roots = mutableListOf<File>()
            val external = Environment.getExternalStorageDirectory()
            if (external.exists()) {
                if (PermissionUtils.hasStoragePermission(applicationContext)) {
                    roots.add(external)
                } else {
                    listOf(
                        Environment.DIRECTORY_DOWNLOADS,
                        Environment.DIRECTORY_DOCUMENTS,
                        Environment.DIRECTORY_DCIM,
                        Environment.DIRECTORY_PICTURES
                    ).forEach { dirName ->
                        val folder = Environment.getExternalStoragePublicDirectory(dirName)
                        if (folder.exists()) roots.add(folder)
                    }
                }
            }

            val filesToScan = ScanEngine.gatherFilesUnderRoots(roots).toMutableList()
            val apks = ScanEngine.gatherInstalledApkFiles(applicationContext)
            filesToScan.addAll(apks)

            if (filesToScan.isEmpty()) {
                val packageInfo = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
                packageInfo.applicationInfo?.sourceDir?.let { filesToScan.add(File(it)) }
            }

            if (filesToScan.isEmpty()) {
                showResultNotification(0, 0)
                return Result.success()
            }

            val total = filesToScan.size
            repo.scanFiles(filesToScan) { scanned, t ->
                // Update WorkManager progress for the UI
                setProgress(workDataOf("scanned" to scanned, "total" to t))
                
                // Update system notification
                notificationManager.notify(notificationId, createNotification(scanned, t))
            }

            val scanResults = repo.getLocalHistoryFlow().first()
            val malwareFound = scanResults.count { it.status != "Safe" }
            
            showResultNotification(total, malwareFound)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun createForegroundInfo(scanned: Int, total: Int): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                createNotification(scanned, total),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, createNotification(scanned, total))
        }
    }

    private fun createNotification(scanned: Int, total: Int): Notification {
        val title = "MangoDefend Scanning..."
        val content = if (total > 0) "Scanned $scanned of $total files" else "Preparing files..."
        
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (total > 0) {
            builder.setProgress(total, scanned, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    private fun showResultNotification(total: Int, malware: Int) {
        val title = if (malware > 0) "Scan Complete - Action Required" else "Scan Complete"
        val content = "Finished scanning $total files. Threats found: $malware"
        
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(if (malware > 0) android.R.drawable.ic_dialog_alert else android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(notificationId + 1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "System Scan",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of full system malware scans."
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
