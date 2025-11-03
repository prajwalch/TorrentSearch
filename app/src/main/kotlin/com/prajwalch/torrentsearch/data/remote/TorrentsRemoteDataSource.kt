package com.prajwalch.torrentsearch.data.remote

import android.net.Uri
import android.util.Log

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.SearchProviderException
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
            val searchProviderInfo = searchProvider.info
            Log.i(TAG, "Launching ${searchProviderInfo.name} (${searchProviderInfo.id})")

            launch {
                try {
                    val searchBatchResult = searchProvider.search(
                        query = encodedQuery,
                        context = searchContext,
                    )
                    send(Result.success(searchBatchResult))
                } catch (cause: Throwable) {
                    val searchProviderException = SearchProviderException(
                        id = searchProviderInfo.id,
                        name = searchProviderInfo.name,
                        url = searchProviderInfo.url,
                        cause = cause,
                    )
                    send(Result.failure(searchProviderException))
                }
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