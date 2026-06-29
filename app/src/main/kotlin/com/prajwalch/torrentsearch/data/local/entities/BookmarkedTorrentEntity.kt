package com.prajwalch.torrentsearch.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "bookmarks")
data class BookmarkedTorrentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val infoHash: String,
    val name: String,
    @ColumnInfo(defaultValue = "NULL")
    val size: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val seeders: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val peers: Int? = null,
    val providerName: String,
    @ColumnInfo(defaultValue = "NULL")
    val uploadDate: Long? = null,
    @ColumnInfo(defaultValue = "NULL")
    val category: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val descriptionPageUrl: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val magnetUri: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val fileDownloadLink: String? = null,
)