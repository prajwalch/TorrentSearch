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
import com.prajwalch.torrentsearch.providers.SearchProvider

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.scan
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

    fun searchTorrents(query: String, category: Category): Flow<SearchResults> = flow {
        val enabledProviders = searchProvidersManager.getEnabledProvidersByCategory(category)
        if (enabledProviders.isEmpty()) return@flow

        val encodedQuery = Uri.encode(query)!!
        val searchContext = SearchContext(category = category, httpClient = httpClient)

        launchSearchProviders(
            providers = enabledProviders,
            query = encodedQuery,
            context = searchContext
        )
            .scan(initial = SearchResults()) { results, batchResult ->
                results.appendBatchResult(batchResult)
            }
            // Drop the initial empty results.
            .drop(1)
            .collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    private fun launchSearchProviders(
        providers: List<SearchProvider>,
        query: String,
        context: SearchContext,
    ): Flow<Result<List<Torrent>>> = channelFlow {
        providers.forEach {
            launch { send(runCatchingSearchProvider(it, query, context)) }
        }
    }

    private suspend fun runCatchingSearchProvider(
        provider: SearchProvider,
        query: String,
        context: SearchContext,
    ): Result<List<Torrent>> = try {
        val torrents = provider.search(query = query, context = context)
        Log.i(TAG, "${provider.name} succeed with ${torrents.size} results")

        Result.success(torrents)
    } catch (e: CancellationException) {
        Log.i(TAG, "${provider.name} got canceled")

        // Never catch this as this is used to cancel coroutines.
        throw e
    } catch (cause: Throwable) {
        Log.e(TAG, "${provider.name} crashed", cause)

        val exception = SearchException(
            searchProviderName = provider.name,
            searchProviderUrl = provider.url,
            message = cause.message ?: cause.toString(),
            cause = cause,
        )
        Result.failure(exception)
    }

    suspend fun getTorrentDetails(
        detailsPageUrl: String,
        providerName: String,
    ): GetTorrentDetailsResponse {
        val detailsProvider = searchProvidersManager.findProviderByName(providerName)
        require(detailsProvider != null) { "Couldn't find '$providerName' details provider" }

        return detailsProvider.getDetails(detailsPageUrl)
    }
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