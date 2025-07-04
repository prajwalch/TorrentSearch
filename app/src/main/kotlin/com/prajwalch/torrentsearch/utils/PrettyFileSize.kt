package com.prajwalch.torrentsearch.utils

private const val KB: Float = 1024.0f
private const val MB: Float = KB * 1024.0f
private const val GB: Float = MB * 1024.0f
private const val TB: Float = GB * 1024.0f
private const val PB: Float = TB * 1024.0f

fun prettyFileSize(bytes: Float): String {
    val (value, unit) = when {
        bytes >= PB -> Pair(bytes / PB, "PB")
        bytes >= TB -> Pair(bytes / TB, "TB")
        bytes >= GB -> Pair(bytes / GB, "GB")
        bytes >= MB -> Pair(bytes / MB, "MB")
        bytes >= KB -> Pair(bytes / KB, "KB")
        else -> Pair(0.0f, "B")
    }

    return "${"%.2f".format(value)} $unit"
}

fun prettyFileSize(bytes: String): String {
    return prettyFileSize(bytes = bytes.toFloat())
}