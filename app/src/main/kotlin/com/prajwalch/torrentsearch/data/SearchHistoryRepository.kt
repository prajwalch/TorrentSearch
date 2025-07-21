package com.prajwalch.torrentsearch.data

import com.prajwalch.torrentsearch.database.TorrentSearchDatabase
import com.prajwalch.torrentsearch.database.entities.SearchHistory

import kotlinx.coroutines.flow.Flow

class SearchHistoryRepository(private val database: TorrentSearchDatabase) {
    /** Returns all the saved search history. */
    fun getAll(): Flow<List<SearchHistory>> {
        return database.searchHistories()
    }

    /**
     * Adds the given search history otherwise does noting if the query
     * is already saved.
     */
    suspend fun add(searchHistory: SearchHistory) {
        database.insert(searchHistory)
    }

    /** Removes the given search history. */
    suspend fun remove(searchHistory: SearchHistory) {
        database.delete(searchHistory)
    }

    /** Clears all search history. */
    suspend fun clearAll() {
        database.clearSearchHistory()
    }
}