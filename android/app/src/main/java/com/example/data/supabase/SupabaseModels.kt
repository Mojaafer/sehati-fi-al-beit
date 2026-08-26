package com.example.data.supabase

import com.example.data.model.NotificationEntity
import com.example.data.model.OrderEntity
import com.example.data.model.PayoutEntity
import com.example.data.model.PayoutStatus
import com.example.data.model.ProviderEntity
import com.example.data.model.RatingEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseUserDto(
    @Json(name = "uid") val uid: String,
    @Json(name = "role") val role: String = "PATIENT",
    @Json(name = "phone_number") val phoneNumber: String = "",
    @Json(name = "display_name") val displayName: String = "",
    @Json(name = "address") val address: String = "",
    @Json(name = "fcm_token") val fcmToken: String = "",
    @Json(name = "status") val status: String = "ACTIVE",
    @Json(name = "provider_id") val providerId: String = "",
    @Json(name = "created_at_timestamp") val createdAtTimestamp: Long = System.currentTimeMillis(),
    @Json(name = "updated_at_timestamp") val updatedAtTimestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class SupabaseProviderDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "owner_uid") val ownerUid: String = "",
    @Json(name = "full_name") val fullName: String = "",
    @Json(name = "phone") val phone: String = "",
    @Json(name = "category") val category: String = "",
    @Json(name = "specialization") val specialization: String = "",
    @Json(name = "city") val city: String = "",
    @Json(name = "neighborhood") val neighborhood: String = "",
    @Json(name = "bio") val bio: String = "",
    @Json(name = "years_of_experience") val yearsOfExperience: Int = 0,
    @Json(name = "price") val price: Double = 0.0,
    @Json(name = "is_available") val isAvailable: Boolean = true,
    @Json(name = "status") val status: String = "PENDING_REVIEW",
    @Json(name = "rating_sum") val ratingSum: Double = 0.0,
    @Json(name = "rating_count") val ratingCount: Int = 0,
    @Json(name = "rating") val rating: Double = 0.0,
    @Json(name = "reviews_count") val reviewsCount: Int = 0,
    @Json(name = "rejection_reason") val rejectionReason: String = "",
    @Json(name = "document_images") val documentImages: Map<String, String> = emptyMap(),
    @Json(name = "profile_image_url") val profileImageUrl: String = "",
    @Json(name = "created_at_timestamp") val createdAtTimestamp: Long = System.currentTimeMillis()
) {
    fun toEntity(): ProviderEntity = ProviderEntity(
        id = id,
        name = fullName,
        title = specialization,
        experienceYears = yearsOfExperience,
        rating = rating,
        reviewsCount = reviewsCount,
        priceSdg = price,
        isAvailableNow = isAvailable,
        serviceCategory = category,
        area = if (neighborhood.isNotEmpty()) "$city, $neighborhood" else city,
        isVerified = status == "ACTIVE",
        phoneNumber = phone,
        ownerUid = ownerUid,
        status = status,
        reviewNote = rejectionReason,
        ratingSum = ratingSum,
        ratingCount = ratingCount,
        about = bio,
        docs = documentImages
    )

    companion object {
        fun fromEntity(e: ProviderEntity): SupabaseProviderDto = SupabaseProviderDto(
            id = e.id,
            ownerUid = e.ownerUid,
            fullName = e.name,
            phone = e.phoneNumber,
            category = e.serviceCategory,
            specialization = e.title,
            city = e.area.split(",").firstOrNull()?.trim() ?: e.area,
            neighborhood = e.area.split(",").getOrNull(1)?.trim() ?: "",
            bio = e.about,
            yearsOfExperience = e.experienceYears,
            price = e.priceSdg,
            isAvailable = e.isAvailableNow,
            status = e.status,
            ratingSum = e.ratingSum,
            ratingCount = e.ratingCount,
            rating = e.rating,
            reviewsCount = e.reviewsCount,
            rejectionReason = e.reviewNote,
            documentImages = e.docs
        )
    }
}

