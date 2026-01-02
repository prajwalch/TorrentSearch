package com.prajwalch.torrentsearch.domain.models

/** Represents a magnet URI. */
typealias MagnetUri = String

/** Metadata information of a torrent */
data class Torrent(
    val id: Long = 0,
    /** Name of the torrent. */
    val name: String,
    /** Torrent size (in pretty format). */
    val size: String,
    /** Number of seeders. */
    val seeders: UInt,
    /** Number of peers. */
    val peers: UInt,
    /** Name of the search provider from where torrent is searched. */
    val providerName: String,
    /** Torrent upload date (in pretty format). */
    val uploadDate: String,
    /** Category of the torrent. */
    val category: Category? = null,
    /** URL of the page where the torrent details is available. */
    val descriptionPageUrl: String,
    /**
     * Either info hash or magnet URI of the torrent.
     *
     * Either of them is required for download functionality but not both.
     */
    private val infoHashOrMagnetUri: InfoHashOrMagnetUri,
) {
    /** Returns `true` if this torrent is NSFW (Not Safe For Work). */
    fun isNSFW() = category?.isNSFW ?: true

    /** Returns `true` if this torrent is dead. */
    fun isDead() = (seeders == 0u && peers == 0u)

    /** Returns the magnet URI of this torrent. */
    fun magnetUri(): MagnetUri = when (infoHashOrMagnetUri) {
        is InfoHashOrMagnetUri.InfoHash -> createMagnetUri(infoHashOrMagnetUri.hash)
        is InfoHashOrMagnetUri.MagnetUri -> infoHashOrMagnetUri.uri
    }
}

/** Search category. */
enum class Category(val isNSFW: Boolean = false) {
    All,
    Anime,
    Apps,
    Books,
    Games,
    Movies,
    Music,
    Porn(isNSFW = true),
    Series,
    Other(isNSFW = true),
}

/** Represents either a info hash or magnet URI. */
sealed class InfoHashOrMagnetUri {
    data class InfoHash(val hash: String) : InfoHashOrMagnetUri()
    data class MagnetUri(val uri: String) : InfoHashOrMagnetUri()
}

/**
 * A list of public trackers to use when creating a magnet URI.
 *
 * Taken from:
 *   https://raw.githubusercontent.com/ngosang/trackerslist/master/trackers_best.txt (updated daily)
 */
private val PublicTrackers = listOf(
    "udp://tracker.opentrackr.org:1337/announce",
    "udp://open.demonoid.ch:6969/announce",
    "udp://open.demonii.com:1337/announce",
    "udp://open.stealth.si:80/announce",
    "udp://exodus.desync.com:6969/announce",
    "udp://tracker.torrent.eu.org:451/announce",
    "udp://explodie.org:6969/announce",
    "udp://tracker2.dler.org:80/announce",
    "udp://tracker.qu.ax:6969/announce",
    "udp://tracker.filemail.com:6969/announce",
    "udp://tracker.dler.org:6969/announce",
    "udp://tracker.bittor.pw:1337/announce",
    "udp://tracker.0x7c0.com:6969/announce",
    "udp://tracker-udp.gbitt.info:80/announce",
    "udp://run.publictracker.xyz:6969/announce",
    "udp://retracker01-msk-virt.corbina.net:80/announce",
    "udp://p4p.arenabg.com:1337/announce",
    "udp://opentracker.io:6969/announce",
    "udp://open.tracker.cl:1337/announce",
    "udp://leet-tracker.moe:1337/announce",
)

/** Public trackers params formatted in a single string. */
private val PublicTrackersParams = PublicTrackers.joinToString(separator = "&") { "tr=$it" }

private fun createMagnetUri(infoHash: String): MagnetUri {
    return "magnet:?xt=urn:btih:${infoHash}&$PublicTrackersParams"
}