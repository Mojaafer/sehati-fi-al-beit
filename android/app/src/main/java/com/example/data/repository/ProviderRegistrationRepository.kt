package com.example.data.repository

import com.example.data.model.ProviderEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface ProviderRegistrationRepository {
    /** Returns the new provider document id. Created in PENDING_REVIEW; an admin activates it. */
    suspend fun registerProvider(provider: ProviderEntity): String
    suspend fun attachDocuments(providerId: String, docUrls: Map<String, String>)
    suspend fun myProviderApplication(): ProviderEntity?
    suspend fun pendingApplications(): List<ProviderEntity>

    /** Returns the application as it stood before approval, so the caller can notify its owner. */
    suspend fun approveProvider(providerId: String): ProviderEntity?

    /**
     * Turns the application down, recording [reason] on the document. The owner keeps the
     * PATIENT role they never left, and `users/{uid}.providerId` is deliberately left in place
     * so the applicant can still open their application and read why it was refused.
     */
    suspend fun rejectProvider(providerId: String, reason: String): ProviderEntity?
}

class FirestoreProviderRegistrationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ProviderRegistrationRepository {

    override suspend fun registerProvider(provider: ProviderEntity): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("لا يمكن التسجيل كمقدم خدمة قبل تسجيل الدخول")

        val docRef = firestore.collection("providers").document()
        docRef.set(
            provider.copy(
                id = docRef.id,
                ownerUid = uid,
                status = "PENDING_REVIEW",
                isVerified = false
            )
        ).await()

        // The rules block a client from setting its own role, so the link lives on the
        // user doc as providerId; an admin flips role to PROVIDER when approving.
        firestore.collection("users").document(uid)
            .update("providerId", docRef.id).await()

        return docRef.id
    }

    override suspend fun attachDocuments(providerId: String, docUrls: Map<String, String>) {
        if (providerId.isEmpty() || docUrls.isEmpty()) return
        firestore.collection("providers").document(providerId)
            .update(docUrls.mapKeys { "docs.${it.key}" }).await()
    }

    override suspend fun myProviderApplication(): ProviderEntity? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            firestore.collection("providers")
                .whereEqualTo("ownerUid", uid)
                .limit(1)
                .get().await()
                .documents.firstOrNull()
                ?.toObject(ProviderEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun pendingApplications(): List<ProviderEntity> {
        return try {
            firestore.collection("providers")
                .whereEqualTo("status", "PENDING_REVIEW")
                .get().await()
                .documents.mapNotNull { it.toObject(ProviderEntity::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun approveProvider(providerId: String): ProviderEntity? {
        val providerRef = firestore.collection("providers").document(providerId)
        val provider = providerRef.get().await().toObject(ProviderEntity::class.java) ?: return null

        providerRef.update(
            mapOf("status" to "ACTIVE", "isVerified" to true, "reviewNote" to "")
        ).await()

        if (provider.ownerUid.isNotEmpty()) {
            firestore.collection("users").document(provider.ownerUid)
                .update("role", "PROVIDER").await()
        }
        return provider
    }

    override suspend fun rejectProvider(providerId: String, reason: String): ProviderEntity? {
        val providerRef = firestore.collection("providers").document(providerId)
        val provider = providerRef.get().await().toObject(ProviderEntity::class.java) ?: return null

        providerRef.update(
            mapOf("status" to "REJECTED", "isVerified" to false, "reviewNote" to reason)
        ).await()

        return provider
    }
}
