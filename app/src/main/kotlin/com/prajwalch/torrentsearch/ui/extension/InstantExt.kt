package com.prajwalch.torrentsearch.ui.extension

import android.text.format.DateUtils
import java.time.Instant

fun Instant.toDisplayDate(): String {
    return DateUtils.getRelativeTimeSpanString(
        /* time = */
        this.toEpochMilli(),
        /* now = */
        System.currentTimeMillis(),
        /* minResolution = */
        DateUtils.MINUTE_IN_MILLIS,
        /* flags = */
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}