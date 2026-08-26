package com.example.data.model

import com.google.firebase.firestore.DocumentId

// See [ProviderEntity] for why these are `var`.
data class RatingEntity(
    @DocumentId var id: String = "",
    var orderId: String = "",
    var providerId: String = "",
    var patientUid: String = "",
    var patientName: String = "",
    var stars: Int = 0,
    var chips: List<String> = emptyList(),
    var comment: String = "",
    var createdAtTimestamp: Long = System.currentTimeMillis()
)
