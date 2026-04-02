package com.prajwalch.torrentsearch.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.domain.model.Torrent

import kotlinx.serialization.Serializable

@Entity(
    tableName = "bookmarks",
    indices = [Index("name", unique = true)],
)
@Serializable
data class BookmarkedTorrent(
    @PrimaryKey
    val id: String,
    val name: String,
    val size: String,
    val seeders: Int,
    val peers: Int,
    val providerName: String,
    val uploadDate: String,
    val category: String,
    val descriptionPageUrl: String,
    val magnetUri: String,
    @ColumnInfo(defaultValue = "NULL")
    val fileDownloadLink: String? = null,
)

fun BookmarkedTorrent.toDomain() =
    Torrent(
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
        infoHashOrMagnetUri = InfoHashOrMagnetUri.MagnetUri(uri = this.magnetUri),
        fileDownloadLink = this.fileDownloadLink,
    )

fun Torrent.toEntity() =
    BookmarkedTorrent(
        id = this.id,
        name = this.name,
        size = this.size,
        seeders = this.seeders.toInt(),
        peers = this.peers.toInt(),
        providerName = this.providerName,
        uploadDate = this.uploadDate,
        category = this.category?.name ?: "",
        descriptionPageUrl = this.descriptionPageUrl,
        magnetUri = this.magnetUri(),
        fileDownloadLink = this.fileDownloadLink,
    )

fun List<BookmarkedTorrent>.toDomain() = this.map { it.toDomain() }