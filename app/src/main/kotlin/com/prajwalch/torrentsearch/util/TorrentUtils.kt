package com.prajwalch.torrentsearch.util

object TorrentUtils {
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

    private const val MAGNET_URI_PREFIX = "magnet:?xt=urn:btih:"

    fun getInfoHashFromMagnetUri(magnetUri: String): String {
        require(magnetUri.startsWith(MAGNET_URI_PREFIX)) {
            "Can't extract info hash from '$magnetUri'"
        }

        return magnetUri.removePrefix(MAGNET_URI_PREFIX).takeWhile { it != '&' }
    }

    fun createMagnetUri(infoHash: String): String =
        "magnet:?xt=urn:btih:${infoHash}&$PublicTrackersParams"
}