package com.example.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// Firestore deserializes through setters, so these are `var`; a `val` is written but always
// read back as its default. @PropertyName pins the wire name of the `is` booleans, which the
// mapper would otherwise shorten to `availableNow` / `verified`.
data class ProviderEntity(
    @DocumentId var id: String = "",
    var name: String = "",
    var title: String = "",
    var experienceYears: Int = 0,
    var rating: Double = 0.0,
    var reviewsCount: Int = 0,
    var distanceKm: Double = 0.0,
    var priceSdg: Double = 0.0,
    @get:PropertyName("isAvailableNow")
    @set:PropertyName("isAvailableNow")
    var isAvailableNow: Boolean = false,
    var serviceCategory: String = "", // "LAB_DRAW", "NURSING", "PHYSIO", "DOCTOR"
    var area: String = "",
    @get:PropertyName("isVerified")
    @set:PropertyName("isVerified")
    // Verification is something an admin grants (see ProviderRegistrationRepository.approve), so a
    // document that never got the field must not wear the badge by default.
    var isVerified: Boolean = false,
    // Never a plausible-looking number: this one gets dialled, and a stray default would ring a
    // stranger who never signed up.
    var phoneNumber: String = "",
    var ownerUid: String = "",
    var status: String = "PENDING_REVIEW", // "ACTIVE", "PENDING_REVIEW", "REJECTED"
    /** Why an admin turned the application down; empty for every other status. */
    var reviewNote: String = "",
    var ratingSum: Double = 0.0,
    var ratingCount: Int = 0,
    var about: String = "",
    var qualifications: List<String> = emptyList(),
    var completedVisits: Int = 0,
    var responseRate: Int = 0,
    // Firestore image references keyed by document type: "id", "certificate", "license".
    var docs: Map<String, String> = emptyMap()
)
