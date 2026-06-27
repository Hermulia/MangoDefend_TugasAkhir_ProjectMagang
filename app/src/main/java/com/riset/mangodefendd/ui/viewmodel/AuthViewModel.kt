package com.riset.mangodefendd.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.riset.mangodefendd.auth.AuthenticationManager
import com.riset.mangodefendd.auth.AuthUser
import com.riset.mangodefendd.auth.TokenManager
import com.riset.mangodefendd.data.network.ApiService
import com.riset.mangodefendd.data.network.dto.FirebaseLoginDto
import com.riset.mangodefendd.data.network.dto.FirebaseSessionDevice
import com.riset.mangodefendd.ml.MalwareRepository
import com.riset.mangodefendd.service.RealtimeMonitorService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val user: AuthUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthenticationManager,
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val repo: MalwareRepository,
    private val workManager: WorkManager
) : ViewModel() {
    private val _authState = MutableStateFlow(
        AuthUiState(
            isLoggedIn = authManager.isLoggedIn() && tokenManager.getToken() != null,
            user = authManager.getCurrentUser()
        )
    )
    val authState: StateFlow<AuthUiState> = _authState

    fun signInWithGoogle(idToken: String) {
        _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val (user, firebaseToken) = authManager.signInWithGoogle(idToken)

                val deviceId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
                val device = FirebaseSessionDevice(
                    hardwareId = deviceId,
                    hostname = android.os.Build.MODEL ?: "Android Device",
                    appType = "android",
                    osType = "android"
                )

                val response = apiService.firebaseLogin(FirebaseLoginDto(firebaseToken, device))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    tokenManager.saveToken(authResponse.data.accessToken)
                    tokenManager.saveUserId(authResponse.data.user.id)
                    _authState.value = AuthUiState(isLoggedIn = true, user = user)
                } else {
                    authManager.signOut()
                    _authState.value = AuthUiState(isLoading = false, errorMessage = "Backend login failed: ${response.code()}")
                }
            } catch (e: Exception) {
                authManager.signOut()
                _authState.value = AuthUiState(isLoading = false, errorMessage = e.message ?: "Unknown error")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // Hentikan scan yang sedang berjalan
            workManager.cancelUniqueWork("total_scan")

            // Hentikan perlindungan real-time
            try {
                val intent = Intent(context, RealtimeMonitorService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                // Abaikan jika gagal berhenti
            }

            authManager.signOut()
            tokenManager.clearToken()
            tokenManager.clearUserId()
            repo.clearLocalHistory()
            _authState.value = AuthUiState(isLoggedIn = false, user = null)
        }
    }

    fun getSignInIntent() = authManager.getSignInIntent()
}
