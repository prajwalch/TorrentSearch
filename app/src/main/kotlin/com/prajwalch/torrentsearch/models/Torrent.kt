package com.prajwalch.torrentsearch.models

/** Represents a magnet URI. */
typealias MagnetUri = String

/** Metadata information of a torrent */
data class Torrent(
    val name: String = "UNKNOWN",
    val hash: String = "0000",
    val size: String = "0 B",
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