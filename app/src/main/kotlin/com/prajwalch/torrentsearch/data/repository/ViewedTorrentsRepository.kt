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
class ViewedTorrentsRepository @Inject constructor(
    private val dao: ViewedTorrentDao
) {
    /**
     * Returns a Flow of all viewed torrent IDs as a Set for efficient lookups.
     */
    fun getAllViewedIds(): Flow<Set<String>> = dao.getAllIds().map { it.toSet() }

    /**
     * Marks a torrent as viewed by storing its ID.
     */
    suspend fun markAsViewed(torrentId: String) {
        Log.d(TAG, "Marking torrent as viewed: $torrentId")
        dao.insert(ViewedTorrentEntity(id = torrentId))
    }

    /**
     * Clears all viewed history.
     */
    suspend fun clearAll() {
        Log.i(TAG, "Clearing all viewed history")
        dao.deleteAll()
    }

    private companion object {
        private const val TAG = "ViewedTorrentsRepository"
    }
}
