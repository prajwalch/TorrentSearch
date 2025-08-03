package com.prajwalch.torrentsearch.data

import com.prajwalch.torrentsearch.database.dao.SearchHistoryDao
import com.prajwalch.torrentsearch.database.entities.SearchHistory

import kotlinx.coroutines.flow.Flow

class SearchHistoryRepository(private val dao: SearchHistoryDao) {
    /**
     * Adds the given search history otherwise does noting if the query
     * is already saved.
     */
    suspend fun add(searchHistory: SearchHistory) {
        dao.insert(searchHistory)
    }

    /** Returns all the saved search history. */
    fun getAll(): Flow<List<SearchHistory>> {
        return dao.getAll()
    }

    /** Removes the given search history. */
    suspend fun remove(searchHistory: SearchHistory) {
        dao.delete(searchHistory)
    }

    /** Clears all search history. */
    suspend fun clearAll() {
        dao.clearAll()
    }
}