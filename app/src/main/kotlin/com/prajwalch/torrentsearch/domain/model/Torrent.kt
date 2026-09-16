package com.prajwalch.torrentsearch.domain.model

import java.time.Instant

/** Metadata information of a torrent */
data class Torrent(
    /** Unique ID of the torrent. */
    val id: String,
    /** Name of the torrent. */
    val name: String,
    /** Torrent size (in pretty format). */
    val size: String? = null,
    /** Number of seeders. */
    val seeders: UInt? = null,
    /** Number of peers. */
    val peers: UInt? = null,
    /** Name of the search provider from where torrent is searched. */
    val providerName: String,
    /** Torrent upload date. */
    val uploadDate: Instant? = null,
    /** Category of the torrent. */
    val category: Category? = null,
    /** URL of the page where the torrent details is available. */
    val descriptionPageUrl: String? = null,
    /** Magnet URI state. */
    val magnetUri: MagnetUri,
    /**
     * A URL from where .torrent file can be downloaded.
     */
    val fileDownloadLink: String? = null,
) {
    /** Indicates if this torrent is NSFW (Not Safe For Work). */
    val isNSFW get() = category?.isNSFW ?: false

    /** Indicates if this torrent is dead. */
    val isDead get() = (seeders == 0u && peers == 0u)
}

sealed interface MagnetUri {
    data class Available(val value: String) : MagnetUri

    data class RequiresFetch(val url: String) : MagnetUri
}