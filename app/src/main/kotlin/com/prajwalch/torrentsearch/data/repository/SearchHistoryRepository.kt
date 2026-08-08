package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.data.local.dao.SearchHistoryDao
import com.prajwalch.torrentsearch.data.local.entities.toDomain
import com.prajwalch.torrentsearch.data.local.entities.toEntity
import com.prajwalch.torrentsearch.domain.model.SearchHistory
import com.prajwalch.torrentsearch.domain.model.SearchHistoryId

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchHistoryRepository(private val dao: SearchHistoryDao) {
    /** Returns all the saved search history. */
    fun getAllSearchHistories(): Flow<List<SearchHistory>> {
        return dao.getAllSearchHistories().map { it.toDomain() }
    }

    /** Returns all the saved search history that contains the given term. */
    fun getSearchHistoriesByTerm(term: String): Flow<List<SearchHistory>> {
        return dao.getSearchHistoriesByTerm(term).map { it.toDomain() }
    }

    /**
     * Adds the given search history otherwise does noting if the query
     * is already saved.
     */
    suspend fun createNewSearchHistory(query: String) {
        val searchHistory = SearchHistory(query = query.trim())
        dao.insertSearchHistory(searchHistory.toEntity())
    }

    /** Deletes the search history which matches the specified id. */
    suspend fun deleteSearchHistoryById(id: SearchHistoryId) {
        dao.deleteSearchHistoryById(id = id)
    }

    /** Clears all search history. */
    suspend fun deleteAllSearchHistories() {
        dao.deleteAllSearchHistories()
    }
}