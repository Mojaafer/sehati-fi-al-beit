package com.example.data.repository

import com.example.data.model.OrderEntity
import com.example.data.model.OrderStatus
import com.example.data.model.PayoutEntity
import com.example.data.model.PayoutStatus
import org.junit.Assert.*
import org.junit.Test

/**
 * The admin finance screen and the provider earnings screen both render straight out of these
 * folds, so a wrong filter here would quietly misstate the business.
 */
class PayoutSummaryTest {

    private fun payout(
        id: String,
        amount: Double,
        status: String
    ) = PayoutEntity(id = id, orderId = id, providerId = "p1", amountSdg = amount, status = status)

    @Test
    fun testEarningsSplitOwedFromPaid() {
        val earnings = foldEarnings(
            listOf(
                payout("o1", 8_500.0, PayoutStatus.ACCRUED),
                payout("o2", 12_750.0, PayoutStatus.ACCRUED),
                payout("o3", 4_250.0, PayoutStatus.PAID)
            )
        )

        assertEquals(21_250.0, earnings.accruedSdg, 0.001)
        assertEquals(2, earnings.accruedCount)
        assertEquals(4_250.0, earnings.paidSdg, 0.001)
    }

    @Test
    fun testAnEmptyLedgerOwesNothingRatherThanFailing() {
        val earnings = foldEarnings(emptyList())
        assertEquals(0.0, earnings.accruedSdg, 0.001)
        assertEquals(0.0, earnings.paidSdg, 0.001)
        assertEquals(0, earnings.accruedCount)
    }

    @Test
    fun testCollectionCountsOnlyMoneyActuallyConfirmedOrEarned() {
        // A booking nobody paid for and one that was cancelled are not revenue, however
        // optimistic the dashboard feels on a slow day.
        val orders = listOf(
            OrderEntity(id = "a", status = OrderStatus.ORDER_SENT, priceSdg = 9_999.0),
            OrderEntity(id = "b", status = OrderStatus.PAYMENT_UNDER_REVIEW, priceSdg = 9_999.0),
            OrderEntity(id = "c", status = OrderStatus.CANCELLED, priceSdg = 9_999.0),
            OrderEntity(id = "d", status = OrderStatus.PAYMENT_CONFIRMED, priceSdg = 10_000.0),
            OrderEntity(id = "e", status = OrderStatus.COMPLETED, priceSdg = 5_000.0)
        )

        val summary = foldAdminFinance(orders, emptyList())

        assertEquals(15_000.0, summary.collectedSdg, 0.001)
    }

    @Test
    fun testCommissionComesFromTheOrderNotTheRateRestated() {
        // The fold reads commissionSdg off each order so the headline rate can change in one
        // place without history silently repricing itself.
        val orders = listOf(
            OrderEntity(
                id = "d",
                status = OrderStatus.PAYMENT_CONFIRMED,
                priceSdg = 10_000.0,
                commissionSdg = 1_500.0
            )
        )

        val summary = foldAdminFinance(orders, emptyList())

        assertEquals(1_500.0, summary.commissionSdg, 0.001)
    }

    @Test
    fun testNetPositionIsWhatIsStillHeldForOthers() {
        val summary = foldAdminFinance(
            listOf(
                OrderEntity(id = "d", status = OrderStatus.PAYMENT_CONFIRMED, priceSdg = 10_000.0)
            ),
            listOf(
                payout("d", 8_500.0, PayoutStatus.ACCRUED),
                payout("old", 3_000.0, PayoutStatus.PAID)
            )
        )

        assertEquals(-1_500.0, summary.netPositionSdg, 0.001)
    }

    @Test
    fun testRepeatRateCountsReturningPatientsAmongFinishers() {
        val orders = listOf(
            // p1 finished twice -> returning; p2 once; p3 booked and paid but never finished,
            // so they are not part of a retention denominator at all.
            OrderEntity(id = "a", status = OrderStatus.COMPLETED, patientUid = "p1"),
            OrderEntity(id = "b", status = OrderStatus.COMPLETED, patientUid = "p1"),
            OrderEntity(id = "c", status = OrderStatus.COMPLETED, patientUid = "p2"),
            OrderEntity(id = "e", status = OrderStatus.PAYMENT_CONFIRMED, patientUid = "p3")
        )
        assertEquals(0.5, repeatBookingRate(orders)!!, 0.001)
    }

    @Test
    fun testRepeatRateIsUnknownUntilSomebodyFinishesAVisit() {
        assertNull(repeatBookingRate(emptyList()))
        assertNull(
            repeatBookingRate(
                listOf(OrderEntity(id = "a", status = OrderStatus.ORDER_SENT, patientUid = "p1"))
            )
        )
    }
}
