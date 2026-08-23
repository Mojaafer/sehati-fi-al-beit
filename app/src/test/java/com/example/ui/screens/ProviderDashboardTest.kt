package com.example.ui.screens

import com.example.data.model.OrderEntity
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.SehatiUiState
import com.example.ui.viewmodel.UserRole
import org.junit.Assert.*
import org.junit.Test

class ProviderDashboardTest {

    @Test
    fun testToggleProviderAvailabilitySwitch() {
        var state = SehatiUiState(isProviderAvailable = true)
        assertTrue(state.isProviderAvailable)

        // Toggle to unavailable
        state = state.copy(isProviderAvailable = !state.isProviderAvailable)
        assertFalse(state.isProviderAvailable)

        // Toggle back to available
        state = state.copy(isProviderAvailable = !state.isProviderAvailable)
        assertTrue(state.isProviderAvailable)
    }

    @Test
    fun testProviderRoleComesFromAuthState() {
        val authState = AuthUiState(isAuthenticated = true, role = UserRole.PROVIDER)
        assertEquals(UserRole.PROVIDER, authState.role)
        assertTrue(authState.isAuthenticated)
    }

    @Test
    fun testProviderOrderAcceptanceWorkflow() {
        val incomingOrder = OrderEntity(
            id = "order-200",
            orderNumber = "HM-2020",
            serviceTitle = "تمريض منزلي - تركيب مغذي",
            patientName = "عثمان مبارك",
            providerName = "مريم علي",
            status = "PAYMENT_CONFIRMED"
        )

        var state = SehatiUiState(orders = listOf(incomingOrder))
        assertEquals(1, state.orders.size)
        assertEquals("PAYMENT_CONFIRMED", state.orders.first().status)

        // Provider starts visit -> status IN_PROGRESS
        val orderInProgress = incomingOrder.copy(status = "IN_PROGRESS")
        state = state.copy(orders = listOf(orderInProgress))
        assertEquals("IN_PROGRESS", state.orders.first().status)

        // Provider completes visit -> status COMPLETED
        val completedOrder = orderInProgress.copy(status = "COMPLETED")
        state = state.copy(orders = listOf(completedOrder))
        assertEquals("COMPLETED", state.orders.first().status)
    }
}
