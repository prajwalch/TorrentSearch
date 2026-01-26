package com.prajwalch.torrentsearch.data.remote

import android.net.Uri
import android.util.Log

import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.SearchException
import com.prajwalch.torrentsearch.domain.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.providers.SearchContext
import com.prajwalch.torrentsearch.providers.SearchProvider

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

import javax.inject.Inject

class TorrentsRemoteDataSource @Inject constructor(
    private val httpClient: HttpClient,
) {
    fun searchTorrents(
        query: String,
        category: Category,
        searchProviders: List<SearchProvider>,
    ): Flow<Result<List<Torrent>>> = channelFlow {
        Log.i(TAG, "Searching torrents for $query ($category)")

        if (searchProviders.isEmpty()) {
            send(Result.success(emptyList()))
            return@channelFlow
        }

        val encodedQuery = encodeQuery(query = query)
        val searchContext = SearchContext(category = category, httpClient = httpClient)
        Log.d(TAG, "Query encoded as $encodedQuery")

        for (searchProvider in searchProviders) {
            Log.i(TAG, "Launching ${searchProvider.info.name}")

            launch {
                val result = runCatchingSearchProvider(
                    provider = searchProvider,
                    query = encodedQuery,
                    context = searchContext,
                )
                send(result)
            }
        }
    }

    private fun encodeQuery(query: String): String {
        return Uri.encode(query) ?: query.replace(' ', '+').trim()
    }

    private suspend fun runCatchingSearchProvider(
        provider: SearchProvider,
        query: String,
        context: SearchContext,
    ): Result<List<Torrent>> = try {
        val torrents = provider.search(query = query, context = context)
        Log.i(TAG, "${provider.info.name} succeed with ${torrents.size} results")

        Result.success(torrents)
    } catch (e: CancellationException) {
        Log.i(TAG, "${provider.info.name} got canceled")

        // Never catch this as this is used to cancel coroutines.
        throw e
    } catch (cause: Throwable) {
        Log.e(TAG, "${provider.info.name} crashed", cause)

        val exception = SearchException(
            searchProviderName = provider.info.name,
            searchProviderUrl = provider.info.url,
            message = cause.message ?: cause.toString(),
            cause = cause,
        )
        Result.failure(exception)
    }

    private companion object {
        private const val TAG = "TorrentsRemoteDataSource"
    }
}