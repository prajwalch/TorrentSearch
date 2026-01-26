package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.data.local.dao.SearchHistoryDao
import com.prajwalch.torrentsearch.data.local.entities.toDomain
import com.prajwalch.torrentsearch.data.local.entities.toEntity
import com.prajwalch.torrentsearch.domain.models.SearchHistory
import com.prajwalch.torrentsearch.domain.models.SearchHistoryId

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
        val searchHistory = SearchHistory(query = query.trim())
        dao.insert(searchHistory.toEntity())
    }

    /** Deletes the search history which matches the specified id. */
    suspend fun deleteSearchHistoryById(id: SearchHistoryId) {
        dao.deleteById(id = id)
    }

    /** Clears all search history. */
    suspend fun deleteAllSearchHistories() {
        dao.deleteAll()
    }
}