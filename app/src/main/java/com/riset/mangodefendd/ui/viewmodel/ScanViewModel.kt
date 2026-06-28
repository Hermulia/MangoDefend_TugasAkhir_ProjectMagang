package com.riset.mangodefendd.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riset.mangodefendd.ml.MalwareRepository
import com.riset.mangodefendd.ml.ThreatEvent
import com.riset.mangodefendd.ml.ScanAction
import com.riset.mangodefendd.data.scan.ScanResultEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repo: MalwareRepository
) : ViewModel() {
    private val _scanState = MutableStateFlow<List<ScanResultEntity>>(emptyList())
    val scanState: StateFlow<List<ScanResultEntity>> = _scanState

    private val _displayLimit = MutableStateFlow(10)
    val displayLimit: StateFlow<Int> = _displayLimit

    private val _totalItems = MutableStateFlow(0)
    val totalItems: StateFlow<Int> = _totalItems

    private val _pendingThreat = MutableStateFlow<ThreatEvent?>(null)
    val pendingThreat: StateFlow<ThreatEvent?> = _pendingThreat

    init {
        // Listen for threats during scan
        viewModelScope.launch {
            repo.pendingThreat.collect { event ->
                _pendingThreat.value = event
            }
        }

        // Collect total count
        viewModelScope.launch {
            repo.getTotalScannedCount().collect { count ->
                _totalItems.value = count
            }
        }

        // Collect local database flow with dynamic limit
        viewModelScope.launch {
            _displayLimit.flatMapLatest { limit ->
                repo.getPagedHistoryFlow(limit)
            }.collect { localHistory ->
                _scanState.value = localHistory
            }
        }

        // Fetch remote history on start
        loadHistory()
    }

    fun loadMore() {
        _displayLimit.value += 10
    }

    fun loadHistory() {
        viewModelScope.launch {
            try {
                // This will trigger a sync or fetch remote data
                repo.getHistory()
            } catch (e: Exception) {
                // Ignore for now
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repo.clearLocalHistory()
        }
    }

    fun deleteHistoryItems(ids: List<String>) {
        viewModelScope.launch {
            repo.deleteHistoryItems(ids)
        }
    }

    fun scanSingleFile(file: File, onDone: (ScanResultEntity?) -> Unit) {
        viewModelScope.launch {
            try {
                val res = repo.scanFile(file)
                onDone(res)
            } catch (e: Exception) {
                onDone(null)
            }
        }
    }

    fun scanBatch(files: List<File>, progress: (Int,Int) -> Unit, onComplete: (List<ScanResultEntity>) -> Unit) {
        viewModelScope.launch {
            val results = repo.scanFiles(files) { s,t -> progress(s,t) }
            onComplete(results)
        }
    }

    fun resolvePendingThreat(action: ScanAction) {
        _pendingThreat.value?.onResponse?.invoke(action)
        _pendingThreat.value = null
    }
}





