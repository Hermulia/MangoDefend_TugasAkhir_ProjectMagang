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
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            try {
                val history = repo.getHistory()
                _scanState.value = history
            } catch (e: Exception) {
                // Ignore for now
            }
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





