package com.example.ui.viewmodel

import com.example.MainDispatcherRule
import com.example.data.model.OrderEntity
import com.example.data.model.ProviderEntity
import com.example.data.repository.FakeNotificationRepository
import com.example.data.repository.FakeSehatiRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Notifications are written client-side by whoever changes an order's status (no Cloud
 * Functions), so these tests pin the patient's inbox to each status transition.
 */
class OrderNotificationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val patientUid = "patient-1"

    private fun order(status: String) = OrderEntity(
        id = "o1",
        orderNumber = "HM-7781",
        serviceTitle = "سحب عينات منزلية",
        providerName = "محمد عبدالرحمن",
        patientUid = patientUid,
        status = status,
        receiptImageUri = if (status == "PAYMENT_UNDER_REVIEW") "image-receipt" else null
    )

    private fun viewModelWith(
        order: OrderEntity,
        notifications: FakeNotificationRepository
    ) = SehatiViewModel(
        repository = FakeSehatiRepository(orders = listOf(order)),
        storageRepository = null,
        notificationRepository = notifications
    )

    @Test
    fun testProviderAcceptanceNotifiesPatient() = runTest {
        val notifications = FakeNotificationRepository()
        val viewModel = viewModelWith(order("ORDER_SENT"), notifications)

        viewModel.acceptOrderByProvider("o1")
        advanceUntilIdle()

        val inbox = notifications.inboxOf(patientUid)
        assertEquals(1, inbox.size)
        assertEquals("PROVIDER", inbox.first().type)
        assertEquals("تم قبول طلبك", inbox.first().title)
        assertEquals("o1", inbox.first().orderId)
        assertFalse(inbox.first().read)
        assertTrue(inbox.first().body.contains("HM-7781"))
    }

    @Test
    fun testPaymentApprovalNotifiesPatient() = runTest {
        val notifications = FakeNotificationRepository()
        val viewModel = viewModelWith(order("PAYMENT_UNDER_REVIEW"), notifications)

        viewModel.approvePaymentByAdmin("o1")
        advanceUntilIdle()

        val inbox = notifications.inboxOf(patientUid)
        assertEquals(1, inbox.size)
        assertEquals("PAYMENT", inbox.first().type)
        assertEquals("تم اعتماد الدفع", inbox.first().title)
    }

    @Test
    fun testPaymentRejectionNotifiesPatientToReupload() = runTest {
        val notifications = FakeNotificationRepository()
        val viewModel = viewModelWith(order("PAYMENT_UNDER_REVIEW"), notifications)

        viewModel.rejectPaymentByAdmin("o1")
        advanceUntilIdle()

        val inbox = notifications.inboxOf(patientUid)
        assertEquals(1, inbox.size)
        assertEquals("PAYMENT", inbox.first().type)
        assertTrue(inbox.first().body.contains("إعادة رفع"))
    }

    @Test
    fun testCompletingVisitSetsCompletedStatusAndNotifies() = runTest {
        val notifications = FakeNotificationRepository()
        val repository = FakeSehatiRepository(orders = listOf(order("PAYMENT_CONFIRMED")))
        val viewModel = SehatiViewModel(
            repository = repository,
            storageRepository = null,
            notificationRepository = notifications
        )

        viewModel.completeOrderByProvider("o1")
        advanceUntilIdle()

        assertEquals("COMPLETED", repository.getOrderById("o1")?.status)
        val inbox = notifications.inboxOf(patientUid)
        assertEquals("ORDER", inbox.first().type)
        assertEquals("تمت الزيارة", inbox.first().title)
    }

    @Test
    fun testGuestOrderWithoutPatientUidSkipsNotification() = runTest {
        val notifications = FakeNotificationRepository()
        val viewModel = viewModelWith(order("ORDER_SENT").copy(patientUid = ""), notifications)

        viewModel.acceptOrderByProvider("o1")
        advanceUntilIdle()

        assertTrue(notifications.inboxOf("").isEmpty())
        assertTrue(notifications.inboxOf(patientUid).isEmpty())
    }

    /**
     * The other direction, and the one the provider's whole day depends on: nothing polls the
     * orders collection on their behalf, so a booking they are not told about is a booking they
     * discover late.
     */
    @Test
    fun testANewBookingReachesTheProvidersInbox() = runTest {
        val notifications = FakeNotificationRepository()
        val provider = ProviderEntity(
            id = "p1",
            name = "محمد عبدالرحمن",
            serviceCategory = "LAB_DRAW",
            ownerUid = "provider-owner-uid",
            priceSdg = 18000.0
        )
        val viewModel = SehatiViewModel(
            repository = FakeSehatiRepository(providers = listOf(provider)),
            storageRepository = null,
            notificationRepository = notifications
        )

        viewModel.selectProvider(provider)
        viewModel.createNewBooking(
            date = "الثلاثاء 11 أغسطس",
            time = "10:00 صباحاً",
            address = "ود مدني - حي الموردة",
            notes = "",
            patientName = "سارة عثمان",
            patientPhone = "+249912345678"
        )
        advanceUntilIdle()

        val inbox = notifications.inboxOf("provider-owner-uid")
        assertEquals(1, inbox.size)
        assertEquals("ORDER", inbox.first().type)
        assertEquals("طلب جديد", inbox.first().title)
        assertEquals("order-1", inbox.first().orderId)
        assertFalse(inbox.first().read)
    }

    /** The provider triages by whether they can be there, so the slot and place have to be in it. */
    @Test
    fun testTheNewBookingAlertCarriesTheSlotAndThePlace() = runTest {
        val notifications = FakeNotificationRepository()
        val provider = ProviderEntity(
            id = "p1",
            name = "محمد عبدالرحمن",
            serviceCategory = "LAB_DRAW",
            ownerUid = "provider-owner-uid"
        )
        val viewModel = SehatiViewModel(
            repository = FakeSehatiRepository(providers = listOf(provider)),
            storageRepository = null,
            notificationRepository = notifications
        )

        viewModel.selectProvider(provider)
        viewModel.createNewBooking(
            date = "الثلاثاء 11 أغسطس",
            time = "10:00 صباحاً",
            address = "ود مدني - حي الموردة",
            notes = "",
            patientName = "سارة عثمان"
        )
        advanceUntilIdle()

        val body = notifications.inboxOf("provider-owner-uid").first().body
        assertTrue(body.contains("سارة عثمان"))
        assertTrue(body.contains("ود مدني - حي الموردة"))
        assertTrue(body.contains("الثلاثاء 11 أغسطس"))
        assertTrue(body.contains("10:00 صباحاً"))
    }

    /** An unclaimed listing has no owner to write to; the booking must still go through. */
    @Test
    fun testABookingOnAnUnclaimedListingStillSucceeds() = runTest {
        val notifications = FakeNotificationRepository()
        val provider = ProviderEntity(id = "p1", name = "عيادة", serviceCategory = "LAB_DRAW")
        val repository = FakeSehatiRepository(providers = listOf(provider))
        val viewModel = SehatiViewModel(
            repository = repository,
            storageRepository = null,
            notificationRepository = notifications
        )

        viewModel.selectProvider(provider)
        viewModel.createNewBooking("اليوم", "6:00 مساءً", "ود مدني", "", "سارة عثمان")
        advanceUntilIdle()

        assertNotNull(repository.getOrderById("order-1"))
        assertTrue(notifications.inboxOf("").isEmpty())
    }
}
