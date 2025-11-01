package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.data.remote.TorrentsRemoteDataSource
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.providers.SearchProvider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

import javax.inject.Inject

typealias SearchResult = Result<List<Torrent>>

class TorrentsRepository @Inject constructor(
    private val remoteDataSource: TorrentsRemoteDataSource,
) {
    fun search(
        query: String,
        category: Category,
        searchProviders: List<SearchProvider>,
    ): Flow<List<SearchResult>> = flow {
        val searchResults = mutableListOf<SearchResult>()

        remoteDataSource
            .searchTorrents(
                query = query,
                category = category,
                searchProviders = searchProviders,
            )
            .collect {
                searchResults.add(it)
                emit(searchResults.toList())
            }
    }.flowOn(Dispatchers.IO)
}