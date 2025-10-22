package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.data.database.dao.SearchHistoryDao
import com.prajwalch.torrentsearch.data.database.entities.toDomain
import com.prajwalch.torrentsearch.data.database.entities.toEntity
import com.prajwalch.torrentsearch.models.SearchHistory

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import javax.inject.Inject

class SearchHistoryRepository @Inject constructor(private val dao: SearchHistoryDao) {
    /** Returns all the saved search history. */
    fun observeAllSearchHistories(): Flow<List<SearchHistory>> {
        return dao.observeAll().map { it.toDomain() }
    }

    /**
     * Adds the given search history otherwise does noting if the query
     * is already saved.
     */
    suspend fun createNewSearchHistory(query: String) {
        val searchHistory = SearchHistory(query = query)
        dao.insert(searchHistory.toEntity())
    }

    /** Removes the given search history. */
    suspend fun deleteSearchHistory(searchHistory: SearchHistory) {
        dao.delete(searchHistory.toEntity())
    }

    /** Clears all search history. */
    suspend fun deleteAllSearchHistories() {
        dao.deleteAll()
    }
}