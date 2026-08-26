package com.example.data.supabase

import com.example.data.model.ProviderEntity
import com.example.data.repository.ProviderRegistrationRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

class SupabaseProviderRegistrationRepository(
    private val api: SupabaseApiService = SupabaseClient.api,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ProviderRegistrationRepository {

    override suspend fun registerProvider(provider: ProviderEntity): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("لا يمكن التسجيل كمقدم خدمة قبل تسجيل الدخول")

        val providerId = provider.id.ifEmpty { UUID.randomUUID().toString() }
        val providerDto = SupabaseProviderDto.fromEntity(
            provider.copy(
                id = providerId,
                ownerUid = uid,
                status = "PENDING_REVIEW",
                isVerified = false
            )
        )

        api.insertProvider(providerDto)

        try {
            api.updateUser(
                filter = "eq.$uid",
                updates = mapOf("provider_id" to providerId)
            )
        } catch (_: Exception) {
        }

        return providerId
    }

    override suspend fun attachDocuments(providerId: String, docUrls: Map<String, String>) {
        if (providerId.isEmpty() || docUrls.isEmpty()) return
        try {
            api.updateProvider(
                filter = "eq.$providerId",
                updates = mapOf("document_images" to docUrls)
            )
        } catch (_: Exception) {
        }
    }

    override suspend fun myProviderApplication(): ProviderEntity? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val list = api.getProviders(
                mapOf("owner_uid" to "eq.$uid", "limit" to "1")
            )
            list.firstOrNull()?.toEntity()
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun pendingApplications(): List<ProviderEntity> {
        return try {
            api.getProviders(
                mapOf("status" to "eq.PENDING_REVIEW")
            ).map { it.toEntity() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun approveProvider(providerId: String): ProviderEntity? {
        val provider = try {
            api.getProviders(mapOf("id" to "eq.$providerId", "limit" to "1"))
                .firstOrNull()?.toEntity() ?: return null
        } catch (_: Exception) {
            return null
        }

        try {
            api.updateProvider(
                filter = "eq.$providerId",
                updates = mapOf(
                    "status" to "ACTIVE",
                    "rejection_reason" to ""
                )
            )

            if (provider.ownerUid.isNotEmpty()) {
                api.updateUser(
                    filter = "eq.${provider.ownerUid}",
                    updates = mapOf("role" to "PROVIDER")
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("SehatiProviderReg", "approveProvider failed for $providerId", e)
        }

        return provider.copy(status = "ACTIVE", isVerified = true)
    }

    override suspend fun rejectProvider(providerId: String, reason: String): ProviderEntity? {
        val provider = try {
            api.getProviders(mapOf("id" to "eq.$providerId", "limit" to "1"))
                .firstOrNull()?.toEntity() ?: return null
        } catch (_: Exception) {
            return null
        }

        try {
            api.updateProvider(
                filter = "eq.$providerId",
                updates = mapOf(
                    "status" to "REJECTED",
                    "rejection_reason" to reason
                )
            )
        } catch (e: Exception) {
            android.util.Log.w("SehatiProviderReg", "rejectProvider failed for $providerId", e)
        }

        return provider.copy(status = "REJECTED", reviewNote = reason)
    }
}
