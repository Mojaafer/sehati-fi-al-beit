package com.example.data.supabase

import com.example.data.model.RatingEntity
import com.example.data.repository.RatingRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow

class SupabaseRatingRepository(
    private val api: SupabaseApiService = SupabaseClient.api,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val pollIntervalMs: Long = 5000L
) : RatingRepository {

    override fun ratingsForProvider(providerId: String): Flow<List<RatingEntity>> = pollFeed(pollIntervalMs) {
        api.getRatings(
            mapOf(
                "provider_id" to "eq.$providerId",
                "order" to "created_at_timestamp.desc",
                "limit" to "20"
            )
        ).map { it.toEntity() }
    }

    override suspend fun submitRating(rating: RatingEntity) {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("لا يمكن إرسال التقييم قبل تسجيل الدخول")
        require(rating.stars in 1..5) { "التقييم يجب أن يكون بين نجمة و5 نجوم" }
        require(rating.orderId.isNotEmpty()) { "التقييم يحتاج رقم الطلب" }

        try {
            val request = SubmitRatingRpcRequest(
                orderId = rating.orderId,
                providerId = rating.providerId,
                patientUid = uid,
                patientName = rating.patientName,
                stars = rating.stars,
                comment = rating.comment
            )

            api.submitRatingRpc(request)
        } catch (t: Throwable) {
            android.util.Log.w("Sehati", "submitRatingRpc failed for order=${rating.orderId}", t)
            throw t
        }
    }
}
