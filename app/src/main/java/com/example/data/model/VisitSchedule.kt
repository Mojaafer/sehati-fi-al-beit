package com.example.data.model

import java.util.Calendar
import java.util.TimeZone

/**
 * Visit dates and times used to be three hardcoded strings — "اليوم"، "غداً"، "الخميس" and a
 * default of 8:00 مساءً — so every order carried the same fictional slot no matter when it was
 * placed, and "الخميس" was wrong on six days out of seven. These are derived from the clock
 * instead, and a slot that has already passed is not offered.
 *
 * Pure Kotlin on [Calendar] rather than java.time: minSdk is 24 and the module has no desugaring.
 */
object VisitSchedule {

    /** A provider needs warning before a home visit, so the next couple of hours are not bookable. */
    const val LEAD_TIME_HOURS = 2

    private val WEEKDAYS = arrayOf(
        "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
    )

    private val MONTHS = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    data class TimeSlot(val label: String, val hour: Int)

    val TIME_SLOTS = listOf(
        TimeSlot("8:00 صباحاً", 8),
        TimeSlot("10:00 صباحاً", 10),
        TimeSlot("12:00 ظهراً", 12),
        TimeSlot("3:00 عصراً", 15),
        TimeSlot("6:00 مساءً", 18),
        TimeSlot("8:00 مساءً", 20)
    )

    /**
     * Labels for the next [days] days. The first two read "اليوم"/"غداً" because that is how a
     * patient thinks about them; the rest carry a real weekday and date so the order is not
     * ambiguous a week later.
     */
    fun dateOptions(nowMillis: Long, days: Int = 4, timeZone: TimeZone = TimeZone.getDefault()): List<String> {
        return (0 until days).map { offset ->
            when (offset) {
                0 -> "اليوم"
                1 -> "غداً"
                else -> {
                    val day = Calendar.getInstance(timeZone).apply {
                        timeInMillis = nowMillis
                        add(Calendar.DAY_OF_YEAR, offset)
                    }
                    val weekday = WEEKDAYS[day.get(Calendar.DAY_OF_WEEK) - 1]
                    val month = MONTHS[day.get(Calendar.MONTH)]
                    "$weekday ${day.get(Calendar.DAY_OF_MONTH)} $month"
                }
            }
        }
    }

    /**
     * Slots offered for the day at [dayOffset]. Today drops anything inside [LEAD_TIME_HOURS],
     * which can leave the list empty late in the evening — the caller is expected to say so
     * rather than show a day with nothing on it.
     */
    fun timeSlotsFor(
        dayOffset: Int,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<String> {
        if (dayOffset != 0) return TIME_SLOTS.map { it.label }

        val now = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val earliest = now.get(Calendar.HOUR_OF_DAY) + LEAD_TIME_HOURS
        return TIME_SLOTS.filter { it.hour >= earliest }.map { it.label }
    }
}
