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
import kotlinx.coroutines.flow.combine
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

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter: StateFlow<String> = _statusFilter

    private val _totalItems = MutableStateFlow(0)
    val totalItems: StateFlow<Int> = _totalItems

    private val _pendingThreat = MutableStateFlow<ThreatEvent?>(null)
    val pendingThreat: StateFlow<ThreatEvent?> = _pendingThreat

    init {
        viewModelScope.launch {
            repo.activeThreat.collect { event ->
                _pendingThreat.value = event
            }
        }

        viewModelScope.launch {
            repo.getTotalScannedCount().collect { count ->
                _totalItems.value = count
            }
        }

        viewModelScope.launch {
            combine(_displayLimit, _statusFilter) { limit, filter ->
                limit to filter
            }.flatMapLatest { (limit, filter) ->
                repo.getFilteredHistoryFlow(limit, filter)
            }.collect { localHistory ->
                _scanState.value = localHistory
            }
        }

        loadHistory()
    }

    fun setFilter(status: String) {
        _statusFilter.value = status
        _displayLimit.value = 10 // Reset limit when changing filter
    }

    fun loadMore() {
        _displayLimit.value += 10
    }

    fun loadHistory() {
        viewModelScope.launch {
            try {
                repo.getHistory()
            } catch (e: Exception) { }
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

    fun deleteFileAndHistory(result: ScanResultEntity) {
        viewModelScope.launch {
            repo.deletePhysicalFile(result.filePath)
            val updated = result.copy(
                status = "Terhapus",
                actionTaken = "Deleted"
            )
            repo.updateHistoryItem(updated)
        }
    }

    fun scanSingleFile(
        file: File, 
        onDeleteSource: (suspend () -> Unit)? = null, 
        originalPath: String? = null,
        onDone: (ScanResultEntity?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val res = repo.scanFile(file, onDeleteSource = onDeleteSource, originalPath = originalPath)
                onDone(res)
            } catch (e: Exception) {
                onDone(null)
            }
        }
    }

    fun scanBatch(
        files: List<File>, 
        onDeleteSources: Map<String, suspend () -> Unit> = emptyMap(),
        originalPaths: Map<String, String> = emptyMap(),
        progress: (Int,Int) -> Unit, 
        onComplete: (List<ScanResultEntity>) -> Unit
    ) {
        viewModelScope.launch {
            val results = repo.scanFiles(files, onDeleteSources, originalPaths) { s,t -> progress(s,t) }
            onComplete(results)
        }
    }

    fun resolvePendingThreat(action: ScanAction) {
        _pendingThreat.value?.onResponse?.invoke(action)
    }
}
