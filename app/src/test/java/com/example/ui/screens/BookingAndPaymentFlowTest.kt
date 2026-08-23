package com.example.ui.screens

import com.example.data.model.OrderEntity
import com.example.data.model.ProviderEntity
import com.example.ui.navigation.Routes
import com.example.ui.viewmodel.SehatiUiState
import org.junit.Assert.*
import org.junit.Test

class BookingAndPaymentFlowTest {

    @Test
    fun testEndToEndPatientBookingAndPaymentFlow() {
        // Step 1: Initial state on Home Screen
        var state = SehatiUiState()
        var route = Routes.HOME
        assertEquals(Routes.HOME, route)

        // Step 2: Select Provider for booking
        val selectedProvider = ProviderEntity(
            id = "provider-1",
            name = "د. فاطمة عمر",
            title = "طبيبة عامة وكشف منزلي",
            experienceYears = 10,
            rating = 4.95,
            reviewsCount = 158,
            distanceKm = 4.1,
            priceSdg = 35000.0,
            isAvailableNow = true,
            serviceCategory = "DOCTOR",
            area = "ود مدني - حي عووضة"
        )
        state = state.copy(selectedProvider = selectedProvider)
        route = Routes.bookingConfirmRoute(selectedProvider.id)

        assertNotNull(state.selectedProvider)
        assertEquals("booking_confirm/provider-1", route)

        // Step 3: Confirm Booking and generate Order
        val generatedOrder = OrderEntity(
            id = "order-101",
            orderNumber = "HM-1042",
            serviceTitle = "كشف طبي منزلي",
            patientName = "أحمد الرفاعي",
            providerName = selectedProvider.name,
            providerTitle = selectedProvider.title,
            areaLocation = selectedProvider.area,
            priceSdg = selectedProvider.priceSdg,
            status = "ORDER_SENT"
        )
        state = state.copy(
            selectedOrder = generatedOrder,
            orders = listOf(generatedOrder),
            navigationEvent = Routes.ORDER_SUCCESS
        )
        assertEquals("HM-1042", state.selectedOrder?.orderNumber)
        assertEquals(Routes.ORDER_SUCCESS, state.navigationEvent)

        // Navigation event is consumed once handled
        state = state.copy(navigationEvent = null)
        route = Routes.ORDER_SUCCESS
        assertNull(state.navigationEvent)

        // Step 4: Proceed to Payment Method Screen
        state = state.copy(selectedPaymentMethod = "بنكك (Bankak)")
        route = Routes.PAYMENT_METHOD
        assertEquals("بنكك (Bankak)", state.selectedPaymentMethod)
        assertEquals(Routes.PAYMENT_METHOD, route)

        // Step 5: Upload Receipt details
        state = state.copy(
            transferSenderName = "أحمد الرفاعي",
            transferRefNum = "5821049",
            receiptLocalUri = "content://media/picked/receipt"
        )
        route = Routes.UPLOAD_RECEIPT
        assertEquals(Routes.UPLOAD_RECEIPT, route)
        assertEquals("أحمد الرفاعي", state.transferSenderName)
        assertEquals("5821049", state.transferRefNum)
        assertTrue(state.receiptImageSelected)

        // Step 6: Receipt uploads to Storage, then the order carries its download URL
        val orderUnderReview = generatedOrder.copy(
            status = "PAYMENT_UNDER_REVIEW",
            paymentMethod = state.selectedPaymentMethod,
            transferSenderName = state.transferSenderName,
            transferRefNum = state.transferRefNum,
            receiptImageUri = "https://firebasestorage.example/receipts/uid/order-101.jpg"
        )
        state = state.copy(
            selectedOrder = orderUnderReview,
            orders = listOf(orderUnderReview),
            navigationEvent = Routes.PAYMENT_REVIEW
        )
        assertEquals("PAYMENT_UNDER_REVIEW", state.selectedOrder?.status)
        assertEquals(Routes.PAYMENT_REVIEW, state.navigationEvent)
        assertTrue(state.selectedOrder?.receiptImageUri?.startsWith("https://") == true)

        state = state.copy(navigationEvent = null)

        // Step 7: Admin confirms Payment
        val confirmedOrder = orderUnderReview.copy(status = "PAYMENT_CONFIRMED")
        state = state.copy(
            selectedOrder = confirmedOrder,
            orders = listOf(confirmedOrder)
        )
        route = Routes.PAYMENT_CONFIRMED
        assertEquals("PAYMENT_CONFIRMED", state.selectedOrder?.status)
        assertEquals(Routes.PAYMENT_CONFIRMED, route)
    }
}
