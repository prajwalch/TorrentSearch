package com.prajwalch.torrentsearch.utils

import java.text.SimpleDateFormat
import java.util.Locale

/** Returns the pretty date format. */
fun prettyDate(epochSeconds: Long): String {
    val simpleDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return simpleDateFormat.format(epochSeconds * 1000L)
}