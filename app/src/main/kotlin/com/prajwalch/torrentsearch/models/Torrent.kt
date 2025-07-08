package com.prajwalch.torrentsearch.models

/** Represents a magnet URI. */
typealias MagnetUri = String

/** Metadata information of a torrent */
data class Torrent(
    /** Name of the torrent. */
    val name: String,
    /** Torrent size (in pretty format). */
    val size: String,
    /** Number of seeders. */
    val seeds: UInt,
    /** Number of peers/leechers. */
    val peers: UInt,
    /** Name of the provider/source from where torrent is extracted. */
    val providerName: String,
    /** Torrent upload date (in pretty format). */
    val uploadDate: String,
    /** URL of the page where the torrent details is available. */
    val descriptionPageUrl: String,
    /**
     * Either info hash or magnet URI of the torrent.
     *
     * Either of them is required for download functionality but not both.
     */
    private val infoHashOrMagnetUri: InfoHashOrMagnetUri,
) {
    /** Returns the magnet URI of this torrent. */
    fun magnetUri(): MagnetUri = when (infoHashOrMagnetUri) {
        is InfoHashOrMagnetUri.InfoHash -> createMagnetUri(infoHashOrMagnetUri.hash)
        is InfoHashOrMagnetUri.MagnetUri -> infoHashOrMagnetUri.uri
    }

}

/** Represents either a info hash or magnet URI. */
sealed class InfoHashOrMagnetUri {
    data class InfoHash(val hash: String) : InfoHashOrMagnetUri()
    data class MagnetUri(val uri: String) : InfoHashOrMagnetUri()
}

// List of trackers to use in the magnet link.
//
// Taken from:
//   https://yts.mx/api
//   https://github.com/qbittorrent/search-plugins/blob/master/nova3/engines/torrentscsv.py#L41
private val trackers = listOf(
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

private fun createMagnetUri(infoHash: String): MagnetUri {
    // For example: &tr=<trackerurl>&tr=<tracker2url>
    val formattedTrackers = trackers.joinToString(
        separator = "&tr=",
    )
    return "magnet:?xt=urn:btih:${infoHash}&tr=$formattedTrackers"
}