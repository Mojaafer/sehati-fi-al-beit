package com.example.ui.viewmodel

import com.example.ui.navigation.Routes
import com.example.data.model.OrderStatus
import org.junit.Assert.*
import org.junit.Test

class SehatiViewModelTest {

    @Test
    fun testCategorySelectionState() {
        var state = SehatiUiState()
        assertEquals("ALL", state.selectedCategory)

        state = state.copy(selectedCategory = "LAB_DRAW")
        assertEquals("LAB_DRAW", state.selectedCategory)
    }

    @Test
    fun testPaymentDetailsStateUpdate() {
        var state = SehatiUiState()
        state = state.copy(
            transferSenderName = "علي عثمان",
            transferRefNum = "1234567",
            selectedPaymentMethod = "فوري"
        )

        assertEquals("علي عثمان", state.transferSenderName)
        assertEquals("1234567", state.transferRefNum)
        assertEquals("فوري", state.selectedPaymentMethod)
    }

    @Test
    fun testProviderAvailabilityToggleState() {
        var state = SehatiUiState()
        assertTrue(state.isProviderAvailable)

        state = state.copy(isProviderAvailable = !state.isProviderAvailable)
        assertFalse(state.isProviderAvailable)
    }

    @Test
    fun testNotificationMessageState() {
        var state = SehatiUiState(notificationMessage = "تم إرسال الطلب بنجاح")
        assertEquals("تم إرسال الطلب بنجاح", state.notificationMessage)

        state = state.copy(notificationMessage = null)
        assertNull(state.notificationMessage)
    }

    @Test
    fun testNavigationEventIsConsumable() {
        var state = SehatiUiState()
        assertNull(state.navigationEvent)

        state = state.copy(navigationEvent = Routes.ORDER_SUCCESS)
        assertEquals(Routes.ORDER_SUCCESS, state.navigationEvent)

        state = state.copy(navigationEvent = null)
        assertNull(state.navigationEvent)
    }

    @Test
    fun patientPaymentRequiresProviderAcceptance() {
        assertFalse(OrderStatus.canPatientPay(OrderStatus.ORDER_SENT))
        assertTrue(OrderStatus.canPatientPay(OrderStatus.ACCEPTED_BY_PROVIDER))
        assertTrue(OrderStatus.canPatientPay(OrderStatus.PAYMENT_PENDING))
        assertTrue(OrderStatus.canPatientPay(OrderStatus.REJECTED))
        assertFalse(OrderStatus.canPatientPay(OrderStatus.PAYMENT_CONFIRMED))
    }

    @Test
    fun providerCanCompleteOnlyConfirmedPayment() {
        assertFalse(OrderStatus.canProviderComplete(OrderStatus.ACCEPTED_BY_PROVIDER))
        assertTrue(OrderStatus.canProviderComplete(OrderStatus.PAYMENT_CONFIRMED))
        assertFalse(OrderStatus.canProviderComplete(OrderStatus.COMPLETED))
    }
}
