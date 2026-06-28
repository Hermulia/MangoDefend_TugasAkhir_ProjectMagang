package com.riset.mangodefendd.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.riset.mangodefendd.ml.MalwareRepository
import com.riset.mangodefendd.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RealtimeMonitorService : Service() {

    private val observers = mutableMapOf<String, FileObserver>()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    @Inject
    lateinit var repo: MalwareRepository

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        val notification = buildNotification("Real-time protection is active and monitoring your device.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(101, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(101, notification)
        }

        setupObservers()
    }

    private fun setupObservers() {
        scope.launch {
            val root = Environment.getExternalStorageDirectory()
            if (PermissionUtils.hasStoragePermission(applicationContext)) {
                // Monitor root and common entry points recursively
                startWatchingRecursive(root, depth = 0)
            } else {
                // Fallback to public folders if full permission not granted
                listOf(
                    Environment.DIRECTORY_DOWNLOADS,
                    Environment.DIRECTORY_DOCUMENTS,
                    Environment.DIRECTORY_DCIM,
                    Environment.DIRECTORY_PICTURES
                ).forEach { dirName ->
                    val dir = Environment.getExternalStoragePublicDirectory(dirName)
                    if (dir.exists()) startWatchingRecursive(dir, depth = 0)
                }
            }
        }
    }

    private fun startWatchingRecursive(dir: File, depth: Int) {
        // Limit depth to avoid too many inotify watches and battery drain
        if (depth > 3 || observers.size > 150) return
        
        val path = dir.absolutePath
        if (observers.containsKey(path)) return

        try {
            val observer = object : FileObserver(path, CLOSE_WRITE or MOVED_TO or CREATE) {
                override fun onEvent(event: Int, name: String?) {
                    if (name == null) return
                    val file = File(dir, name)
                    
                    val mask = event and ALL_EVENTS
                    if (mask == CREATE && file.isDirectory) {
                        // Watch new directories as they are created
                        startWatchingRecursive(file, depth + 1)
                    } else if (mask == CLOSE_WRITE || mask == MOVED_TO) {
                        if (file.exists() && file.isFile) {
                            handleNewFile(file)
                        }
                    }
                }
            }
            observer.startWatching()
            observers[path] = observer

            // Recursively add existing subdirectories
            dir.listFiles()?.forEach {
                if (it.isDirectory && !it.name.startsWith(".") && isInterestingDir(it.name)) {
                    startWatchingRecursive(it, depth + 1)
                }
            }
        } catch (e: Exception) {
            // Ignore permission or access errors for specific folders
        }
    }

    private fun isInterestingDir(name: String): Boolean {
        val n = name.lowercase()
        val ignored = setOf("android", "data", "lost.dir", "backups")
        if (n in ignored) return false
        
        // Focus on common entry points for new files
        val critical = setOf("download", "documents", "whatsapp", "telegram", "pictures", "dcim", "movies")
        return n in critical || observers.size < 50 // Be more generous if we haven't hit many observers yet
    }

    private fun handleNewFile(file: File) {
        // Skip hidden files or system files
        if (file.name.startsWith(".") || file.length() == 0L) return
        
        // Skip large files (> 50MB) for real-time to avoid lag
        if (file.length() > 50 * 1024 * 1024) return

        scope.launch {
            try {
                // Wait a bit to ensure the file is fully unlocked by the system/app
                delay(1000)
                if (file.exists() && file.canRead()) {
                    val result = repo.scanFile(file)
                    if (result.status != "Safe") {
                        showDetectionNotification(result.fileName, result.status)
                    }
                }
            } catch (e: Exception) {
                // Ignore errors during background scan
            }
        }
    }

    private fun showDetectionNotification(fileName: String, status: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, "realtime_channel")
            .setContentTitle("Threat Detected!")
            .setContentText("Potential $status file: $fileName")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        nm.notify(fileName.hashCode(), notification)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, "realtime_channel")
            .setContentTitle("MangoDefend Protection")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(
                NotificationChannel(
                    "realtime_channel",
                    "Real-time Protection",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Monitors device for new files and potential threats."
                }
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        observers.values.forEach { it.stopWatching() }
        observers.clear()
    }
}
