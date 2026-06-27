package com.riset.mangodefendd.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riset.mangodefendd.auth.TokenManager
import com.riset.mangodefendd.data.network.ApiService
import com.riset.mangodefendd.data.network.dto.TransactionDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionUiState(
    val isLoading: Boolean = false,
    val transactions: List<TransactionDto> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState

    fun loadTransactionHistory() {
        val userId = tokenManager.getUserId()
        if (userId == -1) {
            _uiState.update { it.copy(errorMessage = "User not logged in") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val response = apiService.getTransactionHistory(userId)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            transactions = response.body()!!.data
                        ) 
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load transactions: ${response.code()}"
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error"
                    ) 
                }
            }
        }
    }
}
