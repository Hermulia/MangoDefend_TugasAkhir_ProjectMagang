package com.riset.mangodefendd.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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
    val lastScanTimestamp: Long? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val repo: MalwareRepository
) : ViewModel() {
    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState

    init {
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
    }

    fun startTotalScan() {
        viewModelScope.launch {
            _dashboardState.update { 
                it.copy(
                    isScanning = true,
                    scanProgress = 0,
                    scanProgressMax = 0
                )
            }
            val scanWorkRequest = OneTimeWorkRequestBuilder<ScanWorker>().build()
            workManager.enqueueUniqueWork("total_scan", androidx.work.ExistingWorkPolicy.REPLACE, scanWorkRequest)
            
            // Monitor progress (simplified)
            workManager.getWorkInfoByIdLiveData(scanWorkRequest.id).observeForever { workInfo ->
                if (workInfo != null) {
                    val progress = workInfo.progress.getInt("scanned", 0)
                    val total = workInfo.progress.getInt("total", 0)
                    _dashboardState.update { 
                        it.copy(
                            scanProgress = progress,
                            scanProgressMax = total
                        )
                    }
                    if (workInfo.state.isFinished) {
                        _dashboardState.update {
                            it.copy(
                                isScanning = false,
                                lastScanTimestamp = System.currentTimeMillis()
                            )
                        }
                    }
                }
            }
        }
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

