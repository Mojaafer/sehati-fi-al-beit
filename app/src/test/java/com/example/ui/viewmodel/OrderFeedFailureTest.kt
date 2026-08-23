package com.example.ui.viewmodel

import com.example.MainDispatcherRule
import com.example.data.model.OrderEntity
import com.example.data.repository.FakeSehatiRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * A Firestore snapshot listener serves the local cache before the server rejects the listen, so a
 * refused feed looks identical to a live one until it silently stops. The admin dashboard hid a
 * failed approval that way, so these pin the refusal to a visible error and a working retry.
 */
class OrderFeedFailureTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val patientUid = "patient-1"

    private fun order() = OrderEntity(
        id = "o1",
        orderNumber = "HM-4975",
        serviceTitle = "سحب عينات منزلية",
        patientUid = patientUid,
        status = "PAYMENT_UNDER_REVIEW"
    )

    @Test
    fun testRefusedOrderListenReportsErrorInsteadOfLoadingForever() = runTest {
        val repository = FakeSehatiRepository(orders = listOf(order()))
        repository.denyOrderListens = true
        val viewModel = SehatiViewModel(repository = repository, storageRepository = null)

        viewModel.observeOrders(UserRole.ADMIN, "admin-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingOrders)
        assertNotNull(state.ordersErrorMessage)
    }

    @Test
    fun testLiveOrderListenReportsNoError() = runTest {
        val repository = FakeSehatiRepository(orders = listOf(order()))
        val viewModel = SehatiViewModel(repository = repository, storageRepository = null)

        viewModel.observeOrders(UserRole.ADMIN, "admin-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.ordersErrorMessage)
        assertEquals(1, state.orders.size)
    }

    @Test
    fun testRetryResubscribesAfterARefusedListen() = runTest {
        val repository = FakeSehatiRepository(orders = listOf(order()))
        repository.denyOrderListens = true
        val viewModel = SehatiViewModel(repository = repository, storageRepository = null)

        viewModel.observeOrders(UserRole.ADMIN, "admin-1")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.ordersErrorMessage)

        // Standing in for the account gaining the role it was missing.
        repository.denyOrderListens = false
        viewModel.retryObserveOrders(UserRole.ADMIN, "admin-1")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.ordersErrorMessage)

        // The feed has to be live again, not merely error-free: the bug was a list that kept
        // rendering while ignoring every later change.
        repository.updateOrderStatus("o1", "PAYMENT_CONFIRMED")
        advanceUntilIdle()

        assertEquals("PAYMENT_CONFIRMED", viewModel.uiState.value.orders.first().status)
    }
}
