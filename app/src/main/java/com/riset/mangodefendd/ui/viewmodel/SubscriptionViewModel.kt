package com.riset.mangodefendd.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riset.mangodefendd.auth.TokenManager
import com.riset.mangodefendd.data.network.ApiService
import com.riset.mangodefendd.data.network.dto.CreateTransactionRequest
import com.riset.mangodefendd.data.network.dto.PlanDto
import com.riset.mangodefendd.data.network.dto.SubscriptionDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val plans: List<PlanDto> = emptyList(),
    val activeSubscription: SubscriptionDto? = null,
    val errorMessage: String? = null,
    val checkoutSuccessMessage: String? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState

    fun loadData() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, checkoutSuccessMessage = null) }
        viewModelScope.launch {
            try {
                // Fetch all plans
                val planResponse = apiService.getAllPlans()
                val plans = if (planResponse.isSuccessful) {
                    val rawPlans = planResponse.body() ?: emptyList()
                    // Sort by price (ascending)
                    rawPlans.sortedBy { it.price.toDoubleOrNull() ?: 0.0 }
                } else {
                    emptyList()
                }

                // Fetch active subscription
                val userId = tokenManager.getUserId()
                var activeSub: SubscriptionDto? = null
                if (userId != -1) {
                    val subResponse = apiService.getActiveSubscription(userId)
                    if (subResponse.isSuccessful) {
                        val subs = subResponse.body() ?: emptyList()
                        activeSub = subs.firstOrNull { it.isActive }
                    }
                }

                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        plans = plans,
                        activeSubscription = activeSub
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load data") }
            }
        }
    }

    fun checkoutPlan(planId: Int, method: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, checkoutSuccessMessage = null) }
        viewModelScope.launch {
            try {
                // 1. Checkout to create pending transaction
                val request = CreateTransactionRequest(planId = planId, method = method)
                val response = apiService.checkout(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val transactionId = response.body()!!.data.id
                    
                    // 2. Simulate Payment Webhook Success
                    val webhookResponse = apiService.simulatePaymentSuccess(transactionId)
                    if (webhookResponse.isSuccessful) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                checkoutSuccessMessage = "Payment simulated successfully! Your subscription is now active."
                            ) 
                        }
                        // Refresh data
                        loadData()
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to simulate payment") }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Checkout failed: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error during checkout") }
            }
        }
    }
    
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, checkoutSuccessMessage = null) }
    }
}
