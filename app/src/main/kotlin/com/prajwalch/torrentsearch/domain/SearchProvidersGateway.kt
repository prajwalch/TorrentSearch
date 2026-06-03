package com.prajwalch.torrentsearch.domain

import android.net.Uri
import android.util.Log

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.GetTorrentDetailsResponse
import com.prajwalch.torrentsearch.domain.model.SearchException
import com.prajwalch.torrentsearch.domain.model.SearchResults
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.addResult
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

    fun searchTorrents(query: String, category: Category): Flow<SearchResults> =
        channelFlow {
            val enabledProviders = searchProvidersManager.getEnabledProvidersByCategory(category)
            if (enabledProviders.isEmpty()) return@channelFlow

            val encodedQuery = Uri.encode(query)!!
            val searchContext = SearchContext(category = category, httpClient = httpClient)

            enabledProviders.forEach {
                launch {
                    val result = runCatchingProvider(it) {
                        search(encodedQuery, searchContext)
                    }
                    send(result)
                }
            }
        }
            .runningFold(SearchResults()) { results, batchResult ->
                results.addResult(batchResult)
            }
            .drop(1)
            .flowOn(Dispatchers.IO)

    private suspend fun <T : SearchProvider> runCatchingProvider(
        provider: T,
        action: suspend T.() -> List<Torrent>,
    ): Result<List<Torrent>> = try {
        val torrents = provider.action()
        Log.i(TAG, "${provider.name} succeed with ${torrents.size} results")

        Result.success(torrents)
    } catch (e: CancellationException) {
        Log.i(TAG, "${provider.name} got canceled")

        // Never catch this as this is used to cancel coroutines.
        throw e
    } catch (cause: Throwable) {
        Log.e(TAG, "${provider.name} crashed", cause)

        if (cause is CloudflareChallengeException) {
            Log.i(TAG, "Locking ${provider.name} (${provider.id})")
            searchProvidersManager.lockProvider(provider.id)
        }

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
        val detailsProvider = searchProvidersManager.findDetailsProviderByUrl(detailsPageUrl)
            ?: searchProvidersManager.findDetailsProviderByName(providerName)
            ?: return GetTorrentDetailsResponse.UnsupportedUrl

        return detailsProvider.getDetails(detailsPageUrl)
            ?.let(GetTorrentDetailsResponse::Success)
            ?: GetTorrentDetailsResponse.Unavailable
    }

    fun getLatestTorrents(category: Category = Category.All): Flow<PersistentList<Torrent>> =
        channelFlow {
            searchProvidersManager.getEnabledLatestTorrentsProviders(category).forEach {
                launch {
                    val result = runCatchingProvider(it) {
                        getLastestTorrents(category)
                    }
                    if (result.isSuccess) {
                        val torrents = result.getOrNull()!!
                        send(torrents)
                    }
                }
            }
        }
            .runningFold(persistentListOf<Torrent>()) { results, batchResult ->
                results.addAll(batchResult)
            }
            .drop(1)
            .flowOn(Dispatchers.IO)

    fun getTopTorrents(category: Category = Category.All): Flow<PersistentList<Torrent>> =
        channelFlow {
            searchProvidersManager.getEnabledTopTorrentsProviders(category).forEach {
                launch {
                    val result =
                        runCatchingProvider(it) { getTopTorrents(category) }
                    if (result.isSuccess) {
                        val torrents = result.getOrNull()!!
                        send(torrents)
                    }
                }
            }
        }
            .runningFold(persistentListOf<Torrent>()) { results, batchResult ->
                results.addAll(batchResult)
            }
            .drop(1)
            .flowOn(Dispatchers.IO)
}