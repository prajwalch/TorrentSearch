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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import javax.inject.Inject

class TorrentsRemoteDataSource @Inject constructor(private val httpClient: HttpClient) {
    private val cache = mutableListOf<Torrent>()
    private val mutex = Mutex()

    fun getCache() = cache.toList()

    fun searchTorrents(
        query: String,
        category: Category,
        searchProviders: List<SearchProvider>,
    ): Flow<Result<List<Torrent>>> = channelFlow {
        mutex.withLock { cache.clear() }

        val searchProviders = searchProviders.filterByCategory(category)
        if (searchProviders.isEmpty()) {
            Log.i(TAG, "Search providers are empty. Cancelling search")
            return@channelFlow
        }

        val encodedQuery = Uri.encode(query) ?: query.replace(' ', '+').trim()
        val searchContext = SearchContext(category = category, httpClient = httpClient)
        Log.d(TAG, "Encoded query = $encodedQuery")

        for (searchProvider in searchProviders) {
            Log.i(TAG, "Launching ${searchProvider.info.name} (${searchProvider.info.id})")

            launch {
                val result = runCatching {
                    searchProvider
                        .search(query = encodedQuery, context = searchContext)
                        .also { mutex.withLock { cache.addAll(it) } }
                }
                Log.d(TAG, "Got ${searchProvider.info.name} result: success = ${result.isSuccess}")
                send(result)
            }
        }
    }

    /**
     * Filters and returns only those search providers that is capable of
     * handling the given category.
     */
    private fun List<SearchProvider>.filterByCategory(category: Category): List<SearchProvider> {
        if (category == Category.All) {
            return this
        }

        return this.filter {
            // NOTE: Currently, if the search provider's specialized is set to
            //       `All` we can't surely know whether the underlying server
            //       supports setting specific category or not.
            //
            //       For example: TorrentCSV does allow to search any type of
            //       torrent but it doesn't support setting specific category
            //       explicitly. Meaning, we can't ask it to search torrents
            //       of specific category.
            //
            //       To address this issue, force each and every search provider
            //       to list out all categories they allow or support to set
            //       explicitly instead of single `specializedCategory`.
            (it.info.specializedCategory == Category.All) || (category == it.info.specializedCategory)
        }
    }

    private companion object {
        private const val TAG = "TorrentsRemoteDataSource"
    }
}