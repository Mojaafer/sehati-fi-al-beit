package com.example.ui.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CareLoopViewModelTest {
    @Test
    fun newStateNeedsCheckInAndWeeklyReview() {
        val state = CareLoopUiState()

        assertFalse(state.checkedInToday)
        assertTrue(state.weeklyReviewDue)
        assertTrue(state.remindersEnabled)
    }

    @Test
    fun recentCheckInIsMarkedForToday() {
        val state = CareLoopUiState(
            lastCheckIn = WellbeingChoice.WELL,
            lastCheckInAt = System.currentTimeMillis()
        )

        assertTrue(state.checkedInToday)
        assertTrue(state.lastCheckIn == WellbeingChoice.WELL)
    }

    @Test
    fun weeklyReviewCompletedTodayIsNotDue() {
        val state = CareLoopUiState(weeklyReviewCompletedAt = System.currentTimeMillis())

        assertFalse(state.weeklyReviewDue)
    }
}
