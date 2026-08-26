package com.example.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTimeTest {

    private val now = 1_700_000_000_000L

    private fun agoMinutes(minutes: Long) = now - minutes * 60_000L
    private fun agoHours(hours: Long) = agoMinutes(hours * 60)
    private fun agoDays(days: Long) = agoHours(days * 24)

    @Test
    fun testJustNowUnderAMinute() {
        assertEquals("الآن", relativeTime(now - 30_000L, now))
    }

    @Test
    fun testMinutesAndHours() {
        assertEquals("منذ 5 دقيقة", relativeTime(agoMinutes(5), now))
        assertEquals("منذ 3 ساعة", relativeTime(agoHours(3), now))
    }

    @Test
    fun testYesterdayAndDays() {
        assertEquals("أمس", relativeTime(agoDays(1), now))
        assertEquals("منذ 4 أيام", relativeTime(agoDays(4), now))
    }

    @Test
    fun testWeeks() {
        assertEquals("منذ 2 أسابيع", relativeTime(agoDays(14), now))
    }
}
