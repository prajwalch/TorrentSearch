package com.prajwalch.torrentsearch.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object TorrentDateParser {
    private val RelativeTimePattern = Regex(
        """\b(\d+(?:\.\d+)?)\s*(s|sec|secs|second|seconds|m|min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days|w|wk|wks|week|weeks|mo|mos|month|months|y|yr|yrs|year|years)\b\s*(ago)?\b""",
        RegexOption.IGNORE_CASE
    )

    private val DefaultTimeZone = ZoneOffset.UTC

    fun parse(date: String, format: String): Instant? {
        val inputFormatter = DateTimeFormatter.ofPattern(format)
        
        return try {
            LocalDateTime.parse(date, inputFormatter)
                .toInstant(DefaultTimeZone)
        } catch (_: DateTimeParseException) {
            // No time component in format, fallback to date only.
            LocalDate.parse(date, inputFormatter)
                .atStartOfDay(DefaultTimeZone)
                .toInstant()
        }
    }

    fun tryParseRelative(date: String): Instant? {
        // One of the clown doesn't even display proper relative time, and instead
        // uses '1 Year+' formatting for every old torrents.
        val date = date.removeSuffix("+").trim()

        val parsed = tryParseSpecialRelative(date)
        if (parsed != null) return parsed

        val matched = RelativeTimePattern.find(date) ?: return null
        val value = matched.groupValues[1].toDoubleOrNull()?.toLong() ?: return null
        val unit = matched.groupValues[2].lowercase()
        val duration = when (unit) {
            "s", "sec", "secs", "second", "seconds" -> Duration.ofSeconds(value)
            "m", "min", "mins", "minute", "minutes" -> Duration.ofMinutes(value)
            "h", "hr", "hrs", "hour", "hours" -> Duration.ofHours(value)
            "d", "day", "days" -> Duration.ofDays(value)
            "w", "wk", "wks", "week", "weeks" -> Duration.ofDays(value * 7L)
            "mo", "mos", "month", "months" -> Duration.ofDays(value * 30L)
            "y", "yr", "yrs", "year", "years" -> Duration.ofDays(value * 365L)
            else -> return null
        }

        return Instant.now().minus(duration)
    }

    fun tryParseSpecialRelative(date: String): Instant? = when (date.lowercase()) {
        "today", "just now", "moments ago" -> Instant.now()
        "yesterday" -> LocalDate.now(DefaultTimeZone)
            .minusDays(1L)
            .atStartOfDay(DefaultTimeZone)
            .toInstant()

        "last week" -> Instant.now().minus(Duration.ofDays(7))
        "last month" -> Instant.now().minus(Duration.ofDays(30))
        "last year" -> Instant.now().minus(Duration.ofDays(365))
        else -> null
    }

    fun epochSecondToInstant(second: Long): Instant =
        Instant.ofEpochSecond(second)

    fun parseIso(date: String): Instant =
        OffsetDateTime.parse(date).toInstant()

    fun parseRFC1123(date: String): Instant {
        val inputFormatter = DateTimeFormatter.RFC_1123_DATE_TIME
        return LocalDate.parse(date, inputFormatter).atStartOfDay(DefaultTimeZone).toInstant()
    }

    fun getTodayDate(): Instant =
        LocalDate.now(DefaultTimeZone)
            .atStartOfDay(DefaultTimeZone)
            .toInstant()

    fun getYesterdayDate(): Instant =
        LocalDate.now(DefaultTimeZone)
            .minusDays(1L)
            .atStartOfDay(DefaultTimeZone)
            .toInstant()
}