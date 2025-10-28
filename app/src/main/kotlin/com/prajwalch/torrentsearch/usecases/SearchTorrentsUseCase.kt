package com.prajwalch.torrentsearch.usecases

import com.prajwalch.torrentsearch.data.repository.SearchProvidersRepository
import com.prajwalch.torrentsearch.data.repository.SearchResult
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.data.repository.TorrentsRepository
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.MaxNumResults
import com.prajwalch.torrentsearch.providers.SearchProvider

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile

import javax.inject.Inject

class SearchTorrentsUseCase @Inject constructor(
    private val torrentsRepository: TorrentsRepository,
    private val searchProvidersRepository: SearchProvidersRepository,
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(query: String, category: Category): Flow<List<SearchResult>> = flow {
        val enabledSearchProviders = getEnabledSearchProviders(category = category)
        val limit = getSearchResultsLimit()

        torrentsRepository
            .search(
                query = query,
                category = category,
                searchProviders = enabledSearchProviders,
            )
            .takeWhile { limit.isUnlimited() || it.size < limit.n }
            .collect { emit(it) }
    }

    private suspend fun getEnabledSearchProviders(category: Category): List<SearchProvider> {
        val searchProviders = searchProvidersRepository
            .getSearchProvidersInstance(category = category)
        val enabledSearchProvidersId = settingsRepository.enabledSearchProvidersId.first()

        return searchProviders.filter { it.info.id in enabledSearchProvidersId }
    }

    private suspend fun getSearchResultsLimit(): MaxNumResults {
        return settingsRepository.maxNumResults.firstOrNull() ?: MaxNumResults.Unlimited
    }
}