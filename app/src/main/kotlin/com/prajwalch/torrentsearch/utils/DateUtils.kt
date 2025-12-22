package com.prajwalch.torrentsearch.utils

object DateUtils {
    private val dateFormatter = TorrentSearchDateFormatter.init()

    fun formatEpochSecond(epochSecond: Long) = dateFormatter.formatEpochSecond(epochSecond)

    fun formatYearMonthDay(date: String) = dateFormatter.formatYearMonthDay(date)

    fun formatDayMonthYear(date: String) = dateFormatter.formatDayMonthYear(date)

    fun formatMonthDayYear(date: String) = dateFormatter.formatMonthDayYear(date)

    fun formatIsoDate(date: String) = dateFormatter.formatIsoDate(date)

    fun formatTodayDate() = dateFormatter.formatTodayDate()

    fun formatYesterdayDate() = dateFormatter.formatYesterdayDate()
}