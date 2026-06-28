package com.prajwalch.torrentsearch.data.repository

import android.util.Log

import com.prajwalch.torrentsearch.data.local.dao.BookmarkedTorrentDao
import com.prajwalch.torrentsearch.data.local.entities.BookmarkedTorrentEntity
import com.prajwalch.torrentsearch.domain.model.BookmarkedTorrent
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent

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
import javax.inject.Inject

class BookmarkRepository @Inject constructor(private val dao: BookmarkedTorrentDao) {
    fun getAllBookmarks(): Flow<List<BookmarkedTorrent>> {
        return dao.getAllBookmarks().map { it.toDomain() }
    }

    suspend fun bookmarkTorrent(torrent: Torrent) {
        dao.insertBookmark(bookmarkedTorrent = torrent.toEntity())
    }

    suspend fun deleteBookmarkById(id: Long) {
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
        private const val TAG = "BookmarksRepository"
    }
}

private fun BookmarkedTorrentEntity.toDomain() =
    BookmarkedTorrent(
        id = this.id,
        torrent = Torrent(
            infoHash = this.infoHash,
            name = this.name,
            size = this.size,
            seeders = this.seeders?.toUInt(),
            peers = this.peers?.toUInt(),
            providerName = this.providerName,
            uploadDate = this.uploadDate?.let(Instant::ofEpochMilli),
            category = if (this.category.isNotEmpty()) {
                Category.valueOf(this.category)
            } else {
                null
            },
            descriptionPageUrl = this.descriptionPageUrl,
            magnetUri = this.magnetUri,
            fileDownloadLink = this.fileDownloadLink,
        ),
    )

private fun Torrent.toEntity() =
    BookmarkedTorrentEntity(
        infoHash = this.infoHash,
        name = this.name,
        size = this.size,
        seeders = this.seeders?.toInt(),
        peers = this.peers?.toInt(),
        providerName = this.providerName,
        uploadDate = this.uploadDate?.toEpochMilli(),
        category = this.category?.name ?: "",
        descriptionPageUrl = this.descriptionPageUrl,
        magnetUri = this.magnetUri,
        fileDownloadLink = this.fileDownloadLink,
    )

fun List<BookmarkedTorrentEntity>.toDomain() = this.map { it.toDomain() }