package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.data.remote.TorrentsRemoteDataSource
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.providers.SearchProvider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import javax.inject.Inject

typealias SearchResult = Result<List<Torrent>>

class TorrentsRepository @Inject constructor(
    private val remoteDataSource: TorrentsRemoteDataSource,
) {
    private val mutex = Mutex()
    private val cache = mutableListOf<SearchResult>()

    fun search(
        query: String,
        category: Category,
        searchProviders: List<SearchProvider>,
    ): Flow<List<SearchResult>> = flow {
        clearCache()

        remoteDataSource
            .searchTorrents(
                query = query,
                category = category,
                searchProviders = searchProviders,
            )
            .collect { emit(addAndGetCache(it)) }
    }.flowOn(Dispatchers.IO)

    private suspend fun addAndGetCache(searchResult: SearchResult): List<SearchResult> {
        return mutex.withLock {
            cache.add(searchResult)
            cache.toList()
        }
    }

    private suspend fun clearCache() {
        mutex.withLock { cache.clear() }
    }
}