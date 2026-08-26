package com.example.ui.viewmodel

import com.example.MainDispatcherRule
import com.example.data.model.OrderEntity
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
 * Cancelling is the one action that destroys value for the other party, so each test checks both
 * halves: the order really changed, and the other side was told why.
 */
class OrderCancellationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val patientUid = "patient-1"
    private val providerUid = "provider-owner-1"

    private val provider = ProviderEntity(
        id = "p1",
        name = "محمد عبدالرحمن",
        ownerUid = providerUid
    )

    private fun order(status: String) = OrderEntity(
        id = "o1",
        orderNumber = "HM-7781",
        serviceTitle = "سحب عينات منزلية",
        providerName = "محمد عبدالرحمن",
        providerId = "p1",
        patientUid = patientUid,
        status = status
    )

    private fun setup(status: String): Triple<SehatiViewModel, FakeSehatiRepository, FakeNotificationRepository> {
        val repository = FakeSehatiRepository(
            providers = listOf(provider),
            orders = listOf(order(status))
        )
        val notifications = FakeNotificationRepository()
        return Triple(
            SehatiViewModel(
                repository = repository,
                storageRepository = null,
                notificationRepository = notifications
            ),
            repository,
            notifications
        )
    }

    @Test
    fun testPatientCancellationRecordsReasonAndTellsProvider() = runTest {
        val (viewModel, repository, notifications) = setup(OrderStatus.ACCEPTED_BY_PROVIDER)

        viewModel.cancelOrderByPatient("o1", "تغير موعدي")
        advanceUntilIdle()

        val cancelled = repository.getOrderById("o1")
        assertEquals(OrderStatus.CANCELLED, cancelled?.status)
        assertEquals(OrderStatus.BY_PATIENT, cancelled?.cancelledBy)
        assertEquals("تغير موعدي", cancelled?.cancelReason)

        val providerInbox = notifications.inboxOf(providerUid)
        assertEquals(1, providerInbox.size)
        assertEquals("ألغى المريض الطلب", providerInbox.first().title)
        assertTrue(providerInbox.first().body.contains("تغير موعدي"))
        assertTrue(notifications.inboxOf(patientUid).isEmpty())
    }

    @Test
    fun testPatientCannotCancelAfterPaymentIsConfirmed() = runTest {
        val (viewModel, repository, notifications) = setup(OrderStatus.PAYMENT_CONFIRMED)

        viewModel.cancelOrderByPatient("o1", "غيرت رأيي")
        advanceUntilIdle()

        assertEquals(OrderStatus.PAYMENT_CONFIRMED, repository.getOrderById("o1")?.status)
        assertTrue(notifications.inboxOf(providerUid).isEmpty())
        assertTrue(viewModel.uiState.value.notificationMessage!!.contains("تواصل مع الإدارة"))
    }

    @Test
    fun testProviderDeclineTellsPatientToPickSomeoneElse() = runTest {
        val (viewModel, repository, notifications) = setup(OrderStatus.ORDER_SENT)

        viewModel.declineOrderByProvider("o1", "المنطقة بعيدة")
        advanceUntilIdle()

        val cancelled = repository.getOrderById("o1")
        assertEquals(OrderStatus.CANCELLED, cancelled?.status)
        assertEquals(OrderStatus.BY_PROVIDER, cancelled?.cancelledBy)

        val patientInbox = notifications.inboxOf(patientUid)
        assertEquals(1, patientInbox.size)
        assertEquals("اعتذر مقدم الخدمة", patientInbox.first().title)
        assertTrue(patientInbox.first().body.contains("المنطقة بعيدة"))
        assertTrue(patientInbox.first().body.contains("مقدم خدمة آخر"))
    }

    @Test
    fun testProviderCannotDeclineAnOrderTheyAlreadyAccepted() = runTest {
        val (viewModel, repository, notifications) = setup(OrderStatus.ACCEPTED_BY_PROVIDER)

        viewModel.declineOrderByProvider("o1", "مرتبط بزيارة أخرى")
        advanceUntilIdle()

        assertEquals(OrderStatus.ACCEPTED_BY_PROVIDER, repository.getOrderById("o1")?.status)
        assertTrue(notifications.inboxOf(patientUid).isEmpty())
        assertTrue(viewModel.uiState.value.notificationMessage!!.contains("تم قبوله بالفعل"))
    }

    @Test
    fun testCancellingWithoutAReasonStillNotifiesWithoutADanglingDash() = runTest {
        val (viewModel, _, notifications) = setup(OrderStatus.ORDER_SENT)

        viewModel.declineOrderByProvider("o1", "")
        advanceUntilIdle()

        val body = notifications.inboxOf(patientUid).first().body
        assertFalse(body.contains("السبب:"))
        assertTrue(body.contains("مقدم خدمة آخر"))
    }

    @Test
    fun testSecondTapIsIgnoredWhileTheFirstIsStillWriting() = runTest {
        val (viewModel, _, notifications) = setup(OrderStatus.ORDER_SENT)

        // Both calls land before the dispatcher runs, so the busy guard must swallow the second.
        viewModel.declineOrderByProvider("o1", "المنطقة بعيدة")
        viewModel.declineOrderByProvider("o1", "المنطقة بعيدة")
        advanceUntilIdle()

        assertEquals(1, notifications.inboxOf(patientUid).size)
    }

    @Test
    fun testBusyOrderIdIsClearedOnceTheActionFinishes() = runTest {
        val (viewModel, _, _) = setup(OrderStatus.ORDER_SENT)

        viewModel.declineOrderByProvider("o1", "الوقت غير مناسب")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.busyOrderId)
    }
}
