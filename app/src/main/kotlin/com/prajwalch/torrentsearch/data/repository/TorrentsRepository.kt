package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.data.remote.TorrentsRemoteDataSource
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.SearchResults
import com.prajwalch.torrentsearch.models.Torrent
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
        val failures = mutableListOf<Throwable>()

        remoteDataSource
            .searchTorrents(
                query = query,
                category = category,
                searchProviders = searchProviders,
            )
            .collect { searchBatchResult ->
                searchBatchResult.fold(
                    onSuccess = { successes.addAll(it) },
                    onFailure = { failures.add(it) }
                )

                val searchResults = SearchResults(
                    successes = successes.toImmutableList(),
                    failures = failures.toImmutableList(),
                )
                emit(searchResults)
            }
    }.flowOn(Dispatchers.IO)
}