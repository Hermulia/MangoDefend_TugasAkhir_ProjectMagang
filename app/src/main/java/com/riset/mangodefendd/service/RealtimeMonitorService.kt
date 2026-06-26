package com.riset.mangodefendd.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.FileObserver
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import com.riset.mangodefendd.ml.OnnxMalwareClassifier
import com.riset.mangodefendd.ml.BinaryImagePreprocessor
import com.riset.mangodefendd.ml.MalwareRepository
import com.riset.mangodefendd.data.scan.ScanDatabase

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RealtimeMonitorService : Service() {

    private val observers = mutableListOf<FileObserver>()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    @Inject
    lateinit var repo: MalwareRepository

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, buildNotification("Realtime Protection active"), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, buildNotification("Realtime Protection active"))
        }

        try {

            val dirsToWatch = mutableListOf<File?>()
            dirsToWatch.add(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS))
            dirsToWatch.add(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS))
            dirsToWatch.add(getExternalFilesDir(null))

            dirsToWatch.filterNotNull().forEach { dir ->
                if (dir.exists()) {
                    val obs = object : FileObserver(dir.absolutePath, CLOSE_WRITE) {
                        override fun onEvent(event: Int, path: String?) {
                            if (path == null) return
                            val f = File(dir, path)
                            
                            if (f.exists() && f.isFile && f.canRead()) {
                                scope.launch {
                                    try {
                                        // Wait a tiny bit to ensure file system is ready
                                        kotlinx.coroutines.delay(500)
                                        val result = repo.scanFile(f)
                                        if (result.status == "Dangerous" || result.status == "Suspicious") {
                                            showDetectionNotification(result.fileName, result.status)
                                        }
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        }
                    }
                    obs.startWatching()
                    observers.add(obs)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showDetectionNotification(fileName: String, status: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, "realtime_channel")
            .setContentTitle("Threat Detected!")
            .setContentText("$fileName is marked as $status")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(fileName.hashCode(), notification)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, "realtime_channel")
            .setContentTitle("MangoDefend Real-time")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("realtime_channel", "Realtime Protection", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        observers.forEach { 
            try {
                it.stopWatching()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

