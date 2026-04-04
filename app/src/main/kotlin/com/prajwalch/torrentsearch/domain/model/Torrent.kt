package com.prajwalch.torrentsearch.domain.model

import com.prajwalch.torrentsearch.util.TorrentUtils

/** Represents a magnet URI. */
typealias MagnetUri = String

/** Metadata information of a torrent */
data class Torrent(
    /** The unique info hash of the torrent. */
    val infoHash: String,
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
    /** The magnet URI of the torrent. */
    val magnetUri: String? = null,
    /**
     * A URL from where .torrent file can be downloaded.
     */
    val fileDownloadLink: String? = null,
) {
    /** Returns `true` if this torrent is NSFW (Not Safe For Work). */
    fun isNSFW() = category?.isNSFW ?: true

    /** Returns `true` if this torrent is dead. */
    fun isDead() = (seeders == 0u && peers == 0u)

    fun magnetUri(): String = magnetUri ?: TorrentUtils.createMagnetUri(infoHash)
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