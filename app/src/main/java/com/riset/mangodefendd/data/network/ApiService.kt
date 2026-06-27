package com.riset.mangodefendd.data.network

import com.riset.mangodefendd.data.network.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/firebase-login")
    suspend fun firebaseLogin(@Body request: FirebaseLoginDto): Response<AuthResponse>

    @Multipart
    @POST("scans")
    suspend fun createScan(
        @Part files: List<MultipartBody.Part>,
        @Part("userId") userId: RequestBody,
        @Part("scanType") scanType: RequestBody,
        @Part("totalFiles") totalFiles: RequestBody,
        @Part("totalMalware") totalMalware: RequestBody?,
        @Part("isMalware") isMalware: RequestBody?
    ): Response<ScanResultDto>

    @GET("scans/history/{userId}")
    suspend fun getScanHistory(@Path("userId") userId: Int): Response<ScanHistoryResponse>

    // SUBSCRIPTIONS
    @GET("subscriptions/plan")
    suspend fun getAllPlans(): Response<List<PlanDto>>

    @GET("subscriptions/active/{userId}")
    suspend fun getActiveSubscription(@Path("userId") userId: Int): Response<List<SubscriptionDto>>

    // TRANSACTIONS
    @POST("transactions/checkout")
    suspend fun checkout(@Body request: CreateTransactionRequest): Response<TransactionResponseDto>

    @POST("transactions/webhook/success/{id}")
    suspend fun simulatePaymentSuccess(@Path("id") transactionId: Int): Response<TransactionResponseDto>

    @GET("transactions/history/{userId}")
    suspend fun getTransactionHistory(@Path("userId") userId: Int): Response<TransactionHistoryResponse>

    // PROFILE
    @GET("users/{id}")
    suspend fun getProfile(@Path("id") userId: Int): Response<UserProfileResponse>

    @PATCH("users/{id}")
    suspend fun updateProfile(
        @Path("id") userId: Int,
        @Body request: UpdateProfileRequest
    ): Response<UserProfileResponse>
}
