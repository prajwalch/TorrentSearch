package com.prajwalch.torrentsearch.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a viewed torrent.
 * Stores the torrent's unique ID and the timestamp when it was viewed.
 */
@Entity(tableName = "viewed_torrents")
data class ViewedTorrentEntity(
    /** Unique identifier of the torrent (UUID generated from infoHash). */
    @PrimaryKey val id: String,
    /** Timestamp when the torrent was viewed. */
    val viewedAt: Long = System.currentTimeMillis()
)
