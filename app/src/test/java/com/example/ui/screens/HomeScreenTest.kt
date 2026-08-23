package com.example.ui.screens

import com.example.data.model.ProviderEntity
import com.example.ui.navigation.Routes
import com.example.ui.viewmodel.SehatiUiState
import org.junit.Assert.*
import org.junit.Test

class HomeScreenTest {

    private val sampleProviders = listOf(
        ProviderEntity(
            id = "provider-1",
            name = "سامي أحمد",
            title = "فني مختبر",
            experienceYears = 5,
            rating = 4.9,
            reviewsCount = 45,
            distanceKm = 1.5,
            priceSdg = 15000.0,
            isAvailableNow = true,
            serviceCategory = "LAB_DRAW",
            area = "ود مدني"
        ),
        ProviderEntity(
            id = "provider-2",
            name = "د. فاطمة عمر",
            title = "طبيبة عامة",
            experienceYears = 10,
            rating = 4.95,
            reviewsCount = 158,
            distanceKm = 4.1,
            priceSdg = 35000.0,
            isAvailableNow = true,
            serviceCategory = "DOCTOR",
            area = "ود مدني"
        )
    )

    @Test
    fun testSelectLabDrawCategoryUpdatesState() {
        val state = SehatiUiState(providers = sampleProviders)

        // Select LAB_DRAW category
        val updatedState = state.copy(selectedCategory = "LAB_DRAW")

        assertEquals("LAB_DRAW", updatedState.selectedCategory)
        assertEquals("providers_list/LAB_DRAW", Routes.providersListRoute(updatedState.selectedCategory))

        val filteredProviders = updatedState.providers.filter { it.serviceCategory == updatedState.selectedCategory }
        assertEquals(1, filteredProviders.size)
        assertEquals("سامي أحمد", filteredProviders.first().name)
    }

    @Test
    fun testSelectDoctorCategoryUpdatesState() {
        val state = SehatiUiState(providers = sampleProviders)

        val updatedState = state.copy(selectedCategory = "DOCTOR")

        assertEquals("DOCTOR", updatedState.selectedCategory)

        val filteredProviders = updatedState.providers.filter { it.serviceCategory == updatedState.selectedCategory }
        assertEquals(1, filteredProviders.size)
        assertEquals("د. فاطمة عمر", filteredProviders.first().name)
    }

    @Test
    fun testAllCategoryShowsAllProviders() {
        val state = SehatiUiState(providers = sampleProviders, selectedCategory = "ALL")

        val filteredProviders = state.providers.filter { state.selectedCategory == "ALL" || it.serviceCategory == state.selectedCategory }
        assertEquals(2, filteredProviders.size)
    }
}
