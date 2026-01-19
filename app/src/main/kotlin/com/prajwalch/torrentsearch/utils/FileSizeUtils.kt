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

    fun normalizeSize(size: String): String = buildString {
        // No regex usage. Performance is important while doing in tight loops.
        for (idx in size.indices) {
            append(size[idx])

            if ((idx < (size.length - 1)) &&
                size[idx].isDigit() &&
                size[idx + 1].isLetter()
            ) {
                append(' ')
            }
        }
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
        val (value, unit) = getSizeValueAndUnit(formattedSize = formattedSize)
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

    private fun getSizeValueAndUnit(formattedSize: String): Pair<String, String> {
        // If the size is formatted using space then simply do split and
        // return the rest.
        if (formattedSize.contains(' ')) {
            val (value, unit) = formattedSize.split(' ', limit = 2)
            return Pair(value, unit)
        }

        // If not formatted with space (e.g.: 1.2MB) then we don't have a choice
        // but to do splitting manually. Regex helps but performance is important
        // for us.
        //
        // Find the index from where the unit starts and then do splitting work
        // using that index.
        val unitStartIndex = formattedSize.indexOfFirst { it.isLetter() }.takeIf { it != -1 }

        // If unit is not present then simply fallback to KiloBytes instead of
        // crashing the application or throwing an exception.
        if (unitStartIndex == null) {
            return Pair(formattedSize, FileSizeUnits.KB)
        }

        // If present then it's a simple work.
        val sizeValue = formattedSize.substring(0..<unitStartIndex)
        val sizeUnit = formattedSize.substring(unitStartIndex)

        return Pair(sizeValue, sizeUnit)
    }
}