@JsonClass(generateAdapter = true)
data class SupabaseOrderDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "order_number") val orderNumber: String = "",
    @Json(name = "patient_uid") val patientUid: String = "",
    @Json(name = "patient_name") val patientName: String = "",
    @Json(name = "patient_phone") val patientPhone: String = "",
    @Json(name = "provider_id") val providerId: String = "",
    @Json(name = "provider_name") val providerName: String = "",
    @Json(name = "provider_phone") val providerPhone: String = "",
    @Json(name = "category") val category: String = "",
    @Json(name = "location") val location: String = "",
    @Json(name = "notes") val notes: String = "",
    @Json(name = "status") val status: String = "ORDER_SENT",
    @Json(name = "payable_amount_sdg") val payableAmountSdg: Double = 0.0,
    @Json(name = "base_price_sdg") val basePriceSdg: Double = 0.0,
    @Json(name = "commission_sdg") val commissionSdg: Double = 0.0,
    @Json(name = "provider_payout_sdg") val providerPayoutSdg: Double = 0.0,
    @Json(name = "random_fee_offset_sdg") val randomFeeOffsetSdg: Double = 0.0,
    @Json(name = "price_sdg") val priceSdg: Double = 0.0,
    @Json(name = "scheduled_time") val scheduledTime: String = "",
    @Json(name = "receipt_image_uri") val receiptImageUri: String? = null,
    @Json(name = "receipt_url") val receiptUrl: String = "",
    @Json(name = "cancelled_by") val cancelledBy: String = "",
    @Json(name = "cancellation_reason") val cancellationReason: String = "",
    @Json(name = "refund_status") val refundStatus: String = "",
    @Json(name = "refund_reason") val refundReason: String = "",
    @Json(name = "is_rated") val isRated: Boolean = false,
    @Json(name = "created_at_timestamp") val createdAtTimestamp: Long = System.currentTimeMillis(),
    @Json(name = "updated_at_timestamp") val updatedAtTimestamp: Long = System.currentTimeMillis()
) {
    fun toEntity(): OrderEntity = OrderEntity(
        id = id,
        orderNumber = orderNumber,
        serviceTitle = category,
        serviceDetails = notes,
        patientName = patientName,
        patientPhone = patientPhone,
        providerName = providerName,
        providerPhone = providerPhone,
        areaLocation = location,
        visitTime = scheduledTime,
        notes = notes,
        priceSdg = if (priceSdg > 0) priceSdg else basePriceSdg,
        payableAmountSdg = payableAmountSdg,
        providerPayoutSdg = providerPayoutSdg,
        commissionSdg = commissionSdg,
        status = status,
        cancelledBy = cancelledBy,
        cancelReason = cancellationReason,
        receiptImageUri = receiptImageUri ?: receiptUrl.takeIf { it.isNotEmpty() },
        patientUid = patientUid,
        providerId = providerId,
        isRated = isRated,
        createdAtTimestamp = createdAtTimestamp
    )

    companion object {
        fun fromEntity(e: OrderEntity): SupabaseOrderDto = SupabaseOrderDto(
            id = e.id,
            orderNumber = e.orderNumber,
            patientUid = e.patientUid,
            patientName = e.patientName,
            patientPhone = e.patientPhone,
            providerId = e.providerId,
            providerName = e.providerName,
            providerPhone = e.providerPhone,
            category = e.serviceTitle,
            location = e.areaLocation,
            notes = e.notes,
            status = e.status,
            payableAmountSdg = e.payableAmountSdg,
            basePriceSdg = e.priceSdg,
            commissionSdg = e.commissionSdg,
            providerPayoutSdg = e.providerPayoutSdg,
            priceSdg = e.priceSdg,
            scheduledTime = "${e.visitDate} ${e.visitTime}".trim(),
            receiptImageUri = e.receiptImageUri,
            receiptUrl = e.receiptImageUri.orEmpty(),
            cancelledBy = e.cancelledBy,
            cancellationReason = e.cancelReason,
            isRated = e.isRated,
            createdAtTimestamp = e.createdAtTimestamp,
            updatedAtTimestamp = System.currentTimeMillis()
        )
    }
}

