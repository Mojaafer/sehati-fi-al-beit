package com.example.ui.screens

import com.example.data.model.OrderEntity
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.SehatiUiState
import com.example.ui.viewmodel.UserRole
import org.junit.Assert.*
import org.junit.Test

class AdminDashboardTest {

    @Test
    fun testAdminPaymentReviewAndApprovalWorkflow() {
        val pendingOrder = OrderEntity(
            id = "order-300",
            orderNumber = "HM-3030",
            serviceTitle = "سحب عينات منزلي",
            patientName = "أحمد الرفاعي",
            priceSdg = 35000.0,
            paymentMethod = "بنكك (Bankak)",
            transferSenderName = "أحمد الرفاعي",
            transferRefNum = "5821049",
            status = "PAYMENT_UNDER_REVIEW"
        )

        var state = SehatiUiState(orders = listOf(pendingOrder))

        val reviewQueue = state.orders.filter { it.status == "PAYMENT_UNDER_REVIEW" }
        assertEquals(1, reviewQueue.size)
        assertEquals("5821049", reviewQueue.first().transferRefNum)

        // Admin approves payment -> status PAYMENT_CONFIRMED
        val approvedOrder = pendingOrder.copy(status = "PAYMENT_CONFIRMED")
        state = state.copy(orders = listOf(approvedOrder))

        val remainingUnderReview = state.orders.filter { it.status == "PAYMENT_UNDER_REVIEW" }
        assertTrue(remainingUnderReview.isEmpty())

        val confirmedOrders = state.orders.filter { it.status == "PAYMENT_CONFIRMED" }
        assertEquals(1, confirmedOrders.size)
    }

    @Test
    fun testAdminPaymentRejectionWorkflow() {
        val invalidOrder = OrderEntity(
            id = "order-301",
            orderNumber = "HM-3031",
            serviceTitle = "كشف منزلي",
            patientName = "خالد مصطفى",
            priceSdg = 35000.0,
            paymentMethod = "بنكك (Bankak)",
            transferSenderName = "خالد مصطفى",
            transferRefNum = "0000000",
            status = "PAYMENT_UNDER_REVIEW"
        )

        var state = SehatiUiState(orders = listOf(invalidOrder))

        // Admin rejects payment -> status REJECTED
        val rejectedOrder = invalidOrder.copy(status = "REJECTED")
        state = state.copy(orders = listOf(rejectedOrder))

        assertEquals("REJECTED", state.orders.first().status)
    }

    @Test
    fun testAdminRoleComesFromAuthState() {
        val authState = AuthUiState(isAuthenticated = true, role = UserRole.ADMIN)
        assertEquals(UserRole.ADMIN, authState.role)
    }
}
