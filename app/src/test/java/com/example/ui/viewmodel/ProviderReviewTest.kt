package com.example.ui.viewmodel

import com.example.MainDispatcherRule
import com.example.data.model.NotificationEntity
import com.example.data.model.ProviderEntity
import com.example.data.repository.FakeNotificationRepository
import com.example.data.repository.FakeProviderRegistrationRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * A provider application is the one flow where the two parties cannot address each other: the
 * applicant has no way to discover an admin's uid, and the admin only learns the applicant's when
 * the document arrives. These tests pin both directions — the alert into the shared admin inbox,
 * and the decision back into the applicant's own.
 */
class ProviderReviewTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val applicantUid = "applicant-uid"

    private fun application(
        id: String = "provider-1",
        docs: Map<String, String> = mapOf("id" to "fsimg://a")
    ) = ProviderEntity(
        id = id,
        name = "سميرة أحمد",
        title = "ممرضة قانونية",
        area = "الحلة الجديدة",
        ownerUid = applicantUid,
        status = "PENDING_REVIEW",
        docs = docs
    )

    private fun viewModelWith(
        repository: FakeProviderRegistrationRepository,
        notifications: FakeNotificationRepository
    ) = ProviderRegistrationViewModel(
        repository = repository,
        storageRepository = null,
        notifications = notifications
    )

    @Test
    fun testFilingAnApplicationAlertsTheAdminInbox() = runTest {
        val notifications = FakeNotificationRepository()
        val viewModel = viewModelWith(FakeProviderRegistrationRepository(), notifications)

        viewModel.submitBasicInfo(
            name = "سميرة أحمد",
            title = "ممرضة قانونية",
            category = "NURSING",
            experienceYears = 6,
            priceSdg = 8000.0,
            area = "الحلة الجديدة",
            about = "خبرة في العناية المنزلية"
        )
        advanceUntilIdle()

        val adminInbox = notifications.adminInbox()
        assertEquals(1, adminInbox.size)
        assertEquals("PROVIDER", adminInbox.first().type)
        assertEquals("طلب انتساب جديد", adminInbox.first().title)
        assertFalse(adminInbox.first().read)
    }

    /** Addressed to the shared collection, because no admin uid is knowable at this point. */
    @Test
    fun testTheAdminAlertIsNotWrittenToAnyUserInbox() = runTest {
        val notifications = FakeNotificationRepository()
        val viewModel = viewModelWith(FakeProviderRegistrationRepository(), notifications)

        viewModel.submitBasicInfo("سميرة أحمد", "ممرضة", "NURSING", 6, 8000.0, "الحلة", "خبرة")
        advanceUntilIdle()

        assertEquals(NotificationEntity.SCOPE_ADMIN, notifications.adminInbox().first().scope)
        assertTrue(notifications.inboxOf(applicantUid).isEmpty())
    }

    /** The admin taps the alert to reach the application, so the id has to travel with it. */
    @Test
    fun testTheAdminAlertNamesTheApplicantAndCarriesTheProviderId() = runTest {
        val notifications = FakeNotificationRepository()
        val repository = FakeProviderRegistrationRepository()
        val viewModel = viewModelWith(repository, notifications)

        viewModel.submitBasicInfo(
            "سميرة أحمد", "ممرضة قانونية", "NURSING", 6, 8000.0, "الحلة الجديدة", "خبرة"
        )
        advanceUntilIdle()

        val alert = notifications.adminInbox().first()
        assertTrue(alert.body.contains("سميرة أحمد"))
        assertTrue(alert.body.contains("الحلة الجديدة"))
        assertEquals("provider-1", alert.providerId)
    }

    @Test
    fun testApprovalNotifiesTheApplicantAndPromotesThem() = runTest {
        val notifications = FakeNotificationRepository()
        val repository = FakeProviderRegistrationRepository(listOf(application()))
        val viewModel = viewModelWith(repository, notifications)

        viewModel.approveProvider("provider-1")
        advanceUntilIdle()

        assertEquals("ACTIVE", repository.providerById("provider-1")?.status)
        assertEquals("PROVIDER", repository.grantedRoles[applicantUid])

        val inbox = notifications.inboxOf(applicantUid)
        assertEquals(1, inbox.size)
        assertEquals("تم اعتماد طلبك ✓", inbox.first().title)
        assertEquals("provider-1", inbox.first().providerId)
    }

    @Test
    fun testRejectionRecordsTheReasonOnTheApplication() = runTest {
        val notifications = FakeNotificationRepository()
        val repository = FakeProviderRegistrationRepository(listOf(application()))
        val viewModel = viewModelWith(repository, notifications)

        viewModel.rejectProvider("provider-1", "المستندات غير واضحة")
        advanceUntilIdle()

        val rejected = repository.providerById("provider-1")
        assertEquals("REJECTED", rejected?.status)
        assertEquals("المستندات غير واضحة", rejected?.reviewNote)
        assertFalse(rejected?.isVerified ?: true)
    }

    /** A refusal is only actionable if the applicant is told what to fix. */
    @Test
    fun testRejectionDeliversTheReasonToTheApplicant() = runTest {
        val notifications = FakeNotificationRepository()
        val repository = FakeProviderRegistrationRepository(listOf(application()))
        val viewModel = viewModelWith(repository, notifications)

        viewModel.rejectProvider("provider-1", "التسجيل المهني منتهي")
        advanceUntilIdle()

        val inbox = notifications.inboxOf(applicantUid)
        assertEquals(1, inbox.size)
        assertTrue(inbox.first().body.contains("التسجيل المهني منتهي"))
    }

    @Test
    fun testRejectionWithoutAReasonStillSendsAUsableMessage() = runTest {
        val notifications = FakeNotificationRepository()
        val repository = FakeProviderRegistrationRepository(listOf(application()))
        val viewModel = viewModelWith(repository, notifications)

        viewModel.rejectProvider("provider-1", "")
        advanceUntilIdle()

        val body = notifications.inboxOf(applicantUid).first().body
        assertTrue(body.contains("تواصل مع الإدارة"))
        assertFalse(body.contains("السبب:"))
    }

    /** Rejection must not promote anyone; the applicant keeps the role they had. */
    @Test
    fun testRejectionDoesNotGrantTheProviderRole() = runTest {
        val repository = FakeProviderRegistrationRepository(listOf(application()))
        val viewModel = viewModelWith(repository, FakeNotificationRepository())

        viewModel.rejectProvider("provider-1", "الشهادة غير مكتملة")
        advanceUntilIdle()

        assertTrue(repository.grantedRoles.isEmpty())
    }

    @Test
    fun testADecidedApplicationLeavesThePendingList() = runTest {
        val repository = FakeProviderRegistrationRepository(
            listOf(application(), application(id = "provider-2"))
        )
        val viewModel = viewModelWith(repository, FakeNotificationRepository())

        viewModel.loadPendingApplications()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.pendingApplications.size)

        viewModel.rejectProvider("provider-1", "المستندات غير واضحة")
        advanceUntilIdle()

        val remaining = viewModel.uiState.value.pendingApplications
        assertEquals(1, remaining.size)
        assertEquals("provider-2", remaining.first().id)
    }

    /** A refused write must leave the list alone so the admin can see the decision did not land. */
    @Test
    fun testAFailedDecisionReportsInArabicAndKeepsTheApplication() = runTest {
        val repository = FakeProviderRegistrationRepository(listOf(application()))
        val viewModel = viewModelWith(repository, FakeNotificationRepository())

        viewModel.loadPendingApplications()
        advanceUntilIdle()

        repository.failNextWrite = true
        viewModel.approveProvider("provider-1")
        advanceUntilIdle()

        assertEquals("تعذر اعتماد مقدم الخدمة", viewModel.uiState.value.errorMessage)
        assertEquals(1, viewModel.uiState.value.pendingApplications.size)
        assertEquals("PENDING_REVIEW", repository.providerById("provider-1")?.status)
    }

    /** An undelivered inbox write must not undo a decision that already committed. */
    @Test
    fun testADecisionSurvivesAFailedNotification() = runTest {
        val repository = FakeProviderRegistrationRepository(
            listOf(application().copy(ownerUid = ""))
        )
        val viewModel = viewModelWith(repository, FakeNotificationRepository())

        viewModel.approveProvider("provider-1")
        advanceUntilIdle()

        assertEquals("ACTIVE", repository.providerById("provider-1")?.status)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
