package com.example.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Cancellation windows are the only thing standing between a patient and a refund dispute, so
 * they are pinned here rather than left to the UI to decide screen by screen.
 */
class OrderStatusTest {

    @Test
    fun testPatientCanCancelBeforeMoneyIsConfirmed() {
        assertTrue(OrderStatus.canPatientCancel(OrderStatus.ORDER_SENT))
        assertTrue(OrderStatus.canPatientCancel(OrderStatus.ACCEPTED_BY_PROVIDER))
        assertTrue(OrderStatus.canPatientCancel(OrderStatus.PAYMENT_PENDING))
        assertTrue(OrderStatus.canPatientCancel(OrderStatus.PAYMENT_UNDER_REVIEW))
        assertTrue(OrderStatus.canPatientCancel(OrderStatus.REJECTED))
    }

    @Test
    fun testPatientCannotCancelOnceMoneyIsConfirmed() {
        // A confirmed payment owes a refund, which is an admin conversation, not a status flip.
        assertFalse(OrderStatus.canPatientCancel(OrderStatus.PAYMENT_CONFIRMED))
        assertFalse(OrderStatus.canPatientCancel(OrderStatus.COMPLETED))
        assertFalse(OrderStatus.canPatientCancel(OrderStatus.CANCELLED))
    }

    @Test
    fun testProviderCanDeclineOnlyBeforeAccepting() {
        assertTrue(OrderStatus.canProviderDecline(OrderStatus.ORDER_SENT))
        assertFalse(OrderStatus.canProviderDecline(OrderStatus.ACCEPTED_BY_PROVIDER))
        assertFalse(OrderStatus.canProviderDecline(OrderStatus.PAYMENT_CONFIRMED))
        assertFalse(OrderStatus.canProviderDecline(OrderStatus.COMPLETED))
    }

    @Test
    fun testTerminalStatusesEndTheOrder() {
        assertTrue(OrderStatus.isTerminal(OrderStatus.COMPLETED))
        assertTrue(OrderStatus.isTerminal(OrderStatus.CANCELLED))
        assertFalse(OrderStatus.isTerminal(OrderStatus.ORDER_SENT))
        assertFalse(OrderStatus.isTerminal(OrderStatus.PAYMENT_CONFIRMED))
    }

    @Test
    fun testCancelledLabelNamesWhoWalkedAway() {
        assertEquals("اعتذر مقدم الخدمة", OrderStatus.cancelledLabel(OrderStatus.BY_PROVIDER))
        assertEquals("ألغيت الطلب", OrderStatus.cancelledLabel(OrderStatus.BY_PATIENT))
        assertEquals("ألغيت الإدارة الطلب بعد قبول الاسترجاع", OrderStatus.cancelledLabel(OrderStatus.BY_ADMIN))
    }

    @Test
    fun testRefundRequestOnlyFromAPaidUnvisitedOrder() {
        assertTrue(OrderStatus.canPatientRequestRefund(OrderStatus.PAYMENT_CONFIRMED))
        assertFalse(OrderStatus.canPatientRequestRefund(OrderStatus.PAYMENT_UNDER_REVIEW))
        assertFalse(OrderStatus.canPatientRequestRefund(OrderStatus.COMPLETED))
    }

    @Test
    fun testARefundPendingOrderIsNotCancellableByThePatientAgain() {
        // Resolution belongs to the admin; a second patient-side tap would race the review.
        assertFalse(OrderStatus.canPatientCancel(OrderStatus.REFUND_REQUESTED))
    }

    @Test
    fun testRefundPendingHasAnArabicLabel() {
        assertEquals("طلب استرجاع قيد مراجعة الإدارة", OrderStatus.label(OrderStatus.REFUND_REQUESTED))
    }

    @Test
    fun testUnknownStatusFallsBackToItsOwnCode() {
        assertEquals("SOMETHING_NEW", OrderStatus.label("SOMETHING_NEW"))
    }
}
