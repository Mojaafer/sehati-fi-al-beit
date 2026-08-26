package com.example.data.model

import kotlin.math.round

/**
 * The money rules of the marketplace, derived here so no screen or viewmodel ever restates them.
 *
 * The platform keeps [COMMISSION_RATE] of every completed visit and the provider earns the rest.
 * On top of the service price each order carries a small unique suffix (a few SDG) that the
 * patient must transfer exactly: two different orders then never demand the same amount, so a
 * bank statement line identifies its order by amount alone even before the reference number is
 * checked. The suffix lives in the range [UNIQUE_SUFFIX_MIN]..[UNIQUE_SUFFIX_MAX] — large enough
 * to stay unique across concurrent open orders, small enough not to distort the price.
 */
object OrderFees {
    const val COMMISSION_RATE = 0.15

    /** What fraction of the service price lands in the provider's pocket. */
    const val PROVIDER_SHARE_RATE = 1.0 - COMMISSION_RATE

    const val UNIQUE_SUFFIX_MIN = 1
    const val UNIQUE_SUFFIX_MAX = 99

    /** Provider's share of a completed visit, rounded to piasters. */
    fun providerPayoutSdg(priceSdg: Double): Double = round2(priceSdg * PROVIDER_SHARE_RATE)

    /**
     * Derived as price minus payout rather than price times rate, so the two always add back up
     * to the exact price with no rounding drift between them.
     */
    fun commissionSdg(priceSdg: Double): Double = round2(priceSdg - providerPayoutSdg(priceSdg))

    /** The exact figure the patient must transfer: price plus the order's unique suffix. */
    fun payableAmountSdg(priceSdg: Double, uniqueSuffixSdg: Int): Double {
        require(uniqueSuffixSdg in UNIQUE_SUFFIX_MIN..UNIQUE_SUFFIX_MAX) {
            "المبلغ الفريد يجب أن يكون بين $UNIQUE_SUFFIX_MIN و$UNIQUE_SUFFIX_MAX"
        }
        return round2(priceSdg + uniqueSuffixSdg)
    }

    fun randomUniqueSuffix(): Int = (UNIQUE_SUFFIX_MIN..UNIQUE_SUFFIX_MAX).random()

    /**
     * Human-friendly order reference the patient quotes when transferring. The old
     * "HM-${1000..9999}" had a birthday-paradox collision across concurrent bookings — two
     * patients sharing a number makes receipt matching ambiguous. Time-seeded so two bookings
     * only collide if they happen in the same second AND draw the same 1-in-900 tail; still
     * short enough to read out over a phone call.
     */
    fun newOrderNumber(nowMillis: Long = System.currentTimeMillis()): String {
        val secondStamp = (nowMillis / 1000).toString().takeLast(5)
        val randomTail = (100..999).random()
        return "HM-$secondStamp$randomTail"
    }

    /**
     * Orders written before the money layer carry no amounts; they fall back to deriving from
     * the price instead of showing zeros everywhere.
     */
    fun effectivePayableAmountSdg(order: OrderEntity): Double =
        if (order.payableAmountSdg > 0.0) order.payableAmountSdg else order.priceSdg

    fun effectiveProviderPayoutSdg(order: OrderEntity): Double =
        if (order.providerPayoutSdg > 0.0) order.providerPayoutSdg else providerPayoutSdg(order.priceSdg)

    private fun round2(value: Double): Double = round(value * 100) / 100
}
