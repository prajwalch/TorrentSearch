package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.data.local.dao.SearchHistoryDao
import com.prajwalch.torrentsearch.data.local.entities.SearchHistoryEntity
import com.prajwalch.torrentsearch.domain.model.SearchHistory
import com.prajwalch.torrentsearch.domain.model.SearchHistoryId

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Represent a map of search histories grouped by a date.
 */
typealias SearchHistoriesByDate = ImmutableMap<SearchHistoryDate, ImmutableList<SearchHistory>>

/**
 * A date in which search history is searched/created.
 */
data class SearchHistoryDate(val date: LocalDate)

class SearchHistoryRepository(private val dao: SearchHistoryDao) {
    /**
     * Returns all the saved search history.
     */
    fun getAllSearchHistories(): Flow<List<SearchHistory>> {
        return dao.getAllSearchHistories().map { it.toDomain() }
    }

    /**
     * Returns all search histories grouped by a date.
     */
    fun getSearchHistoriesByDate(): Flow<SearchHistoriesByDate> {
        return dao.getAllSearchHistories().map { groupSearchHistoriesByDate(it) }
    }

    private fun groupSearchHistoriesByDate(
        entities: List<SearchHistoryEntity>,
    ): SearchHistoriesByDate = entities
        .sortedByDescending { it.lastSearchedAt }
        .groupingBy { getSearchHistoryDate(it.lastSearchedAt) }
        .fold(
            initialValue = persistentListOf<SearchHistory>(),
            operation = { list, entity -> list.adding(entity.toDomain()) },
        )
        .toImmutableMap()

    private fun getSearchHistoryDate(lastSearchedAt: Long): SearchHistoryDate {
        return Instant.ofEpochMilli(lastSearchedAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .let(::SearchHistoryDate)
    }

    /**
     * Returns all the saved search history that contains the given term.
     */
    fun getSearchHistoriesByTerm(term: String): Flow<List<SearchHistory>> {
        return dao.getSearchHistoriesByTerm(term).map { it.toDomain() }
    }

    fun getRecentSearches(): Flow<List<String>> {
        return dao.getRecentSearchHistories().map { historyEntities ->
            historyEntities.map { it.query }
        }
    }

    /**
     * Adds the given search history otherwise does noting if the query
     * is already saved.
     */
    suspend fun createNewSearchHistory(query: String) {
        dao.insertSearchHistory(
            SearchHistoryEntity(
                query = query.trim(),
                lastSearchedAt = System.currentTimeMillis(),
            )
        )
    }

    /**
     * Deletes the search history which matches the specified id.
     */
    suspend fun deleteSearchHistoryById(id: SearchHistoryId) {
        dao.deleteSearchHistoryById(id = id)
    }

    /**
     * Clears all search history.
     */
    suspend fun deleteAllSearchHistories() {
        dao.deleteAllSearchHistories()
    }
}

private fun SearchHistoryEntity.toDomain() = SearchHistory(id = this.id, query = this.query)

private fun List<SearchHistoryEntity>.toDomain() = this.map { it.toDomain() }