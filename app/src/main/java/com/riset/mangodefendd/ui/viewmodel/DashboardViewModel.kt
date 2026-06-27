package com.riset.mangodefendd.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.riset.mangodefendd.auth.TokenManager
import com.riset.mangodefendd.data.network.ApiService
import com.riset.mangodefendd.data.network.dto.SubscriptionDto
import com.riset.mangodefendd.ml.MalwareRepository
import com.riset.mangodefendd.service.RealtimeMonitorService
import com.riset.mangodefendd.service.ScanWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val totalScanned: Int = 0,
    val malwareDetected: Int = 0,
    val suspiciousFiles: Int = 0,
    val safeFiles: Int = 0,
    val isScanning: Boolean = false,
    val isRealtimeActive: Boolean = false,
    val scanProgress: Int = 0,
    val scanProgressMax: Int = 0,
    val lastScanTimestamp: Long? = null,
    val activeSubscription: SubscriptionDto? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val repo: MalwareRepository,
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState

    init {
        loadSubscription()
        // Monitor Stats from Database
        viewModelScope.launch {
            combine(
                repo.getTotalScannedCount(),
                repo.getMalwareCount(),
                repo.getSuspiciousCount(),
                repo.getSafeCount(),
                repo.getLastScanTimestamp()
            ) { total, malware, suspicious, safe, lastScan ->
                _dashboardState.update { state ->
                    state.copy(
                        totalScanned = total,
                        malwareDetected = malware,
                        suspiciousFiles = suspicious,
                        safeFiles = safe,
                        lastScanTimestamp = lastScan
                    )
                }
            }.collect {}
        }

        // Monitor Scan Progress from WorkManager (Persistent across navigation)
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkLiveData("total_scan").asFlow().collect { workInfos ->
                val workInfo = workInfos.firstOrNull()
                if (workInfo != null) {
                    val isRunning = workInfo.state == WorkInfo.State.RUNNING || 
                                    workInfo.state == WorkInfo.State.ENQUEUED
                    
                    val progress = workInfo.progress.getInt("scanned", 0)
                    val total = workInfo.progress.getInt("total", 0)
                    
                    _dashboardState.update { 
                        it.copy(
                            isScanning = isRunning,
                            scanProgress = progress,
                            scanProgressMax = total
                        )
                    }
                }
            }
        }
    }

    fun loadSubscription() {
        val userId = tokenManager.getUserId()
        if (userId != -1) {
            viewModelScope.launch {
                try {
                    val response = apiService.getActiveSubscription(userId)
                    if (response.isSuccessful) {
                        val activeSub = response.body()?.firstOrNull { it.isActive }
                        _dashboardState.update { it.copy(activeSubscription = activeSub) }
                    }
                } catch (e: Exception) {
                    // Ignore errors for dashboard
                }
            }
        }
    }

    fun startTotalScan(onLimitReached: () -> Unit) {
        val activeSub = _dashboardState.value.activeSubscription
        val limit = activeSub?.plan?.fullScanLimit ?: 1 // Default 1 for guest/free if not specified
        
        // In a real app, we'd check if (currentDayScans >= limit)
        // For this sync, we just demonstrate the limit awareness
        if (limit == 0) {
            onLimitReached()
            return
        }

        val scanWorkRequest = OneTimeWorkRequestBuilder<ScanWorker>().build()
        workManager.enqueueUniqueWork(
            "total_scan",
            androidx.work.ExistingWorkPolicy.REPLACE,
            scanWorkRequest
        )
    }

    fun stopTotalScan() {
        workManager.cancelUniqueWork("total_scan")
        _dashboardState.update { 
            it.copy(
                isScanning = false,
                scanProgress = 0,
                scanProgressMax = 0
            ) 
        }
    }

    fun toggleRealtimeProtection(enabled: Boolean) {
        if (enabled) {
            val intent = Intent(context, RealtimeMonitorService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
            _dashboardState.update { it.copy(isRealtimeActive = true) }
        } else {
            val intent = Intent(context, RealtimeMonitorService::class.java)
            context.stopService(intent)
            _dashboardState.update { it.copy(isRealtimeActive = false) }
        }
    }

    fun updateStats(scanned: Int, malware: Int, suspicious: Int, safe: Int) {
        _dashboardState.value = _dashboardState.value.copy(
            totalScanned = scanned,
            malwareDetected = malware,
            suspiciousFiles = suspicious,
            safeFiles = safe
        )
    }
}

