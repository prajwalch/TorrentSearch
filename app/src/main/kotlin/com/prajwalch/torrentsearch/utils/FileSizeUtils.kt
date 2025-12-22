package com.prajwalch.torrentsearch.utils

object FileSizeUtils {
    private const val KB: Float = 1024.0f
    private const val MB: Float = KB * 1024.0f
    private const val GB: Float = MB * 1024.0f
    private const val TB: Float = GB * 1024.0f
    private const val PB: Float = TB * 1024.0f

    object FileSizeUnits {
        const val B = "B"
        const val KB = "KB"
        const val MB = "MB"
        const val GB = "GB"
        const val TB = "TB"
        const val PB = "PB"
    }

    fun formatBytes(bytes: Float): String {
        val (value, unit) = when {
            bytes >= PB -> Pair(bytes / PB, FileSizeUnits.PB)
            bytes >= TB -> Pair(bytes / TB, FileSizeUnits.TB)
            bytes >= GB -> Pair(bytes / GB, FileSizeUnits.GB)
            bytes >= MB -> Pair(bytes / MB, FileSizeUnits.MB)
            bytes >= KB -> Pair(bytes / KB, FileSizeUnits.KB)
            else -> Pair(0.0f, FileSizeUnits.B)
        }

        return "${"%.2f".format(value)} $unit"
    }

    fun formatBytes(bytes: String): String {
        return formatBytes(bytes = bytes.toFloat())
    }

    fun getBytes(formattedSize: String): Float {
        val (value, unit) = formattedSize.split(' ', limit = 2)
        val valueFloat = value.toFloatOrNull() ?: return 0f

        return when (unit) {
            FileSizeUnits.PB, "PiB" -> valueFloat * PB
            FileSizeUnits.TB, "TiB" -> valueFloat * TB
            FileSizeUnits.GB, "GiB" -> valueFloat * GB
            FileSizeUnits.MB, "MiB" -> valueFloat * MB
            FileSizeUnits.KB, "KiB" -> valueFloat * KB
            else -> 0f
        }
    }
}