package com.example.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// See [ProviderEntity] for why these are `var` and why the `is` boolean is pinned.
//
// Every default here is empty on purpose. Firestore needs a no-arg constructor, so these values are
// what a document missing a field deserialises to — and they used to be a plausible-looking patient
// name, address and bank reference. An order that lost a field would then show an admin a transfer
// reference nobody sent, or name a patient who never booked.
data class OrderEntity(
    @DocumentId var id: String = "",
    var orderNumber: String = "",
    var serviceTitle: String = "",
    var serviceDetails: String = "",
    var patientName: String = "",
    var patientPhone: String = "",
    var providerName: String = "",
    var providerTitle: String = "",
    var providerPhone: String = "",
    var areaLocation: String = "",
    var visitDate: String = "",
    var visitTime: String = "",
    var notes: String = "",
    var priceSdg: Double = 0.0,
    // The money terms are settled once at booking and never restated (see the rules'
    // termsUnchanged). All three default to zero: a document that predates the money layer or
    // lost a field must show "derive from price" — never an invented amount. Derivation lives
    // in [OrderFees.effectivePayableAmountSdg] / [OrderFees.effectiveProviderPayoutSdg].
    var payableAmountSdg: Double = 0.0,
    var providerPayoutSdg: Double = 0.0,
    var commissionSdg: Double = 0.0,
    var paymentMethod: String = "",
    var status: String = "ORDER_SENT", // see [OrderStatus]
    var cancelledBy: String = "",
    var cancelReason: String = "",
    var transferSenderName: String = "",
    var transferRefNum: String = "",
    var receiptImageUri: String? = null,
    var patientUid: String = "",
    var providerId: String = "",
    @get:PropertyName("isRated")
    @set:PropertyName("isRated")
    var isRated: Boolean = false,
    var createdAtTimestamp: Long = System.currentTimeMillis()
)
