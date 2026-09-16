package com.prajwalch.torrentsearch.data.repository

import android.util.Log

import com.prajwalch.torrentsearch.data.local.dao.BookmarkedTorrentDao
import com.prajwalch.torrentsearch.data.local.entities.BookmarkedTorrentEntity
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

class BookmarkRepository(private val dao: BookmarkedTorrentDao) {
    fun getAllBookmarks(): Flow<List<Torrent>> {
        return dao.getAllBookmarks().map { it.toDomain() }
    }

    fun getBookmarksCount(): Flow<Int> {
        return dao.getBookmarksCount()
    }

    fun getBookmarkIds(): Flow<Set<String>> {
        return dao.getBookmarkIds().map { it.toSet() }
    }

//    suspend fun bookmarkTorrent(torrent: Torrent) {
//        dao.insertBookmark(torrent.toEntity())
//    }

    suspend fun createAndAddBookmark(
        torrentId: String,
        name: String,
        magnetUri: String,
        size: String?,
        seeders: UInt?,
        peers: UInt?,
        providerName: String,
        uploadDate: Instant?,
        category: Category?,
        descriptionPageUrl: String?,
        fileDownloadLink: String?,
    ) {
        val bookmarkedTorrentEntity = BookmarkedTorrentEntity(
            id = torrentId,
            name = name,
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            size = size,
            seeders = seeders?.toInt(),
            peers = peers?.toInt(),
            providerName = providerName,
            uploadDate = uploadDate?.toEpochMilli(),
            category = category?.name,
            descriptionPageUrl = descriptionPageUrl,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )

        dao.insertBookmark(bookmarkedTorrentEntity)
    }

    suspend fun deleteBookmarkById(id: String) {
        dao.deleteBookmarkById(id)
    }

    suspend fun deleteAllBookmarks() {
        dao.deleteAllBookmarks()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun importBookmarks(inputStream: InputStream) = withContext(Dispatchers.IO) {
        Log.i(TAG, "Importing bookmarks")

        try {
            val bookmarksEntity = Json.decodeFromStream<List<BookmarkedTorrentEntity>>(inputStream)
            dao.insertBookmarks(bookmarksEntity)
            Log.i(TAG, "Import succeed")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Input cannot be represented as a valid Json type", e)
        } catch (e: IOException) {
            Log.e(TAG, "Input cannot be read from the input stream", e)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun exportBookmarks(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        Log.i(TAG, "Exporting bookmarks")

        try {
            val bookmarksEntity = dao.getAllBookmarks().firstOrNull() ?: return@withContext
            Json.encodeToStream(bookmarksEntity, outputStream)
            Log.i(TAG, "Export succeed")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Input cannot be serialized to Json", e)
        } catch (e: IOException) {
            Log.e(TAG, "Input cannot be write to output stream", e)
        }
    }

    private companion object {
        private const val TAG = "BookmarkRepository"
    }
}

private fun BookmarkedTorrentEntity.toDomain() =
    Torrent(
        id = this.id,
        name = this.name,
        size = this.size,
        seeders = this.seeders?.toUInt(),
        peers = this.peers?.toUInt(),
        providerName = this.providerName,
        uploadDate = this.uploadDate?.let(Instant::ofEpochMilli),
        category = this.category?.let(Category::valueOf),
        descriptionPageUrl = this.descriptionPageUrl,
        magnetUri = MagnetUri.Available(
            this.magnetUri ?: TorrentUtils.createMagnetUri(this.infoHash)
        ),
        fileDownloadLink = this.fileDownloadLink,
    )

fun List<BookmarkedTorrentEntity>.toDomain() = this.map { it.toDomain() }