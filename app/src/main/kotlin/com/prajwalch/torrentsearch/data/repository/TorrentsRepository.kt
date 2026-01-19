package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.data.remote.TorrentsRemoteDataSource
import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.SearchException
import com.prajwalch.torrentsearch.domain.models.SearchResults
import com.prajwalch.torrentsearch.domain.models.Torrent
import com.prajwalch.torrentsearch.providers.SearchProvider

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

import javax.inject.Inject

class TorrentsRepository @Inject constructor(
    private val remoteDataSource: TorrentsRemoteDataSource,
) {
    fun search(
        query: String,
        category: Category,
        searchProviders: List<SearchProvider>,
    ): Flow<SearchResults> = flow {
        val successes = mutableListOf<Torrent>()
        val failures = mutableListOf<SearchException>()

        remoteDataSource.searchTorrents(
            query = query,
            category = category,
            searchProviders = searchProviders,
        ).collect { searchBatchResult ->
            searchBatchResult.fold(
                onSuccess = { successes.addAll(filterTorrentsByCategory(it, category)) },
                onFailure = { failures.add(it as SearchException) }
            )

            val searchResults = SearchResults(
                successes = successes.toImmutableList(),
                failures = failures.toImmutableList(),
            )
            emit(searchResults)
        }
    }.flowOn(Dispatchers.IO)

    private fun filterTorrentsByCategory(
        torrents: List<Torrent>,
        category: Category,
    ): List<Torrent> {
        return if (category == Category.All) {
            torrents
        } else {
            torrents.filter { it.category == category }
        }
    }
}