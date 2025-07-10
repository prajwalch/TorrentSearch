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
    /**
     * Category of the torrent.
     *
     * NOTE: The nullable is made only to support TorrentsCsv.
     * TorrentsCsv doesn't return any category.
     */
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
    /** Returns the magnet URI of this torrent. */
    fun magnetUri(): MagnetUri = when (infoHashOrMagnetUri) {
        is InfoHashOrMagnetUri.InfoHash -> createMagnetUri(infoHashOrMagnetUri.hash)
        is InfoHashOrMagnetUri.MagnetUri -> infoHashOrMagnetUri.uri
    }

}

/** Search category. */
enum class Category {
    All,
    Anime,
    Apps,
    Books,
    Games,
    Movies,
    Music,
    Porn,
    Series,
    Other,
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
//   https://raw.githubusercontent.com/ngosang/trackerslist/master/trackers_best.txt (updated daily) (https://github.com/ngosang/trackerslist/blob/master/trackers_best.txt)
private val trackers = listOf(
    "udp://1c.premierzal.ru:6969/announce",
    "udp://9.rarbg.me:2970/announce",
    "udp://bandito.byterunner.io:6969/announce",
    "udp://bittorrent-tracker.e-n-c-r-y-p-t.net:1337/announce",
    "udp://bt.ktrackers.com:6666/announce",
    "udp://bt.rer.lol:2710/announce",
    "udp://bt.rer.lol:6969/announce",
    "udp://concen.org:6969/announce",
    "udp://d40969.acod.regrucolo.ru:6969/announce",
    "udp://evan.im:6969/announce",
    "udp://exodus.desync.com:6969/announce",
    "udp://explodie.org:6969/announce",
    "udp://fosstorrents.com:6969/announce",
    "udp://ipv4.rer.lol:2710/announce",
    "udp://ipv4.tracker.harry.lu:80/announce",
    "udp://ipv4announce.sktorrent.eu:6969/announce",
    "udp://isk.richardsw.club:6969/announce",
    "udp://leet-tracker.moe:1337/announce",
    "udp://martin-gebhardt.eu:25/announce",
    "udp://ns-1.x-fins.com:6969/announce",
    "udp://open.demonii.com:1337/announce",
    "udp://open.dstud.io:6969/announce",
    "udp://open.free-tracker.ga:6969/announce",
    "udp://open.stealth.si:80/announce",
    "udp://opentracker.io:6969/announce",
    "udp://p2p.publictracker.xyz:6969/announce",
    "udp://p4p.arenabg.ch:1337/announce",
    "udp://p4p.arenabg.com:1337/announce",
    "udp://public.tracker.vraphim.com:6969/announce",
    "udp://retracker.lanta.me:2710/announce",
    "udp://retracker.lanta-net.ru:2710/announce",
    "udp://retracker01-msk-virt.corbina.net:80/announce",
    "udp://t.overflow.biz:6969/announce",
    "udp://torrent.gresille.org:80/announce",
    "udp://tr4ck3r.duckdns.org:6969/announce",
    "udp://tracker.coppersurfer.tk:6969",
    "udp://tracker.ddunlimited.net:6969/announce",
    "udp://tracker.dler.org:6969/announce",
    "udp://tracker.dump.cl:6969/announce",
    "udp://tracker.filemail.com:6969/announce",
    "udp://tracker.fnix.net:6969/announce",
    "udp://tracker.gigantino.net:6969/announce",
    "udp://tracker.gmi.gd:6969/announce",
    "udp://tracker.internetwarriors.net:1337/announce",
    "udp://tracker.leechers-paradise.org:6969",
    "udp://tracker.ololosh.space:6969/announce",
    "udp://tracker.openbittorrent.com:6969/announce",
    "udp://tracker.opentrackr.org:1337/announce",
    "udp://tracker.srv00.com:6969/announce",
    "udp://tracker.theoks.net:6969/announce",
    "udp://tracker.therarbg.to:6969/announce",
    "udp://tracker.tiny-vps.com:6969/announce",
    "udp://tracker.torrent.eu.org:451/announce",
    "udp://tracker.torrust-demo.com:6969/announce",
    "udp://tracker.tryhackx.org:6969/announce",
    "udp://tracker2.dler.org:80/announce",
    "udp://tracker-udp.gbitt.info:80/announce",
    "udp://ttk2.nbaonlineservice.com:6969/announce",
    "udp://wepzone.net:6969/announce",
    "udp://www.torrent.eu.org:451/announce",
)

private fun createMagnetUri(infoHash: String): MagnetUri {
    // For example: &tr=<trackerurl>&tr=<tracker2url>
    val formattedTrackers = trackers.joinToString(
        separator = "&tr=",
    )
    return "magnet:?xt=urn:btih:${infoHash}&tr=$formattedTrackers"
}
