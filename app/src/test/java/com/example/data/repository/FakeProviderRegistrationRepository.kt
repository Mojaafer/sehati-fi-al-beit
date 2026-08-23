package com.example.data.repository

import com.example.data.model.ProviderEntity

/** In-memory [ProviderRegistrationRepository] for unit tests. */
class FakeProviderRegistrationRepository(
    initial: List<ProviderEntity> = emptyList()
) : ProviderRegistrationRepository {

    private val providers = initial.associateBy { it.id }.toMutableMap()

    /** Stands in for the signed-in applicant, whose uid lands on `ownerUid`. */
    var signedInUid: String = "applicant-uid"

    /** Set to make the next write throw, standing in for a refused or offline request. */
    var failNextWrite: Boolean = false

    /** The roles the repository granted, so a test can assert an approval promoted the owner. */
    val grantedRoles = mutableMapOf<String, String>()

    fun providerById(id: String): ProviderEntity? = providers[id]

    override suspend fun registerProvider(provider: ProviderEntity): String {
        if (failNextWrite) throw IllegalStateException("write refused")
        val id = "provider-${providers.size + 1}"
        providers[id] = provider.copy(
            id = id,
            ownerUid = signedInUid,
            status = "PENDING_REVIEW",
            isVerified = false
        )
        return id
    }

    override suspend fun attachDocuments(providerId: String, docUrls: Map<String, String>) {
        val existing = providers[providerId] ?: return
        providers[providerId] = existing.copy(docs = existing.docs + docUrls)
    }

    override suspend fun myProviderApplication(): ProviderEntity? =
        providers.values.firstOrNull { it.ownerUid == signedInUid }

    override suspend fun pendingApplications(): List<ProviderEntity> =
        providers.values.filter { it.status == "PENDING_REVIEW" }

    override suspend fun approveProvider(providerId: String): ProviderEntity? {
        if (failNextWrite) throw IllegalStateException("write refused")
        val provider = providers[providerId] ?: return null
        providers[providerId] = provider.copy(status = "ACTIVE", isVerified = true, reviewNote = "")
        grantedRoles[provider.ownerUid] = "PROVIDER"
        return provider
    }

    override suspend fun rejectProvider(providerId: String, reason: String): ProviderEntity? {
        if (failNextWrite) throw IllegalStateException("write refused")
        val provider = providers[providerId] ?: return null
        providers[providerId] =
            provider.copy(status = "REJECTED", isVerified = false, reviewNote = reason)
        return provider
    }
}
