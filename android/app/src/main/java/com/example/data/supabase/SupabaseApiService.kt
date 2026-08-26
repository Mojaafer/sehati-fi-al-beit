package com.example.data.supabase

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface SupabaseApiService {

    // -------------------------------------------------------------
    // USERS
    // -------------------------------------------------------------
    @GET("users")
    suspend fun getUser(@Query("uid") filter: String): List<SupabaseUserDto>

    @Headers("Prefer: resolution=merge-duplicates")
    @POST("users")
    suspend fun upsertUser(@Body user: SupabaseUserDto): Response<ResponseBody>

    @PATCH("users")
    suspend fun updateUser(
        @Query("uid") filter: String,
        @Body updates: Map<String, Any?>
    ): Response<ResponseBody>

    // -------------------------------------------------------------
    // PROVIDERS
    // -------------------------------------------------------------
    @GET("providers")
    suspend fun getProviders(@QueryMap filters: Map<String, String>): List<SupabaseProviderDto>

    @Headers("Prefer: return=representation")
    @POST("providers")
    suspend fun insertProvider(@Body provider: SupabaseProviderDto): List<SupabaseProviderDto>

    @PATCH("providers")
    suspend fun updateProvider(
        @Query("id") filter: String,
        @Body updates: Map<String, Any?>
    ): Response<ResponseBody>

    // -------------------------------------------------------------
    // ORDERS
    // -------------------------------------------------------------
    @GET("orders")
    suspend fun getOrders(@QueryMap filters: Map<String, String>): List<SupabaseOrderDto>

    @Headers("Prefer: return=representation")
    @POST("orders")
    suspend fun insertOrder(@Body order: SupabaseOrderDto): List<SupabaseOrderDto>

    @PATCH("orders")
    suspend fun updateOrder(
        @Query("id") filter: String,
        @Body updates: Map<String, Any?>
    ): Response<ResponseBody>

    @POST("rpc/transition_order")
    suspend fun transitionOrder(@Body request: TransitionOrderRpcRequest): SupabaseOrderDto

    // -------------------------------------------------------------
    // PAYOUTS
    // -------------------------------------------------------------
    @GET("payouts")
    suspend fun getPayouts(@QueryMap filters: Map<String, String>): List<SupabasePayoutDto>

    @Headers("Prefer: resolution=merge-duplicates")
    @POST("payouts")
    suspend fun insertPayout(@Body payout: SupabasePayoutDto): Response<ResponseBody>

    @PATCH("payouts")
    suspend fun updatePayout(
        @Query("id") filter: String,
        @Body updates: Map<String, Any?>
    ): Response<ResponseBody>

    // -------------------------------------------------------------
    // RATINGS
    // -------------------------------------------------------------
    @GET("ratings")
    suspend fun getRatings(@QueryMap filters: Map<String, String>): List<SupabaseRatingDto>

    @POST("rpc/submit_provider_rating")
    suspend fun submitRatingRpc(@Body request: SubmitRatingRpcRequest): Response<ResponseBody>

    // -------------------------------------------------------------
    // NOTIFICATIONS
    // -------------------------------------------------------------
    @GET("notifications")
    suspend fun getNotifications(@QueryMap filters: Map<String, String>): List<SupabaseNotificationDto>

    @POST("notifications")
    suspend fun insertNotification(@Body notification: SupabaseNotificationDto): Response<ResponseBody>

    @PATCH("notifications")
    suspend fun updateNotification(
        @Query("id") filter: String,
        @Body updates: Map<String, Any?>
    ): Response<ResponseBody>

    @PATCH("notifications")
    suspend fun updateNotificationsByRecipient(
        @Query("recipient_uid") filter: String,
        @Body updates: Map<String, Any?>
    ): Response<ResponseBody>

    @DELETE("notifications")
    suspend fun deleteNotification(@Query("id") filter: String): Response<ResponseBody>

    // -------------------------------------------------------------
    // ADMIN NOTIFICATIONS
    // -------------------------------------------------------------
    @GET("admin_notifications")
    suspend fun getAdminNotifications(@QueryMap filters: Map<String, String>): List<SupabaseNotificationDto>

    @POST("admin_notifications")
    suspend fun insertAdminNotification(@Body notification: SupabaseNotificationDto): Response<ResponseBody>

    @PATCH("admin_notifications")
    suspend fun updateAdminNotification(
        @Query("id") filter: String,
        @Body updates: Map<String, Any?>
    ): Response<ResponseBody>

    @DELETE("admin_notifications")
    suspend fun deleteAdminNotification(@Query("id") filter: String): Response<ResponseBody>

    // -------------------------------------------------------------
    // IMAGES
    // -------------------------------------------------------------
    @GET("images")
    suspend fun getImage(@Query("id") filter: String): List<SupabaseImageDto>

    @Headers("Prefer: return=representation")
    @POST("images")
    suspend fun insertImage(@Body image: SupabaseImageDto): List<SupabaseImageDto>
}
