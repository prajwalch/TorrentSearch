package com.prajwalch.torrentsearch.domain

import android.net.Uri
import android.util.Log

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.GetTorrentDetailsResponse
import com.prajwalch.torrentsearch.domain.model.MaxNumResults
import com.prajwalch.torrentsearch.domain.model.SearchProviderError
import com.prajwalch.torrentsearch.domain.model.SearchProviderFailureReason
import com.prajwalch.torrentsearch.domain.model.SearchProviderResult
import com.prajwalch.torrentsearch.domain.model.SearchResults
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.network.CloudflareChallengeException
import com.prajwalch.torrentsearch.provider.SearchProvider

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

class TorrentQueryService(
    private val searchProviderManager: SearchProviderManager,
    private val settingsRepository: SettingsRepository,
) {
    private companion object {
        private const val TAG = "SearchProvidersGateway"
    }

    private val torrentIdToMagnetUri = mutableMapOf<String, String>()

    fun searchTorrents(query: String, category: Category): Flow<SearchResults> = flow {
        val limit = settingsRepository.maxNumResults.firstOrNull() ?: MaxNumResults.Unlimited
        emitAll(searchTorrents(query, category, limit))
    }

    private fun searchTorrents(
        query: String,
        category: Category,
        limit: MaxNumResults,
    ): Flow<SearchResults> = channelFlow {
        val enabledProviders = searchProviderManager.getEnabledProviders(category)
        if (enabledProviders.isEmpty()) return@channelFlow

        val encodedQuery = Uri.encode(query)!!
        enabledProviders.forEach {
            launch {
                val result = runCatchingProvider(it) { search(encodedQuery, category) }
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
        searchProviderManager.getEnabledLatestTorrentsProviders(category).forEach {
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
        searchProviderManager.getEnabledTopTorrentsProviders(category).forEach {
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
        val detailsProvider = searchProviderManager.findDetailsProviderByUrl(detailsPageUrl)
            ?: searchProviderManager.findDetailsProviderByName(providerName)
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

        val failureReason = if (cause is CloudflareChallengeException) {
            Log.i(TAG, "Locking ${provider.name} (${provider.id})")
            searchProviderManager.lockProvider(provider.id)

            SearchProviderFailureReason.CloudflareChallenge
        } else {
            SearchProviderFailureReason.Crash
        }

        val error = SearchProviderError(
            providerName = provider.name,
            providerUrl = provider.url,
            failureReason = failureReason,
            cause = cause,
        )
        SearchProviderResult.Error(error)
    }

    suspend fun getMagnetUri(
        torrentId: String,
        url: String,
        providerName: String,
    ): GetMagnetUriResult {
        val cachedMagnetUri = torrentIdToMagnetUri[torrentId]
        if (cachedMagnetUri != null) {
            return GetMagnetUriResult.Success(cachedMagnetUri)
        }

        val magnetUriProvider = searchProviderManager.findMagnetUriProviderByName(providerName)
            ?: error("Couldn't find magnet URI provider named '$providerName'")

        return runCatching { magnetUriProvider.getMagnetUri(url) }
            .fold(
                onSuccess = {
                    torrentIdToMagnetUri[torrentId] = it
                    GetMagnetUriResult.Success(it)
                },
                onFailure = {
                    if (it is CancellationException) throw it
                    GetMagnetUriResult.Error(it)
                },
            )
    }
}

sealed interface GetMagnetUriResult {
    data class Success(val magnetUri: String) : GetMagnetUriResult

    data class Error(val cause: Throwable) : GetMagnetUriResult
}