@JsonClass(generateAdapter = true)
data class SupabasePayoutDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "order_id") val orderId: String = "",
    @Json(name = "provider_id") val providerId: String = "",
    @Json(name = "order_number") val orderNumber: String = "",
    @Json(name = "provider_payout_sdg") val providerPayoutSdg: Double = 0.0,
    @Json(name = "amount_sdg") val amountSdg: Double = 0.0,
    @Json(name = "status") val status: String = PayoutStatus.ACCRUED,
    @Json(name = "paid_at_timestamp") val paidAtTimestamp: Long = 0L,
    @Json(name = "created_at_timestamp") val createdAtTimestamp: Long = System.currentTimeMillis()
) {
    fun toEntity(): PayoutEntity = PayoutEntity(
        id = id,
        orderId = orderId,
        orderNumber = orderNumber,
        providerId = providerId,
        amountSdg = if (amountSdg > 0) amountSdg else providerPayoutSdg,
        status = status,
        createdAtTimestamp = createdAtTimestamp,
        paidAtTimestamp = paidAtTimestamp
    )

    companion object {
        fun fromEntity(e: PayoutEntity): SupabasePayoutDto = SupabasePayoutDto(
            id = e.id.ifEmpty { e.orderId },
            orderId = e.orderId,
            providerId = e.providerId,
            orderNumber = e.orderNumber,
            providerPayoutSdg = e.amountSdg,
            amountSdg = e.amountSdg,
            status = e.status,
            paidAtTimestamp = e.paidAtTimestamp,
            createdAtTimestamp = if (e.createdAtTimestamp > 0) e.createdAtTimestamp else System.currentTimeMillis()
        )
    }
}

@JsonClass(generateAdapter = true)
data class SupabaseRatingDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "order_id") val orderId: String = "",
    @Json(name = "provider_id") val providerId: String = "",
    @Json(name = "patient_uid") val patientUid: String = "",
    @Json(name = "patient_name") val patientName: String = "",
    @Json(name = "stars") val stars: Int = 5,
    @Json(name = "rating") val rating: Int = 5,
    @Json(name = "comment") val comment: String = "",
    @Json(name = "created_at_timestamp") val createdAtTimestamp: Long = System.currentTimeMillis()
) {
    fun toEntity(): RatingEntity = RatingEntity(
        id = id,
        orderId = orderId,
        providerId = providerId,
        patientUid = patientUid,
        patientName = patientName,
        stars = stars,
        comment = comment,
        createdAtTimestamp = createdAtTimestamp
    )

    companion object {
        fun fromEntity(e: RatingEntity): SupabaseRatingDto = SupabaseRatingDto(
            id = e.id.ifEmpty { e.orderId },
            orderId = e.orderId,
            providerId = e.providerId,
            patientUid = e.patientUid,
            patientName = e.patientName,
            stars = e.stars,
            rating = e.stars,
            comment = e.comment,
            createdAtTimestamp = e.createdAtTimestamp
        )
    }
}

@JsonClass(generateAdapter = true)
data class SupabaseNotificationDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "recipient_uid") val recipientUid: String? = null,
    @Json(name = "title") val title: String = "",
    @Json(name = "message") val message: String = "",
    @Json(name = "read") val read: Boolean = false,
    @Json(name = "order_id") val orderId: String = "",
    @Json(name = "type") val type: String = "SYSTEM",
    @Json(name = "created_at_timestamp") val createdAtTimestamp: Long = System.currentTimeMillis()
) {
    fun toEntity(scope: String = NotificationEntity.SCOPE_USER): NotificationEntity = NotificationEntity(
        id = id,
        type = type,
        title = title,
        body = message,
        orderId = orderId,
        scope = scope,
        read = read,
        createdAtTimestamp = createdAtTimestamp
    )

    companion object {
        fun fromEntity(e: NotificationEntity, recipientUid: String? = null): SupabaseNotificationDto = SupabaseNotificationDto(
            id = e.id,
            recipientUid = recipientUid,
            title = e.title,
            message = e.body,
            read = e.read,
            orderId = e.orderId,
            type = e.type,
            createdAtTimestamp = e.createdAtTimestamp
        )
    }
}

@JsonClass(generateAdapter = true)
data class SupabaseImageDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "owner_uid") val ownerUid: String = "",
    @Json(name = "data") val data: String = "",
    @Json(name = "created_at_timestamp") val createdAtTimestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class SubmitRatingRpcRequest(
    @Json(name = "p_order_id") val orderId: String,
    @Json(name = "p_provider_id") val providerId: String,
    @Json(name = "p_patient_uid") val patientUid: String,
    @Json(name = "p_patient_name") val patientName: String,
    @Json(name = "p_stars") val stars: Int,
    @Json(name = "p_comment") val comment: String
)

data class TransitionOrderRpcRequest(
    @Json(name = "p_order_id") val orderId: String,
    @Json(name = "p_target_status") val targetStatus: String,
    @Json(name = "p_cancelled_by") val cancelledBy: String = "",
    @Json(name = "p_reason") val reason: String = "",
    @Json(name = "p_receipt_image_uri") val receiptImageUri: String = ""
)
