package com.prajwalch.torrentsearch.data

import com.prajwalch.torrentsearch.database.BookmarkedTorrentDao
import com.prajwalch.torrentsearch.database.entities.BookmarkedTorrent
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarksRepository(private val dao: BookmarkedTorrentDao) {
    suspend fun add(torrent: Torrent) {
        dao.insert(bookmarkedTorrent = torrent.toEntity())
    }

    fun all(): Flow<List<Torrent>> = dao.getAll().map { bookmarkedTorrents ->
        bookmarkedTorrents.map { it.toModel() }
    }

    suspend fun delete(torrent: Torrent) {
        dao.delete(bookmarkedTorrent = torrent.toEntity())
    }
}

private fun Torrent.toEntity(): BookmarkedTorrent = BookmarkedTorrent(
    id = id,
    name = name,
    size = size,
    seeders = seeders.toInt(),
    peers = peers.toInt(),
    providerId = providerId,
    providerName = providerName,
    uploadDate = uploadDate,
    category = category?.name ?: "",
    descriptionPageUrl = descriptionPageUrl,
    magnetUri = magnetUri(),
)

private fun BookmarkedTorrent.toModel(): Torrent = Torrent(
    id = id,
    name = name,
    size = size,
    seeders = seeders.toUInt(),
    peers = peers.toUInt(),
    providerId = providerId,
    providerName = providerName,
    uploadDate = uploadDate,
    category = if (category.isNotEmpty()) Category.valueOf(category) else null,
    descriptionPageUrl = descriptionPageUrl,
    infoHashOrMagnetUri = InfoHashOrMagnetUri.MagnetUri(uri = magnetUri),
    bookmarked = true,
)