package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.data.remote.TorrentRemoteDataSource
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.SearchException
import com.prajwalch.torrentsearch.domain.model.SearchResults
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.providers.SearchProvider

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.scan

import javax.inject.Inject

class TorrentRepository @Inject constructor(
    private val remoteDataSource: TorrentRemoteDataSource,
) {
    fun search(
        query: String,
        category: Category,
        searchProviders: List<SearchProvider>,
    ): Flow<SearchResults> =
        remoteDataSource.searchTorrents(
            query = query,
            category = category,
            searchProviders = searchProviders,
        ).scan(
            initial = SearchResults(),
            operation = ::appendBatchResult,
        ).drop(1).flowOn(Dispatchers.IO)

    private fun appendBatchResult(
        currentSearchResults: SearchResults,
        batchResult: Result<List<Torrent>>,
    ): SearchResults = batchResult.fold(
        onSuccess = { currentSearchResults.appendSuccesses(it) },
        onFailure = { currentSearchResults.appendFailure(it as SearchException) },
    )
}

private fun SearchResults.appendSuccesses(successes: List<Torrent>): SearchResults {
    return this.copy(successes = this.successes.plus(successes).toImmutableList())
}

private fun SearchResults.appendFailure(failure: SearchException): SearchResults {
    return this.copy(failures = this.failures.plus(failure).toImmutableList())
}