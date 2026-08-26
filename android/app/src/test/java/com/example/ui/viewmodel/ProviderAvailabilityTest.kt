package com.example.ui.viewmodel

import com.example.MainDispatcherRule
import com.example.data.model.ProviderEntity
import com.example.data.repository.FakeNotificationRepository
import com.example.data.repository.FakeSehatiRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * The availability switch is the provider's only lever over how much work reaches them, so it has
 * to survive the trip to Firestore. A toggle that only moved locally would keep the provider
 * listed as "متاح الآن" to every patient browsing the catalogue.
 */
class ProviderAvailabilityTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val providerId = "p1"

    private fun repositoryWith(available: Boolean) = FakeSehatiRepository(
        providers = listOf(
            ProviderEntity(id = providerId, name = "محمد عبدالرحمن", ownerUid = "owner-1", isAvailableNow = available)
        )
    )

    private fun viewModel(repository: FakeSehatiRepository) = SehatiViewModel(
        repository = repository,
        storageRepository = null,
        notificationRepository = FakeNotificationRepository()
    )

    @Test
    fun testTogglingAvailabilityReachesTheCatalogue() = runTest {
        val repository = repositoryWith(available = false)
        val viewModel = viewModel(repository)

        viewModel.observeOrders(UserRole.PROVIDER, "owner-1", providerId)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isProviderAvailable)

        viewModel.toggleProviderAvailability()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isProviderAvailable)
        assertTrue(repository.getProviderById(providerId)!!.isAvailableNow)
        assertFalse(viewModel.uiState.value.isSavingAvailability)
    }

    @Test
    fun testSwitchStartsFromTheStoredValueNotAnOptimisticDefault() = runTest {
        val viewModel = viewModel(repositoryWith(available = false))

        viewModel.observeOrders(UserRole.PROVIDER, "owner-1", providerId)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isProviderAvailable)
    }

    @Test
    fun testFailedWriteSnapsTheSwitchBack() = runTest {
        val repository = repositoryWith(available = true)
        repository.failAvailabilityWrites = true
        val viewModel = viewModel(repository)

        viewModel.observeOrders(UserRole.PROVIDER, "owner-1", providerId)
        advanceUntilIdle()

        viewModel.toggleProviderAvailability()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isProviderAvailable)
        assertTrue(repository.getProviderById(providerId)!!.isAvailableNow)
        assertTrue(viewModel.uiState.value.notificationMessage!!.contains("تعذر تحديث حالتك"))
    }

    @Test
    fun testCatalogueFeedCarriesAToggleMadeOnAnotherDevice() = runTest {
        val repository = repositoryWith(available = true)
        val viewModel = viewModel(repository)

        viewModel.observeOrders(UserRole.PROVIDER, "owner-1", providerId)
        viewModel.observeProviders()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isProviderAvailable)

        // Stands in for the same provider switching off from a second phone.
        repository.setProviderAvailability(providerId, false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isProviderAvailable)
    }

    @Test
    fun testDoubleTapDoesNotQueueTwoWrites() = runTest {
        val repository = repositoryWith(available = false)
        val viewModel = viewModel(repository)

        viewModel.observeOrders(UserRole.PROVIDER, "owner-1", providerId)
        advanceUntilIdle()

        viewModel.toggleProviderAvailability()
        viewModel.toggleProviderAvailability()
        advanceUntilIdle()

        // The second tap is swallowed, so the switch does not flip back to unavailable.
        assertTrue(viewModel.uiState.value.isProviderAvailable)
        assertTrue(repository.getProviderById(providerId)!!.isAvailableNow)
    }
}
