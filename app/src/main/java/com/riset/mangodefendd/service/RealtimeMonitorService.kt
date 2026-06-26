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

            val dirsToWatch = listOf(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            )
            
            dirsToWatch.forEach { dir ->
                if (dir?.exists() == true) {
                    val obs = object : FileObserver(dir.absolutePath, CLOSE_WRITE) {
                        override fun onEvent(event: Int, path: String?) {
                            if (path == null || repo == null) return
                            val f = File(dir, path)
                            if (f.isFile && f.canRead()) {
                                scope.launch {
                                    try {
                                        val r = repo.scanFile(f)
                                        if (r.status == "Dangerous") {
                                            // notify
                                            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                                            nm.notify(r.id.hashCode(), buildNotification("Malware detected: ${r.fileName}"))
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
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
            stopSelf()
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, "realtime_channel")
            .setContentTitle("MangoDefendd")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
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

