package com.prajwalch.torrentsearch.domain

import android.net.Uri
import android.util.Log

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.GetTorrentDetailsResponse
import com.prajwalch.torrentsearch.domain.model.MaxNumResults
import com.prajwalch.torrentsearch.domain.model.SearchProviderError
import com.prajwalch.torrentsearch.domain.model.SearchProviderResult
import com.prajwalch.torrentsearch.domain.model.SearchResults
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.network.CloudflareChallengeException
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.providers.SearchContext
import com.prajwalch.torrentsearch.providers.SearchProvider

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch

import javax.inject.Inject

/**
 * A primary class for interacting with different search providers.
 */
class SearchProvidersGateway @Inject constructor(
    private val httpClient: HttpClient,
    private val searchProvidersManager: SearchProvidersManager,
    private val settingsRepository: SettingsRepository,
) {
    private companion object {
        private const val TAG = "SearchProvidersGateway"
    }

    fun searchTorrents(query: String, category: Category): Flow<SearchResults> = flow {
        val limit = settingsRepository.maxNumResults.firstOrNull() ?: MaxNumResults.Unlimited
        emitAll(searchTorrents(query, category, limit))
    }

    private fun searchTorrents(
        query: String,
        category: Category,
        limit: MaxNumResults,
    ): Flow<SearchResults> = channelFlow {
        val enabledProviders = searchProvidersManager.getEnabledProvidersByCategory(category)
        if (enabledProviders.isEmpty()) return@channelFlow

        val encodedQuery = Uri.encode(query)!!
        val searchContext = SearchContext(category = category, httpClient = httpClient)

        enabledProviders.forEach {
            launch {
                val result = runCatchingProvider(it) { search(encodedQuery, searchContext) }
                send(result)
            }
        }
    }
        .runningFold(SearchResults(), SearchResults::addResult)
        .drop(1)
        .transformWhile { searchResults ->
            if (limit.isUnlimited()) {
                emit(searchResults)
                return@transformWhile true
            }

            // If results already reached the limit, emit the results and
            // return a signal to stop the search.
            if (searchResults.torrents.size == limit.n) {
                emit(searchResults)
                return@transformWhile false
            }

            // If results exceeds the limits, truncate and emit results; and return
            // a signal to stop the search.
            if (searchResults.torrents.size > limit.n) {
                emit(searchResults.takeNTorrents(limit.n))
                return@transformWhile false
            }

            // If results not reached the limit yet, emit the results
            // and return a signal to continue the search.
            emit(searchResults)
            return@transformWhile true
        }
        .flowOn(Dispatchers.IO)

    fun getLatestTorrents(category: Category): Flow<PersistentList<Torrent>> = channelFlow {
        searchProvidersManager.getEnabledLatestTorrentsProviders(category).forEach {
            launch {
                runCatchingProvider(it) { getLastestTorrents(category) }
                    .getOrNull()
                    ?.let { torrents -> send(torrents) }
            }
        }
    }
        .runningFold(persistentListOf<Torrent>()) { results, batchResult ->
            results.addingAll(batchResult)
        }
        .drop(1)
        .flowOn(Dispatchers.IO)

    fun getTopTorrents(category: Category): Flow<PersistentList<Torrent>> = channelFlow {
        searchProvidersManager.getEnabledTopTorrentsProviders(category).forEach {
            launch {
                runCatchingProvider(it) { getTopTorrents(category) }
                    .getOrNull()
                    ?.let { torrents -> send(torrents) }
            }
        }
    }
        .runningFold(persistentListOf<Torrent>()) { results, batchResult ->
            results.addingAll(batchResult)
        }
        .drop(1)
        .flowOn(Dispatchers.IO)

    suspend fun getTorrentDetails(
        detailsPageUrl: String,
        providerName: String,
    ): GetTorrentDetailsResponse {
        val detailsProvider = searchProvidersManager.findDetailsProviderByUrl(detailsPageUrl)
            ?: searchProvidersManager.findDetailsProviderByName(providerName)
            ?: return GetTorrentDetailsResponse.UnsupportedUrl

        return detailsProvider.getDetails(detailsPageUrl)
            ?.let(GetTorrentDetailsResponse::Success)
            ?: GetTorrentDetailsResponse.Unavailable
    }

    private suspend fun <P : SearchProvider, T> runCatchingProvider(
        provider: P, action: suspend P.() -> T,
    ): SearchProviderResult<T> = try {
        val result = provider.action()

        if (result is List<*>) {
            Log.i(TAG, "${provider.name} succeed with ${result.size} results")
        } else {
            Log.i(TAG, "${provider.name} completed successfully")
        }

        SearchProviderResult.Success(result)
    } catch (e: CancellationException) {
        Log.i(TAG, "${provider.name} got canceled")

        throw e
    } catch (cause: Exception) {
        Log.e(TAG, "${provider.name} crashed", cause)

        if (cause is CloudflareChallengeException) {
            Log.i(TAG, "Locking ${provider.name} (${provider.id})")
            searchProvidersManager.lockProvider(provider.id)
        }

        val error = SearchProviderError(
            providerName = provider.name,
            providerUrl = provider.url,
            message = cause.message ?: cause.toString(),
            cause = cause,
        )
        SearchProviderResult.Error(error)
    }
}