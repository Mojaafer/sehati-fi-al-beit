package com.example.ui.viewmodel

import com.example.data.model.OrderEntity
import com.example.data.model.ProviderEntity
import com.example.ui.navigation.Routes
import org.junit.Assert.*
import org.junit.Test

class SehatiUiStateTest {

    @Test
    fun testDefaultSehatiUiState() {
        val state = SehatiUiState()
        assertEquals("ALL", state.selectedCategory)
        assertNull(state.selectedProvider)
        assertNull(state.selectedOrder)
        assertTrue(state.providers.isEmpty())
        assertTrue(state.orders.isEmpty())
        assertTrue(state.isProviderAvailable)
        assertEquals("بنكك (Bankak)", state.selectedPaymentMethod)
        assertEquals("", state.transferSenderName)
        assertEquals("", state.transferRefNum)
        assertNull(state.receiptLocalUri)
        assertFalse(state.receiptImageSelected)
        assertFalse(state.isUploadingReceipt)
        assertNull(state.notificationMessage)
        assertNull(state.navigationEvent)
    }

    @Test
    fun testStateCopyPreservesUnrelatedFields() {
        val initialState = SehatiUiState()
        val updatedState = initialState.copy(navigationEvent = Routes.PAYMENT_REVIEW)

        assertEquals(Routes.PAYMENT_REVIEW, updatedState.navigationEvent)
        assertEquals(initialState.selectedCategory, updatedState.selectedCategory)
        assertEquals(initialState.selectedPaymentMethod, updatedState.selectedPaymentMethod)
    }

    @Test
    fun testProviderAndOrderSelection() {
        val provider = ProviderEntity(
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
        val order = OrderEntity(
            id = "order-100",
            orderNumber = "HM-1042",
            serviceTitle = "سحب عينات منزلية",
            patientName = "أحمد الرفاعي",
            status = "ORDER_SENT"
        )

        val state = SehatiUiState(
            selectedProvider = provider,
            selectedOrder = order,
            providers = listOf(provider),
            orders = listOf(order)
        )

        assertNotNull(state.selectedProvider)
        assertEquals("د. فاطمة عمر", state.selectedProvider?.name)
        assertEquals(35000.0, state.selectedProvider?.priceSdg ?: 0.0, 0.01)
        assertEquals(1, state.providers.size)

        assertNotNull(state.selectedOrder)
        assertEquals("HM-1042", state.selectedOrder?.orderNumber)
        assertEquals("ORDER_SENT", state.selectedOrder?.status)
    }

    @Test
    fun testUserRoleEnumValues() {
        val roles = UserRole.values()
        assertEquals(3, roles.size)
        assertTrue(roles.contains(UserRole.PATIENT))
        assertTrue(roles.contains(UserRole.PROVIDER))
        assertTrue(roles.contains(UserRole.ADMIN))
    }

    @Test
    fun testPatientRoutesAreDistinct() {
        val routes = listOf(
            Routes.HOME,
            Routes.PROVIDERS_LIST,
            Routes.BOOKING_CONFIRM,
            Routes.ORDER_SUCCESS,
            Routes.PAYMENT_METHOD,
            Routes.UPLOAD_RECEIPT,
            Routes.PAYMENT_REVIEW,
            Routes.PAYMENT_CONFIRMED,
            Routes.MY_ORDERS,
            Routes.PROFILE
        )
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun testReceiptSelectionDerivesFromLocalUri() {
        var state = SehatiUiState()
        assertFalse(state.receiptImageSelected)

        state = state.copy(receiptLocalUri = "content://media/picked/42")
        assertTrue(state.receiptImageSelected)

        state = state.copy(receiptLocalUri = null)
        assertFalse(state.receiptImageSelected)
    }

    @Test
    fun testParameterizedRouteBuilders() {
        assertEquals("providers_list/LAB_DRAW", Routes.providersListRoute("LAB_DRAW"))
        assertEquals("booking_confirm/abc7", Routes.bookingConfirmRoute("abc7"))
        assertEquals("provider_profile/xyz3", Routes.providerProfileRoute("xyz3"))
        assertEquals("rate_order/ord100", Routes.rateOrderRoute("ord100"))
    }
}
