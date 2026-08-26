package com.example.data.model

import com.google.firebase.firestore.DocumentId

/** Lifecycle of a provider payout entry. */
object PayoutStatus {
    /** The visit is done and the amount is owed to the provider. */
    const val ACCRUED = "ACCRUED"

    /** The admin transferred the money via Bankak and recorded it. */
    const val PAID = "PAID"
}

// Same conventions as [OrderEntity]: `var` for Firestore's no-arg deserialization and every
// default empty/zero, so a document that lost a field never invents an amount or a person.
data class PayoutEntity(
    // Same id as the order that earned it — a second accrual attempt becomes an update,
    // which the rules deny, so one visit can never be paid out twice.
    @DocumentId var id: String = "",
    var orderId: String = "",
    var orderNumber: String = "",
    var providerId: String = "",
    var providerName: String = "",
    var patientName: String = "",
    var amountSdg: Double = 0.0,
    var status: String = PayoutStatus.ACCRUED,
    var createdAtTimestamp: Long = 0L,
    var paidAtTimestamp: Long = 0L
)
