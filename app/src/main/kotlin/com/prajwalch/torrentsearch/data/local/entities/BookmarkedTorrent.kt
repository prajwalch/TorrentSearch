package com.prajwalch.torrentsearch.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    indices = [Index("name", unique = true)],
)
data class BookmarkedTorrent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val size: String,
    val seeders: Int,
    val peers: Int,
    val providerId: String,
    val providerName: String,
    val uploadDate: String,
    val category: String,
    val descriptionPageUrl: String,
    val magnetUri: String,
)