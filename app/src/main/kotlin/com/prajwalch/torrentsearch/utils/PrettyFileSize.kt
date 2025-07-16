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

fun prettySizeToBytes(prettySize: String): Float {
    val (value, unit) = prettySize.split(' ', limit = 2)
    val valueFloat = value.toFloatOrNull() ?: return 0f

    return when (unit) {
        "PB", "PiB" -> valueFloat * PB
        "TB", "TiB" -> valueFloat * TB
        "GB", "GiB" -> valueFloat * GB
        "MB", "MiB" -> valueFloat * MB
        "KB", "KiB" -> valueFloat * KB
        else -> 0f
    }
}