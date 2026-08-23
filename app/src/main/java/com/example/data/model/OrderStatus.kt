package com.example.data.model

/**
 * The order lifecycle. Firestore stores the raw string, but the rules about who may still change
 * an order live here so a screen never has to re-derive them from a status comparison.
 */
object OrderStatus {
    const val ORDER_SENT = "ORDER_SENT"
    const val ACCEPTED_BY_PROVIDER = "ACCEPTED_BY_PROVIDER"
    const val PAYMENT_PENDING = "PAYMENT_PENDING"
    const val PAYMENT_UNDER_REVIEW = "PAYMENT_UNDER_REVIEW"
    const val PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED"

    /**
     * Money is held but the patient asked for it back before the visit. An admin resolves it:
     * granting lands the order in CANCELLED (with the money returned via Bankak outside the
     * app), declining returns it to PAYMENT_CONFIRMED. The visit itself never proceeds while
     * a refund is pending.
     */
    const val REFUND_REQUESTED = "REFUND_REQUESTED"

    const val COMPLETED = "COMPLETED"
    const val REJECTED = "REJECTED"
    const val CANCELLED = "CANCELLED"

    const val BY_PATIENT = "PATIENT"
    const val BY_PROVIDER = "PROVIDER"
    const val BY_ADMIN = "ADMIN"

    /**
     * A patient may back out until their money is confirmed. Past that point a refund is owed,
     * which is a conversation with the admin rather than a status the app flips on its own.
     */
    fun canPatientCancel(status: String): Boolean = status in setOf(
        ORDER_SENT,
        ACCEPTED_BY_PROVIDER,
        PAYMENT_PENDING,
        PAYMENT_UNDER_REVIEW,
        REJECTED
    )

    /** A provider can turn work down only while they have not yet committed to it. */
    fun canProviderDecline(status: String): Boolean = status == ORDER_SENT

    fun canPatientPay(status: String): Boolean = status in setOf(
        ACCEPTED_BY_PROVIDER,
        PAYMENT_PENDING,
        REJECTED
    )

    fun canProviderComplete(status: String): Boolean = status == PAYMENT_CONFIRMED

    /** Once a refund is asked for, resolution belongs to the admin, not to another tap. */
    fun canPatientRequestRefund(status: String): Boolean = status == PAYMENT_CONFIRMED

    fun isTerminal(status: String): Boolean = status == COMPLETED || status == CANCELLED

    /** Arabic label shown on the order card and in the timeline. */
    fun label(status: String): String = when (status) {
        ORDER_SENT -> "بانتظار قبول مقدم الخدمة"
        ACCEPTED_BY_PROVIDER -> "تم قبول الطلب"
        PAYMENT_PENDING -> "بانتظار الدفع"
        PAYMENT_UNDER_REVIEW -> "الدفع قيد المراجعة"
        PAYMENT_CONFIRMED -> "معتمد ومجدول"
        REFUND_REQUESTED -> "طلب استرجاع قيد مراجعة الإدارة"
        COMPLETED -> "تمت الزيارة"
        REJECTED -> "إشعار التحويل يحتاج توضيح"
        CANCELLED -> "ملغي"
        else -> status
    }

    /** Wording differs by who walked away, so the patient is never blamed for a provider's decline. */
    fun cancelledLabel(cancelledBy: String): String = when (cancelledBy) {
        BY_PROVIDER -> "اعتذر مقدم الخدمة"
        BY_ADMIN -> "ألغيت الإدارة الطلب بعد قبول الاسترجاع"
        else -> "ألغيت الطلب"
    }
}
