package com.prajwalch.torrentsearch.data.remote

import android.net.Uri
import android.util.Log

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.providers.SearchContext
import com.prajwalch.torrentsearch.providers.SearchProvider

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

import javax.inject.Inject

class TorrentsRemoteDataSource @Inject constructor(private val httpClient: HttpClient) {
    fun searchTorrents(
        query: String,
        category: Category,
        searchProviders: List<SearchProvider>,
    ): Flow<Result<List<Torrent>>> = channelFlow {

        if (searchProviders.isEmpty()) {
            send(Result.success(emptyList()))
            return@channelFlow
        }

        val encodedQuery = encodeQuery(query = query)
        val searchContext = SearchContext(category = category, httpClient = httpClient)
        Log.d(TAG, "Encoded query = $encodedQuery")

        for (searchProvider in searchProviders) {
            Log.i(TAG, "Launching ${searchProvider.info.name} (${searchProvider.info.id})")

            launch {
                val result = runCatching {
                    searchProvider.search(query = encodedQuery, context = searchContext)
                }
                Log.d(TAG, "Got ${searchProvider.info.name} result: success = ${result.isSuccess}")
                send(result)
            }
        }
    }

    private fun encodeQuery(query: String): String {
        return Uri.encode(query) ?: query.replace(' ', '+').trim()
    }

    private companion object {
        private const val TAG = "TorrentsRemoteDataSource"
    }
}