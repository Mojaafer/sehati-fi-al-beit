package com.example.data.repository

import com.example.data.model.RatingEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface RatingRepository {
    fun ratingsForProvider(providerId: String): Flow<List<RatingEntity>>
    suspend fun submitRating(rating: RatingEntity)
}

/** A provider's running review totals plus the average the UI reads. */
data class RatingAggregate(
    val ratingSum: Double,
    val ratingCount: Long,
    val average: Double
)

/**
 * Folds one review into a provider's totals. Kept separate from the Firestore transaction
 * so the averaging is verifiable without a live backend.
 */
fun foldRating(currentSum: Double, currentCount: Long, stars: Int): RatingAggregate {
    val newSum = currentSum + stars
    val newCount = currentCount + 1
    return RatingAggregate(newSum, newCount, newSum / newCount)
}

class FirestoreRatingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : RatingRepository {

    override fun ratingsForProvider(providerId: String): Flow<List<RatingEntity>> = callbackFlow {
        val listener = firestore.collection("ratings")
            .whereEqualTo("providerId", providerId)
            .orderBy("createdAtTimestamp", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Closing with the error would rethrow it in the collector's scope and take
                    // down the process; a rejected listen just leaves the review list empty.
                    close()
                    return@addSnapshotListener
                }
                trySend(
                    snapshot?.documents?.mapNotNull {
                        it.toObject(RatingEntity::class.java)
                    } ?: emptyList()
                )
            }
        awaitClose { listener.remove() }
    }

    /**
     * Writes the review, folds it into the provider's running average and flags the order
     * as rated — atomically, so a retry can't double-count. The rating document id is the
     * order id, which is what makes a second submission fail against the rules.
     */
    override suspend fun submitRating(rating: RatingEntity) {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("لا يمكن إرسال التقييم قبل تسجيل الدخول")
        require(rating.stars in 1..5) { "التقييم يجب أن يكون بين نجمة و5 نجوم" }
        require(rating.orderId.isNotEmpty()) { "التقييم يحتاج رقم الطلب" }

        val ratingRef = firestore.collection("ratings").document(rating.orderId)
        val providerRef = firestore.collection("providers").document(rating.providerId)
        val orderRef = firestore.collection("orders").document(rating.orderId)

        firestore.runTransaction { transaction ->
            val providerSnapshot = transaction.get(providerRef)

            transaction.set(ratingRef, rating.copy(id = rating.orderId, patientUid = uid))

            if (providerSnapshot.exists()) {
                val aggregate = foldRating(
                    currentSum = providerSnapshot.getDouble("ratingSum") ?: 0.0,
                    currentCount = providerSnapshot.getLong("ratingCount") ?: 0L,
                    stars = rating.stars
                )
                transaction.update(
                    providerRef,
                    mapOf(
                        "ratingSum" to aggregate.ratingSum,
                        "ratingCount" to aggregate.ratingCount,
                        "rating" to aggregate.average,
                        "reviewsCount" to aggregate.ratingCount
                    )
                )
            }

            transaction.update(orderRef, "isRated", true)
        }.await()
    }
}
