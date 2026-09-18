package com.prajwalch.torrentsearch.data.repository

import android.util.Log

import com.prajwalch.torrentsearch.data.local.dao.ViewedTorrentDao
import com.prajwalch.torrentsearch.data.local.entities.ViewedTorrentEntity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing viewed torrents.
 * Provides methods to mark torrents as viewed and retrieve viewed torrent IDs.
 */
class ViewedTorrentRepository(private val dao: ViewedTorrentDao) {
    /**
     * Returns a Flow of all viewed torrent hashes as a Set for efficient lookups.
     */
    fun getAllViewedIds(): Flow<Set<String>> = dao.getAllViewedIds().map { it.toSet() }

    fun getViewedTorrentsCount(): Flow<Int> = dao.getViewedTorrentsCount()

    /**
     * Marks a torrent as viewed by storing its ID.
     */
    suspend fun markAsViewed(id: String) {
        Log.d(TAG, "Marking torrent as viewed: $id")
        dao.insertViewedTorrent(ViewedTorrentEntity(id))
    }

    /**
     * Clears all viewed history.
     */
    suspend fun clearAll() {
        Log.i(TAG, "Clearing all viewed history")
        dao.deleteAllViewedTorrents()
    }

    private companion object {
        private const val TAG = "ViewedTorrentRepository"
    }
}