package com.prajwalch.torrentsearch.data.repository

import android.util.Log

import com.prajwalch.torrentsearch.data.local.dao.BookmarkedTorrentDao
import com.prajwalch.torrentsearch.data.local.entities.BookmarkedTorrent
import com.prajwalch.torrentsearch.data.local.entities.toDomain
import com.prajwalch.torrentsearch.data.local.entities.toEntity
import com.prajwalch.torrentsearch.domain.models.Torrent

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

import javax.inject.Inject

class BookmarksRepository @Inject constructor(private val dao: BookmarkedTorrentDao) {
    fun observeAllBookmarks(): Flow<List<Torrent>> {
        return dao.observeAll().map { it.toDomain() }
    }

    suspend fun bookmarkTorrent(torrent: Torrent) {
        dao.insert(bookmarkedTorrent = torrent.toEntity())
    }

    suspend fun deleteBookmarkedTorrent(torrent: Torrent) {
        dao.delete(bookmarkedTorrent = torrent.toEntity())
    }

    suspend fun deleteAllBookmarks() {
        dao.deleteAll()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun importBookmarks(inputStream: InputStream) = withContext(Dispatchers.IO) {
        Log.i(TAG, "Importing bookmarks")

        try {
            val bookmarksEntity = Json.decodeFromStream<List<BookmarkedTorrent>>(inputStream)
            dao.insertAll(bookmarksEntity)
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
            val bookmarksEntity = dao.observeAll().firstOrNull() ?: return@withContext
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