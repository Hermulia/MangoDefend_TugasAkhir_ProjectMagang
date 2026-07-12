package com.riset.mangodefendd.data.network.dto

import com.google.gson.annotations.SerializedName

data class FirebaseSessionDevice(
    @SerializedName("hardware_id") val hardwareId: String,
    @SerializedName("hostname") val hostname: String,
    @SerializedName("app_type") val appType: String, // "android", "ios", "web"
    @SerializedName("os_type") val osType: String // "android", "ios", "windows", "macos", "linux"
)

data class FirebaseLoginDto(
    val idToken: String,
    val device: FirebaseSessionDevice
)

data class AuthResponse(
    val status: String,
    val message: String,
    val data: AuthData
)

data class AuthData(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String?,
    @SerializedName("expires_in") val expiresIn: Int?,
    @SerializedName("is_new_user") val isNewUser: Boolean?,
    val user: UserDto
)

data class UserDto(
    val id: Int,
    val uid: String,
    val email: String,
    val name: String?,
    val isAnonymous: Boolean,
    val role: String,
    val isVerified: Boolean
)

// PROFILE
data class UserProfileDto(
    val id: Int,
    val email: String,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("photo_url") val photoUrl: String?,
    val role: String?,
    @SerializedName("createdAt") val createdAt: String?
)

data class UserProfileResponse(
    val status: String,
    val data: UserProfileDto
)

data class UpdateProfileRequest(
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("photo_url") val photoUrl: String?
)

data class CreateScanDto(
    val userId: Int,
    val scanType: String, // "MANUAL", "REALTIME", "SCHEDULED"
    val totalFiles: Int,
    val totalMalware: Int? = null,
    val isMalware: Boolean? = null
)

data class ScanResultDto(
    val message: String,
    val data: ScanData
)

data class ScanData(
    val id: Int,
    val userId: Int,
    val scanType: String,
    val totalFiles: Int,
    val status: String,
    val startedAt: String,
    val completedAt: String?,
    val library_info: LibraryInfo
)

data class LibraryInfo(
    val matched_from_library: Int,
    val new_uploads: Int
)

data class ScanHistoryResponse(
    val message: String,
    val data: List<ScanHistoryItem>
)

data class ScanHistoryItem(
    val id: Int,
    val userId: Int,
    val scanType: String,
    val status: String,
    val totalFiles: Int,
    val totalMalware: Int,
    val isMalware: Boolean,
    val startedAt: String,
    val completedAt: String?
)

// ML MODELS
data class MlModelDto(
    val id: Int,
    val version: String,
    @SerializedName("file_path") val filePath: String,
    val checksum: String,
    @SerializedName("is_active") val isActive: Boolean
)

// SUBSCRIPTIONS
data class PlanDto(
    val id: Int,
    @SerializedName("plan_name") val planName: String,
    @SerializedName("durationDays") val durationDays: Int,
    val price: String, 
    val description: String?,
    @SerializedName("upload_file_limit") val uploadFileLimit: Int?,
    @SerializedName("full_scan_limit") val fullScanLimit: Int?,
    val model: MlModelDto? = null
)

data class SubscriptionDto(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("plan_id") val planId: Int,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("is_active") val isActive: Boolean,
    val status: String,
    val plan: PlanDto?
)

// TRANSACTIONS
data class CreateTransactionRequest(
    @SerializedName("plan_id") val planId: Int,
    val method: String
)

data class TransactionResponseDto(
    val status: String,
    val message: String,
    val data: TransactionDto
)

data class TransactionHistoryResponse(
    val status: String,
    val message: String,
    val data: List<TransactionDto>
)

data class TransactionDto(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("plan_id") val planId: Int,
    @SerializedName("external_id") val externalId: String?,
    val amount: String,
    val method: String?,
    val status: String,
    @SerializedName("snap_token") val snapToken: String? = null,
    @SerializedName("redirect_url") val redirectUrl: String? = null,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("update_at") val updatedAt: String?,
    val plan: PlanDto? = null
)
