package com.example.data.model

import com.google.firebase.firestore.DocumentId

// See [ProviderEntity] for why these are `var`.
data class NotificationEntity(
    @DocumentId var id: String = "",
    var type: String = "SYSTEM", // "ORDER", "PAYMENT", "PROVIDER", "SYSTEM"
    var title: String = "",
    var body: String = "",
    var orderId: String = "",
    var providerId: String = "",
    /**
     * Which inbox this document lives in: [SCOPE_USER] for `users/{uid}/notifications`,
     * [SCOPE_ADMIN] for the shared `adminNotifications` collection. Persisted rather than
     * inferred so that marking one read can find its way back to the right collection after
     * the two feeds have been merged into a single list.
     */
    var scope: String = SCOPE_USER,
    var read: Boolean = false,
    var createdAtTimestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val SCOPE_USER = "USER"
        const val SCOPE_ADMIN = "ADMIN"
    }
}

/**
 * The inbox categories. Stored on the wire as plain strings ([NotificationEntity.type]),
 * so this enum only exists to stop call sites hand-typing the literals — adding a new
 * category means adding it here, not editing every builder.
 */
enum class NotificationType(val wireValue: String) {
    ORDER("ORDER"),
    PAYMENT("PAYMENT"),
    PROVIDER("PROVIDER"),
    SYSTEM("SYSTEM")
}
