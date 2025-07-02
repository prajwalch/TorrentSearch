package com.prajwalch.torrentsearch.models

/** Represents a magnet URI. */
typealias MagnetUri = String

/** Metadata information of a torrent */
data class Torrent(
    val name: String = "UNKNOWN",
    val hash: String = "0000",
    val size: FileSize = FileSize(),
    val seeds: UInt = 0U,
    val peers: UInt = 0U,
    val providerName: String = "default",
    val uploadDate: String = "0000-00-00",
) {
    /** Constructs and return the magnet URL of this torrent */
    fun magnetUri(): MagnetUri {
        // List of trackers to use in the magnet link.
        //
        // Taken from:
        //   https://yts.mx/api
        //   https://github.com/qbittorrent/search-plugins/blob/master/nova3/engines/torrentscsv.py#L41
        val trackers = listOf(
            "udp://tracker.internetwarriors.net:1337/announce",
            "udp://tracker.opentrackr.org:1337/announce",
            "udp://tracker.openbittorrent.com:6969/announce",
            "udp://tracker.torrent.eu.org:451/announce",
            "udp://tracker.tiny-vps.com:6969/announce",
            "udp://tracker.dler.org:6969/announce",
            "udp://tracker.coppersurfer.tk:6969",
            "udp://tracker.leechers-paradise.org:6969",
            "udp://p4p.arenabg.ch:1337/announce",
            "udp://www.torrent.eu.org:451/announce",
            "udp://retracker.lanta-net.ru:2710/announce",
            "udp://open.stealth.si:80/announce",
            "udp://exodus.desync.com:6969/announce",
            "udp://9.rarbg.me:2970/announce",
            "udp://ipv4.tracker.harry.lu:80/announce",
            "udp://torrent.gresille.org:80/announce",
        )
        // For example: &tr=<trackerurl>&tr=<tracker2url>
        val formattedTrackers = trackers.joinToString(
            separator = "&tr=",
        )
        return "magnet:?xt=urn:btih:${this.hash}&tr=$formattedTrackers"
    }
}

data class FileSize(val value: Float = 0.0F, val unit: String = "B") {
    companion object {
        const val KB: Float = 1024.0F
        const val MB: Float = KB * 1024.0F
        const val GB: Float = MB * 1024.0F
        const val TB: Float = GB * 1024.0F
        const val PB: Float = TB * 1024.0F

        fun fromBytes(bytes: Float): FileSize = when {
            bytes >= PB -> FileSize(bytes / PB, "PB")
            bytes >= TB -> FileSize(bytes / TB, "TB")
            bytes >= GB -> FileSize(bytes / GB, "GB")
            bytes >= MB -> FileSize(bytes / MB, "MB")
            bytes >= KB -> FileSize(bytes / KB, "KB")
            else -> FileSize(0.0F, "B")
        }

        fun fromString(str: String): FileSize {
            return fromBytes(str.toFloatOrNull() ?: 0.0F)
        }
    }

    override fun toString(): String {
        return "${"%.2f".format(value)} $unit"
    }
}