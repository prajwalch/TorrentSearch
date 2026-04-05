package com.prajwalch.torrentsearch.data.repository

import android.util.Log

import com.prajwalch.torrentsearch.data.local.dao.ViewedTorrentDao
import com.prajwalch.torrentsearch.data.local.entities.ViewedTorrentEntity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import javax.inject.Inject

/**
 * Repository for managing viewed torrents.
 * Provides methods to mark torrents as viewed and retrieve viewed torrent IDs.
 */
class ViewedTorrentRepository @Inject constructor(
    private val dao: ViewedTorrentDao,
) {
    /**
     * Returns a Flow of all viewed torrent hashes as a Set for efficient lookups.
     */
    fun getAllViewedHashes(): Flow<Set<String>> = dao.getAllViewedHashes().map { it.toSet() }

    /**
     * Marks a torrent as viewed by storing its info hash.
     */
    suspend fun markAsViewed(infoHash: String) {
        Log.d(TAG, "Marking torrent as viewed: $infoHash")
        dao.insertViewedTorrent(ViewedTorrentEntity(infoHash = infoHash))
    }

    /**
     * Clears all viewed history.
     */
    suspend fun clearAll() {
        Log.i(TAG, "Clearing all viewed history")
        dao.deleteAllViewedTorrents()
    }

    private companion object {
        private const val TAG = "ViewedTorrentsRepository"
    }
}