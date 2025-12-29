package com.prajwalch.torrentsearch.utils

import android.os.Build
import androidx.annotation.RequiresApi

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class TorrentSearchDateFormatter private constructor(
    formatter: DateFormatter,
) : DateFormatter by formatter {
    companion object {
        /** Initializes appropriate date formatter for current Android version. */
        fun init(): TorrentSearchDateFormatter {
            val formatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ModernDateFormatter()
            } else {
                LegacyDateFormatter()
            }

            return TorrentSearchDateFormatter(formatter = formatter)
        }
    }
}

/** Different date formats used to either format or parse date. */
private object DateFormats {
    /** The date output format. */
    const val OUTPUT_FORMAT = "dd MMM yyyy"

    /** ISO-8601 variant format without timestamp. */
    const val YEAR_MONTH_DAY_HYPHEN_SEPARATED = "yyyy-M-d"

    const val DAY_MONTH_YEAR = "d/M/yyyy"

    const val MONTH_DAY_YEAR = "M/d/yyyy"

    /** Format used to parse ISO-8601 date in legacy date formatter. */
    const val LEGACY_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

    /** Format used to parse RFC-1123 date in legacy date formatter. */
    const val LEGACY_RFC_1123 = "EEE, dd MMM yyyy HH:mm:ss z"
}

/** The common interface for both legacy and modern date formatters. */
private interface DateFormatter {
    fun formatEpochSecond(epochSecond: Long): String

    fun formatYearMonthDay(date: String): String

    fun formatDayMonthYear(date: String): String

    fun formatMonthDayYear(date: String): String

    /**
     * Parses ISO 8601 date (e.g., `2025-06-11T06:13:57+00:00`) into a more
     * readable format.
     *
     * @return Date formatted as [DateFormats.OUTPUT_FORMAT], e.g., `"11 Jun 2025"`.
     */
    fun formatIsoDate(date: String): String

    fun formatRFC1123Date(date: String): String

    fun formatTodayDate(): String

    fun formatYesterdayDate(): String
}

/** The date formatter for Android version >= 8.0. */
@RequiresApi(Build.VERSION_CODES.O)
private class ModernDateFormatter : DateFormatter {
    private val outputFormatter = DateTimeFormatter.ofPattern(DateFormats.OUTPUT_FORMAT)

    override fun formatEpochSecond(epochSecond: Long): String {
        val instant = Instant.ofEpochSecond(epochSecond)

        val zoneId = ZoneId.systemDefault()
        val zonedDateTime = instant.atZone(zoneId)

        return zonedDateTime.format(outputFormatter)
    }

    override fun formatYearMonthDay(date: String): String {
        return formatDate(pattern = DateFormats.YEAR_MONTH_DAY_HYPHEN_SEPARATED, date = date)
    }

    override fun formatDayMonthYear(date: String): String {
        return formatDate(pattern = DateFormats.DAY_MONTH_YEAR, date = date)
    }

    override fun formatMonthDayYear(date: String): String {
        return formatDate(pattern = DateFormats.MONTH_DAY_YEAR, date = date)
    }

    override fun formatIsoDate(date: String): String {
        val date = OffsetDateTime.parse(date)
        return outputFormatter.format(date)
    }

    override fun formatRFC1123Date(date: String): String {
        val inputFormatter = DateTimeFormatter.RFC_1123_DATE_TIME
        val parsedDate = LocalDate.parse(date, inputFormatter) ?: return date

        return parsedDate.format(outputFormatter)
    }

    override fun formatTodayDate(): String {
        return LocalDate.now().format(outputFormatter)
    }

    override fun formatYesterdayDate(): String {
        return LocalDate.now().minusDays(1L).format(outputFormatter)
    }

    private fun formatDate(pattern: String, date: String): String {
        val inputFormatter = DateTimeFormatter.ofPattern(pattern)
        val localDate = LocalDate.parse(date, inputFormatter) ?: return date

        return localDate.format(outputFormatter)
    }
}

/** The date formatter for Android version < 8.0. */
private class LegacyDateFormatter : DateFormatter {
    private val outputFormatter = SimpleDateFormat(DateFormats.OUTPUT_FORMAT, Locale.getDefault())

    override fun formatEpochSecond(epochSecond: Long): String {
        return outputFormatter.format(epochSecond * 1000L)
    }

    override fun formatYearMonthDay(date: String): String {
        return formatDate(pattern = DateFormats.YEAR_MONTH_DAY_HYPHEN_SEPARATED, date = date)
    }

    override fun formatDayMonthYear(date: String): String {
        return formatDate(pattern = DateFormats.DAY_MONTH_YEAR, date = date)
    }

    override fun formatMonthDayYear(date: String): String {
        return formatDate(pattern = DateFormats.MONTH_DAY_YEAR, date = date)
    }

    override fun formatIsoDate(date: String): String {
        return formatDate(pattern = DateFormats.LEGACY_ISO_8601, date = date)
    }

    override fun formatRFC1123Date(date: String): String {
        return formatDate(pattern = DateFormats.LEGACY_RFC_1123, date = date)
    }

    override fun formatTodayDate(): String {
        val todayCalender = Calendar.getInstance()
        todayCalender.set(Calendar.HOUR_OF_DAY, 0)

        val todayDate = todayCalender.time

        return outputFormatter.format(todayDate)
    }

    override fun formatYesterdayDate(): String {
        val todayCalendar = Calendar.getInstance()
        todayCalendar.add(Calendar.DATE, -1)

        val yesterdayDate = todayCalendar.time

        return outputFormatter.format(yesterdayDate)
    }

    private fun formatDate(pattern: String, date: String): String {
        val inputFormatter = SimpleDateFormat(pattern, Locale.getDefault())
        val date = inputFormatter.parse(date) ?: return date

        return outputFormatter.format(date)
    }
}