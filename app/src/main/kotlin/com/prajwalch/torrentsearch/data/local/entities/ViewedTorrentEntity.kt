package com.prajwalch.torrentsearch.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a viewed torrent.
 */
@Entity(tableName = "viewed_torrents")
data class ViewedTorrentEntity(
    /** The unique info hash of the torrent */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val infoHash: String,
    /** Timestamp when the torrent was viewed. */
    val viewedAt: Long = System.currentTimeMillis(),
)