package com.example.data.model

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * The booking screen used to offer three fixed strings — "اليوم"، "غداً"، "الخميس" — so an order
 * placed on a Monday claimed a Thursday slot, and a patient booking at 10pm could still pick
 * 6pm the same evening. These pin the derived behaviour.
 */
class VisitScheduleTest {

    private val khartoum: TimeZone = TimeZone.getTimeZone("Africa/Khartoum")

    /** Sunday 2026-08-09 at the given local hour. */
    private fun sundayAt(hour: Int): Long = Calendar.getInstance(khartoum).apply {
        clear()
        set(2026, Calendar.AUGUST, 9, hour, 0, 0)
    }.timeInMillis

    @Test
    fun testFirstTwoDaysReadAsTodayAndTomorrow() {
        val options = VisitSchedule.dateOptions(sundayAt(9), timeZone = khartoum)

        assertEquals("اليوم", options[0])
        assertEquals("غداً", options[1])
    }

    @Test
    fun testLaterDaysCarryTheRealWeekdayAndDate() {
        val options = VisitSchedule.dateOptions(sundayAt(9), days = 4, timeZone = khartoum)

        // Sunday + 2 is Tuesday the 11th, + 3 is Wednesday the 12th.
        assertEquals("الثلاثاء 11 أغسطس", options[2])
        assertEquals("الأربعاء 12 أغسطس", options[3])
    }

    @Test
    fun testDateOptionsRollOverIntoTheNextMonth() {
        val endOfMonth = Calendar.getInstance(khartoum).apply {
            clear()
            set(2026, Calendar.AUGUST, 30, 9, 0, 0)
        }.timeInMillis

        val options = VisitSchedule.dateOptions(endOfMonth, days = 4, timeZone = khartoum)

        assertEquals("الثلاثاء 1 سبتمبر", options[2])
    }

    @Test
    fun testMorningBookingStillOffersTheWholeDay() {
        val slots = VisitSchedule.timeSlotsFor(dayOffset = 0, nowMillis = sundayAt(5), timeZone = khartoum)

        assertEquals(VisitSchedule.TIME_SLOTS.size, slots.size)
    }

    @Test
    fun testSlotsInsideTheLeadTimeAreNotOffered() {
        // 09:00 + 2h lead means the 8am and 10am slots are gone.
        val slots = VisitSchedule.timeSlotsFor(dayOffset = 0, nowMillis = sundayAt(9), timeZone = khartoum)

        assertFalse(slots.contains("8:00 صباحاً"))
        assertFalse(slots.contains("10:00 صباحاً"))
        assertTrue(slots.contains("12:00 ظهراً"))
    }

    @Test
    fun testLateEveningLeavesNoSlotsToday() {
        val slots = VisitSchedule.timeSlotsFor(dayOffset = 0, nowMillis = sundayAt(21), timeZone = khartoum)

        assertTrue(slots.isEmpty())
    }

    @Test
    fun testFutureDaysAreNotTrimmedByTodaysClock() {
        val slots = VisitSchedule.timeSlotsFor(dayOffset = 1, nowMillis = sundayAt(21), timeZone = khartoum)

        assertEquals(VisitSchedule.TIME_SLOTS.size, slots.size)
        assertTrue(slots.contains("8:00 صباحاً"))
    }
}
