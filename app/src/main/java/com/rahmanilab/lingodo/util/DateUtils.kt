package com.rahmanilab.lingodo.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Small time helpers. Day boundaries always use the device's local time zone so that streaks and
 * "today" counters line up with what the user sees on the clock.
 */
object DateUtils {

    fun zone(): ZoneId = ZoneId.systemDefault()

    fun today(): LocalDate = LocalDate.now(zone())

    fun epochDay(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate().toEpochDay()

    fun todayEpochDay(): Long = today().toEpochDay()

    fun startOfToday(): Long =
        today().atStartOfDay(zone()).toInstant().toEpochMilli()

    fun startOfDaysAgo(days: Long): Long =
        today().minusDays(days).atStartOfDay(zone()).toInstant().toEpochMilli()

    /**
     * A compact, human friendly description of a future interval, used to label the four review
     * buttons (e.g. "10 min", "1 d", "3 mo").
     */
    fun formatInterval(millisFromNow: Long): String {
        if (millisFromNow <= 0) return "now"
        val minutes = millisFromNow / 60_000.0
        if (minutes < 1) return "<1 min"
        if (minutes < 60) return "${minutes.toInt()} min"
        val hours = minutes / 60.0
        if (hours < 24) return "${hours.toInt()} h"
        val days = hours / 24.0
        if (days < 30) return "${days.toInt()} d"
        val months = days / 30.0
        if (months < 12) return "${months.toInt()} mo"
        val years = days / 365.0
        val rounded = (years * 10).toInt() / 10.0
        return "$rounded y"
    }
}
