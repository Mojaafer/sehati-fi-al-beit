package com.example.data.supabase

import android.content.Context
import com.example.BuildConfig
import com.example.data.repository.FirestoreNotificationRepository
import com.example.data.repository.FirestorePayoutRepository
import com.example.data.repository.FirestoreProviderRegistrationRepository
import com.example.data.repository.FirestoreRatingRepository
import com.example.data.repository.FirestoreSehatiRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.PayoutRepository
import com.example.data.repository.ProviderRegistrationRepository
import com.example.data.repository.RatingRepository
import com.example.data.repository.SehatiRepository
import com.example.data.storage.FirestoreImageRepository
import com.example.data.storage.StorageRepository

/**
 * Factory for creating repository instances (Supabase backend).
 */
object RepositoryFactory {

    private val useSupabase: Boolean
        get() = BuildConfig.USE_SUPABASE

    fun createSehatiRepository(): SehatiRepository =
        if (useSupabase) SupabaseSehatiRepository() else FirestoreSehatiRepository()

    fun createNotificationRepository(): NotificationRepository =
        if (useSupabase) SupabaseNotificationRepository() else FirestoreNotificationRepository()

    fun createPayoutRepository(): PayoutRepository =
        if (useSupabase) SupabasePayoutRepository() else FirestorePayoutRepository()

    fun createProviderRegistrationRepository(): ProviderRegistrationRepository =
        if (useSupabase) {
            SupabaseProviderRegistrationRepository()
        } else {
            FirestoreProviderRegistrationRepository()
        }

    fun createRatingRepository(): RatingRepository =
        if (useSupabase) SupabaseRatingRepository() else FirestoreRatingRepository()

    fun createStorageRepository(context: Context? = null): StorageRepository {
        if (!useSupabase) return FirestoreImageRepository()
        val appContext = context ?: com.google.firebase.FirebaseApp.getInstance().applicationContext
        return SupabaseStorageRepository(appContext)
    }

    fun createUserRepository(): SupabaseUserRepository =
        SupabaseUserRepository()
}
