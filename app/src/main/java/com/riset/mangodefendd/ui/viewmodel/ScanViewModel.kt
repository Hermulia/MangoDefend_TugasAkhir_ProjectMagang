package com.riset.mangodefendd.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riset.mangodefendd.ml.MalwareRepository
import com.riset.mangodefendd.data.scan.ScanResultEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repo: MalwareRepository
) : ViewModel() {
    private val _scanState = MutableStateFlow<List<ScanResultEntity>>(emptyList())
    val scanState: StateFlow<List<ScanResultEntity>> = _scanState

    init {
        // Collect local database flow to always have up-to-date data
        viewModelScope.launch {
            repo.getLocalHistoryFlow().collect { localHistory ->
                _scanState.value = localHistory
            }
        }
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
}





