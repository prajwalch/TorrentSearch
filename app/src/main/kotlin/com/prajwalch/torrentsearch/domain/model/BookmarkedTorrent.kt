package com.prajwalch.torrentsearch.domain.model

/**
 * Represents a [Torrent] that is bookmarked by user.
 */
data class BookmarkedTorrent(
    val id: Long,
    val torrent: Torrent,
)