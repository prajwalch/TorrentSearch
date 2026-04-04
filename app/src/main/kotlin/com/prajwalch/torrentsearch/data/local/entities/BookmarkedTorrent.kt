package com.prajwalch.torrentsearch.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent

import kotlinx.serialization.Serializable

@Entity(
    tableName = "bookmarks",
    indices = [Index("name", unique = true)],
)
@Serializable
data class BookmarkedTorrent(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val infoHash: String,
    val name: String,
    val size: String,
    val seeders: Int,
    val peers: Int,
    val providerName: String,
    val uploadDate: String,
    val category: String,
    val descriptionPageUrl: String,
    @ColumnInfo(defaultValue = "NULL")
    val magnetUri: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val fileDownloadLink: String? = null,
)

fun BookmarkedTorrent.toDomain() =
    Torrent(
        infoHash = this.infoHash,
        name = this.name,
        size = this.size,
        seeders = this.seeders.toUInt(),
        peers = this.peers.toUInt(),
        providerName = this.providerName,
        uploadDate = this.uploadDate,
        category = if (this.category.isNotEmpty()) {
            Category.valueOf(this.category)
        } else {
            null
        },
        descriptionPageUrl = this.descriptionPageUrl,
        magnetUri = this.magnetUri,
        fileDownloadLink = this.fileDownloadLink,
    )

fun Torrent.toEntity() =
    BookmarkedTorrent(
        infoHash = this.infoHash,
        name = this.name,
        size = this.size,
        seeders = this.seeders.toInt(),
        peers = this.peers.toInt(),
        providerName = this.providerName,
        uploadDate = this.uploadDate,
        category = this.category?.name ?: "",
        descriptionPageUrl = this.descriptionPageUrl,
        magnetUri = this.magnetUri,
        fileDownloadLink = this.fileDownloadLink,
    )

fun List<BookmarkedTorrent>.toDomain() = this.map { it.toDomain() }