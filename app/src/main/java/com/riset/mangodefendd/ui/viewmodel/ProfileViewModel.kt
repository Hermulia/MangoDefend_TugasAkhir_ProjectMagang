package com.riset.mangodefendd.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riset.mangodefendd.auth.TokenManager
import com.riset.mangodefendd.data.network.ApiService
import com.riset.mangodefendd.data.network.dto.SubscriptionDto
import com.riset.mangodefendd.data.network.dto.UpdateProfileRequest
import com.riset.mangodefendd.data.network.dto.UserProfileDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: UserProfileDto? = null,
    val activeSubscription: SubscriptionDto? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    fun loadProfile() {
        val userId = tokenManager.getUserId()
        if (userId == -1) {
            _uiState.value = _uiState.value.copy(errorMessage = "User not found")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                // Fetch Profile
                val profileResponse = apiService.getProfile(userId)
                
                // Fetch Active Subscription
                val subResponse = apiService.getActiveSubscription(userId)
                val activeSub = if (subResponse.isSuccessful) {
                    subResponse.body()?.firstOrNull { it.isActive }
                } else null

                if (profileResponse.isSuccessful && profileResponse.body() != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profile = profileResponse.body()!!.data,
                        activeSubscription = activeSub
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Gagal memuat profil: ${profileResponse.code()}",
                        activeSubscription = activeSub
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Terjadi kesalahan"
                )
            }
        }
    }

    fun updateProfile(displayName: String, photoUrl: String) {
        val userId = tokenManager.getUserId()
        if (userId == -1) return
        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            try {
                val request = UpdateProfileRequest(
                    displayName = displayName.ifBlank { null },
                    photoUrl = photoUrl.ifBlank { null }
                )
                val response = apiService.updateProfile(userId, request)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        profile = response.body()!!.data,
                        successMessage = "Profil berhasil diperbarui!"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = "Gagal menyimpan profil: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Terjadi kesalahan"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
