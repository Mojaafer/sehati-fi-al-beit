package com.example.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Firestore needs a no-arg constructor, so the defaults on [OrderEntity] are what a document
 * missing a field deserialises to. They used to be demo values — a patient called "أحمد الرفاعي",
 * an address in "حي الدرجة" and bank reference "5821049" — which meant a partial write could show
 * an admin a transfer that was never sent. These pin them empty.
 */
class OrderEntityDefaultsTest {

    private val blank = OrderEntity()

    @Test
    fun testNoIdentityFieldCarriesAnInventedPerson() {
        assertEquals("", blank.patientName)
        assertEquals("", blank.patientPhone)
        assertEquals("", blank.providerName)
        assertEquals("", blank.providerTitle)
    }

    @Test
    fun testNoVisitFieldCarriesAnInventedSlotOrAddress() {
        assertEquals("", blank.areaLocation)
        assertEquals("", blank.visitDate)
        assertEquals("", blank.visitTime)
        assertEquals("", blank.serviceDetails)
    }

    @Test
    fun testNoPaymentFieldCarriesAnInventedTransfer() {
        assertEquals("", blank.transferRefNum)
        assertEquals("", blank.transferSenderName)
        assertEquals("", blank.paymentMethod)
        assertEquals(0.0, blank.priceSdg, 0.001)
        assertNull(blank.receiptImageUri)
    }

    @Test
    fun testMoneyTermsDefaultToZeroNotAnInventedAmount() {
        // Zero is the "derive from price" signal in [OrderFees]; any plausible-looking default
        // here would ask patients for a transfer nobody agreed to.
        assertEquals(0.0, blank.payableAmountSdg, 0.001)
        assertEquals(0.0, blank.providerPayoutSdg, 0.001)
        assertEquals(0.0, blank.commissionSdg, 0.001)
    }

    @Test
    fun testAMalformedOrderDoesNotLandInTheAdminApprovalQueue() {
        // The old default was PAYMENT_UNDER_REVIEW, so a document that lost its status showed up
        // asking an admin to approve money that was never transferred.
        assertEquals(OrderStatus.ORDER_SENT, blank.status)
    }

    @Test
    fun testABlankOrderIsNotSilentlyMarkedRated() {
        assertFalse(blank.isRated)
    }
}
