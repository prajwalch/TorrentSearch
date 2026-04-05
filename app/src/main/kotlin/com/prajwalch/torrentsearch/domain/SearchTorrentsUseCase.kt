package com.prajwalch.torrentsearch.domain

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.data.repository.TorrentRepository
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MaxNumResults
import com.prajwalch.torrentsearch.domain.model.SearchResults

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformWhile

import javax.inject.Inject

class SearchTorrentsUseCase @Inject constructor(
    private val torrentRepository: TorrentRepository,
    private val searchProvidersManager: SearchProvidersManager,
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(query: String, category: Category): Flow<SearchResults> = flow {
        val enabledSearchProviders = searchProvidersManager.getEnabledProvidersByCategory(category)
        val limit = getSearchResultsLimit()

        torrentRepository.search(
            query = query,
            category = category,
            searchProviders = enabledSearchProviders,
        ).transformWhile {
            if (limit.isUnlimited()) {
                emit(it)
                return@transformWhile true
            }

            // If results already reached the limit, emit it and cancel the search.
            if (it.successes.size == limit.n) {
                emit(it)
                return@transformWhile false
            }

            // If results is yet to reach the limit, emit it and continue the search.
            if (it.successes.size < limit.n) {
                emit(it)
                // Continue search since we don't receive sufficient results yet.
                return@transformWhile true
            }

            // If results crossed the limit, take the number of results set
            // by the limit, emit it and then cancel the search.
            val searchResults = it.copy(
                successes = it.successes.take(limit.n).toImmutableList()
            )
            emit(searchResults)

            return@transformWhile false
        }.collect { emit(it) }
    }

    private suspend fun getSearchResultsLimit(): MaxNumResults {
        return settingsRepository.maxNumResults.firstOrNull() ?: MaxNumResults.Unlimited
    }
}