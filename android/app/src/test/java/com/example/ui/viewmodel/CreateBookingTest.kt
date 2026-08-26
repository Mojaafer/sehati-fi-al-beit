package com.example.ui.viewmodel

import com.example.MainDispatcherRule
import com.example.data.model.OrderStatus
import com.example.data.model.ProviderEntity
import com.example.data.repository.FakeNotificationRepository
import com.example.data.repository.FakeSehatiRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Placing an order is the one thing this app exists to do, and until now nothing covered it. The
 * order that lands in Firestore has to carry the slot the patient actually picked, the provider
 * they actually chose, and a phone number the provider can call — anything missing here surfaces
 * as a visit nobody can find.
 */
class CreateBookingTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val provider = ProviderEntity(
        id = "p1",
        name = "محمد عبدالرحمن",
        title = "فني مختبر",
        serviceCategory = "LAB_DRAW",
        priceSdg = 18000.0
    )

    private fun viewModel(repository: FakeSehatiRepository) = SehatiViewModel(
        repository = repository,
        storageRepository = null,
        notificationRepository = FakeNotificationRepository()
    )

    private fun book(viewModel: SehatiViewModel, phone: String = "+249912345678") {
        viewModel.selectProvider(provider)
        viewModel.createNewBooking(
            date = "الثلاثاء 11 أغسطس",
            time = "10:00 صباحاً",
            address = "ود مدني - حي الموردة، بيت رقم 12",
            notes = "الباب الأزرق",
            patientName = "سارة عثمان",
            patientPhone = phone
        )
    }

    @Test
    fun testTheOrderCarriesTheSlotThePatientPicked() = runTest {
        val repository = FakeSehatiRepository(providers = listOf(provider))
        val viewModel = viewModel(repository)

        book(viewModel)
        advanceUntilIdle()

        val order = repository.getOrderById("order-1")!!
        assertEquals("الثلاثاء 11 أغسطس", order.visitDate)
        assertEquals("10:00 صباحاً", order.visitTime)
        assertEquals("ود مدني - حي الموردة، بيت رقم 12", order.areaLocation)
        assertEquals("الباب الأزرق", order.notes)
    }

    @Test
    fun testTheOrderIsBoundToTheChosenProviderAndTheirPrice() = runTest {
        val repository = FakeSehatiRepository(providers = listOf(provider))
        val viewModel = viewModel(repository)

        book(viewModel)
        advanceUntilIdle()

        val order = repository.getOrderById("order-1")!!
        assertEquals("p1", order.providerId)
        assertEquals("محمد عبدالرحمن", order.providerName)
        assertEquals(18000.0, order.priceSdg, 0.001)
        assertEquals("سحب عينات منزلية", order.serviceTitle)
    }

    @Test
    fun testTheProviderGetsANumberToCall() = runTest {
        val repository = FakeSehatiRepository(providers = listOf(provider))
        val viewModel = viewModel(repository)

        book(viewModel)
        advanceUntilIdle()

        assertEquals("+249912345678", repository.getOrderById("order-1")!!.patientPhone)
    }

    @Test
    fun testANewOrderWaitsForTheProviderRatherThanAnAdmin() = runTest {
        val repository = FakeSehatiRepository(providers = listOf(provider))
        val viewModel = viewModel(repository)

        book(viewModel)
        advanceUntilIdle()

        val order = repository.getOrderById("order-1")!!
        assertEquals(OrderStatus.ORDER_SENT, order.status)
        // Nothing has been paid yet, so no transfer detail may be invented on the way in.
        assertEquals("", order.transferRefNum)
        assertEquals("", order.transferSenderName)
        assertNull(order.receiptImageUri)
        assertFalse(order.isRated)
    }

    @Test
    fun testAGuestWithoutASavedNameStillGetsAReadableOrder() = runTest {
        val repository = FakeSehatiRepository(providers = listOf(provider))
        val viewModel = viewModel(repository)

        viewModel.selectProvider(provider)
        viewModel.createNewBooking(
            date = "اليوم",
            time = "6:00 مساءً",
            address = "ود مدني",
            notes = "",
            patientName = "",
            patientPhone = ""
        )
        advanceUntilIdle()

        assertEquals("مريض", repository.getOrderById("order-1")!!.patientName)
    }

    @Test
    fun testTheConfirmationNamesTheOrderTheUserJustPlaced() = runTest {
        val repository = FakeSehatiRepository(providers = listOf(provider))
        val viewModel = viewModel(repository)

        book(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val orderNumber = repository.getOrderById("order-1")!!.orderNumber
        assertTrue(orderNumber.startsWith("HM-"))
        assertEquals(orderNumber, state.selectedOrder?.orderNumber)
        assertTrue(state.notificationMessage!!.contains(orderNumber))
    }

    @Test
    fun testBookingWithoutAProviderDoesNotWriteAnEmptyOrder() = runTest {
        val repository = FakeSehatiRepository()
        val viewModel = viewModel(repository)

        viewModel.createNewBooking(
            date = "اليوم",
            time = "6:00 مساءً",
            address = "ود مدني",
            notes = "",
            patientName = "سارة عثمان"
        )
        advanceUntilIdle()

        assertNull(repository.getOrderById("order-1"))
    }
}
