package com.prajwalch.torrentsearch.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {
    /** The date output format. */
    const val OUTPUT_FORMAT = "dd MMM yyyy"
    
    private val outputFormatter = DateTimeFormatter.ofPattern(OUTPUT_FORMAT)

    fun formatEpochSecond(epochSecond: Long): String {
        val instant = Instant.ofEpochSecond(epochSecond)

        val zoneId = ZoneId.systemDefault()
        val zonedDateTime = instant.atZone(zoneId)

        return zonedDateTime.format(outputFormatter)
    }
}