package com.prajwalch.torrentsearch.domain

import android.net.Uri
import android.util.Log

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.GetTorrentDetailsResponse
import com.prajwalch.torrentsearch.domain.model.SearchException
import com.prajwalch.torrentsearch.domain.model.SearchResults
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.providers.SearchContext

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch

import javax.inject.Inject

/**
 * A primary class for interacting with different search providers.
 */
class SearchProvidersGateway @Inject constructor(
    private val httpClient: HttpClient,
    private val searchProvidersManager: SearchProvidersManager,
) {
    private companion object {
        private const val TAG = "SearchProvidersGateway"
    }

    fun searchTorrents(query: String, category: Category): Flow<SearchResults> = channelFlow {
        val enabledProviders = searchProvidersManager.getEnabledProvidersByCategory(category)
        if (enabledProviders.isEmpty()) return@channelFlow

        val encodedQuery = Uri.encode(query)!!
        val searchContext = SearchContext(category = category, httpClient = httpClient)

        enabledProviders.forEach {
            launch {
                val result = runCatchingProvider(it.name, it.url) {
                    it.search(encodedQuery, searchContext)
                }
                send(result)
            }
        }
    }
        .runningFold(SearchResults()) { results, batchResult ->
            results.appendBatchResult(batchResult)
        }
        .drop(1)
        .flowOn(Dispatchers.IO)

    private suspend fun runCatchingProvider(
        providerName: String,
        providerUrl: String,
        action: suspend () -> List<Torrent>,
    ): Result<List<Torrent>> = try {
        val torrents = action()
        Log.i(TAG, "$providerName succeed with ${torrents.size} results")

        Result.success(torrents)
    } catch (e: CancellationException) {
        Log.i(TAG, "$providerName got canceled")

        // Never catch this as this is used to cancel coroutines.
        throw e
    } catch (cause: Throwable) {
        Log.e(TAG, "$providerName crashed", cause)

        val exception = SearchException(
            searchProviderName = providerName,
            searchProviderUrl = providerUrl,
            message = cause.message ?: cause.toString(),
            cause = cause,
        )
        Result.failure(exception)
    }

    suspend fun getTorrentDetails(
        detailsPageUrl: String,
        providerName: String,
    ): GetTorrentDetailsResponse {
        val detailsProvider = searchProvidersManager.findDetailsProviderByName(providerName)
            ?: return GetTorrentDetailsResponse.RequestNotSupported

        return detailsProvider.getDetails(detailsPageUrl)
            ?.let(GetTorrentDetailsResponse::Success)
            ?: GetTorrentDetailsResponse.DetailsNotFound
    }

    fun getLatestTorrents(category: Category = Category.All): Flow<List<Torrent>> = channelFlow {
        val latestTorrentsProvider = searchProvidersManager.getLatestTorrentsProviders(category)

        latestTorrentsProvider.forEach {
            launch {
                val result = runCatchingProvider(it.name, it.url) {
                    it.getLastestTorrents(category)
                }
                if (result.isSuccess) {
                    val torrents = result.getOrNull()!!
                    send(torrents)
                }
            }
        }
    }
        .runningFold(emptyList<Torrent>()) { results, batchResult -> results + batchResult }
        .drop(1)
        .flowOn(Dispatchers.IO)

    fun getTopTorrents(category: Category = Category.All): Flow<List<Torrent>> = channelFlow {
        val topTorrentsProviders = searchProvidersManager.getTopTorrentsProviders(category)

        topTorrentsProviders.forEach {
            launch {
                val result = runCatchingProvider(it.name, it.url) { it.getTopTorrents(category) }
                if (result.isSuccess) {
                    val torrents = result.getOrNull()!!
                    send(torrents)
                }
            }
        }
    }
        .runningFold(emptyList<Torrent>()) { results, batchResult -> results + batchResult }
        .drop(1)
        .flowOn(Dispatchers.IO)
}

private fun SearchResults.appendBatchResult(batchResult: Result<List<Torrent>>): SearchResults =
    batchResult.fold(
        onSuccess = { this.appendSuccesses(it) },
        onFailure = { this.appendFailure(it as SearchException) },
    )

private fun SearchResults.appendSuccesses(successes: List<Torrent>): SearchResults {
    return this.copy(successes = this.successes.plus(successes).toImmutableList())
}

private fun SearchResults.appendFailure(failure: SearchException): SearchResults {
    return this.copy(failures = this.failures.plus(failure).toImmutableList())
}