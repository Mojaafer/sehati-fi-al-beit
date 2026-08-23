package com.example.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * The money rules every other layer trusts. If these drift, providers get paid the wrong share
 * and patients get asked for amounts nobody can match to a bank statement.
 */
class OrderFeesTest {

    @Test
    fun testCommissionPlusPayoutAddsBackUpToTheExactPrice() {
        // Derived as price minus payout on purpose, so rounding can never open a gap between
        // what the patient paid and where it went.
        val price = 15_000.0
        assertEquals(price, OrderFees.commissionSdg(price) + OrderFees.providerPayoutSdg(price), 0.0)
    }

    @Test
    fun testThePlatformKeeps15Percent() {
        assertEquals(2_250.0, OrderFees.commissionSdg(15_000.0), 0.001)
        assertEquals(12_750.0, OrderFees.providerPayoutSdg(15_000.0), 0.001)
    }

    @Test
    fun testOddPricesRoundToPiastersWithoutLosingMoney() {
        // 85% of 1000.07 is 850.0595 -> 850.06; the commission must absorb the remainder,
        // not drop it. The sum is asserted within a piaster because two independently rounded
        // doubles need not add back bit-exactly — anything below half a piaster is invisible
        // money anyway.
        val price = 1000.07
        val payout = OrderFees.providerPayoutSdg(price)
        val commission = OrderFees.commissionSdg(price)
        assertEquals(price, payout + commission, 0.001)
        assertEquals(850.06, payout, 0.001)
    }

    @Test
    fun testPayableAmountCarriesTheUniqueSuffixExactly() {
        assertEquals(15_042.0, OrderFees.payableAmountSdg(15_000.0, 42), 0.0)
        assertEquals(0.55 + 7.0, OrderFees.payableAmountSdg(0.55, 7), 0.0)
    }

    @Test
    fun testSuffixOutsideItsRangeIsRejectedRatherThanSilentlyAccepted() {
        var thrown = false
        try {
            OrderFees.payableAmountSdg(100.0, 0)
        } catch (e: IllegalArgumentException) {
            thrown = true
        }
        assertTrue("suffix 0 must be rejected", thrown)

        thrown = false
        try {
            OrderFees.payableAmountSdg(100.0, 100)
        } catch (e: IllegalArgumentException) {
            thrown = true
        }
        assertTrue("suffix 100 must be rejected", thrown)
    }

    @Test
    fun testRandomSuffixesAlwaysLandInsideTheUsableRange() {
        repeat(500) {
            val suffix = OrderFees.randomUniqueSuffix()
            assertTrue(suffix in OrderFees.UNIQUE_SUFFIX_MIN..OrderFees.UNIQUE_SUFFIX_MAX)
        }
    }

    @Test
    fun testOrdersFromBeforeTheMoneyLayerFallBackToDeriving() {
        // A zero means "field missing", never "free" — so legacy documents keep showing real
        // figures derived from the price instead of zeros everywhere.
        val legacy = OrderEntity(id = "o1", priceSdg = 20_000.0)
        assertEquals(20_000.0, OrderFees.effectivePayableAmountSdg(legacy), 0.001)
        assertEquals(17_000.0, OrderFees.effectiveProviderPayoutSdg(legacy), 0.001)
    }

    @Test
    fun testExplicitMoneyTermsAreUsedAsWritten() {
        val modern = OrderEntity(
            id = "o2",
            priceSdg = 20_000.0,
            payableAmountSdg = 20_033.0,
            providerPayoutSdg = 17_000.0
        )
        assertEquals(20_033.0, OrderFees.effectivePayableAmountSdg(modern), 0.001)
        assertEquals(17_000.0, OrderFees.effectiveProviderPayoutSdg(modern), 0.001)
    }
}
