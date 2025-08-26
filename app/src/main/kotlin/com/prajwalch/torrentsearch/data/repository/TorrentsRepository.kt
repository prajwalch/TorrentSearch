package com.prajwalch.torrentsearch.data.repository

import android.util.Log

import com.prajwalch.torrentsearch.data.database.dao.BookmarkedTorrentDao
import com.prajwalch.torrentsearch.data.database.entities.BookmarkedTorrent
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.network.HttpClientResponse
import com.prajwalch.torrentsearch.providers.SearchContext
import com.prajwalch.torrentsearch.providers.SearchProvider

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

import javax.inject.Inject

class TorrentsRepository @Inject constructor(
    /** Uses for remote search. */
    private val httpClient: HttpClient,
    /** Uses for bookmarking torrents. */
    private val bookmarkedTorrentDao: BookmarkedTorrentDao,
) {
    /** Currently bookmarked torrents. */
    val bookmarkedTorrents: Flow<List<Torrent>> = bookmarkedTorrentDao
        .getAll()
        .map { bookmarkedTorrents -> bookmarkedTorrents.map { it.toModel() } }

    /** Bookmarks the given torrent. */
    suspend fun bookmarkTorrent(torrent: Torrent) {
        bookmarkedTorrentDao.insert(bookmarkedTorrent = torrent.toEntity())
    }

    /** Deletes the given bookmarked torrent. */
    suspend fun deleteBookmarkedTorrent(torrent: Torrent) {
        bookmarkedTorrentDao.delete(bookmarkedTorrent = torrent.toEntity())
    }

    /** Deletes all bookmarks. */
    suspend fun deleteAllBookmarks() {
        bookmarkedTorrentDao.deleteAll()
    }

    /** Starts a search from the network. */
    fun search(
        query: String,
        category: Category,
        searchProviders: List<SearchProvider>,
    ): Flow<TorrentsRepositoryResult> = channelFlow {
        Log.i(TAG, "search() called")

        val providersName = searchProviders.map { it.info.name }
        Log.d(TAG, "query=$query, category=$category, providers=$providersName")

        val searchProviders = chooseSearchProviders(
            searchProviders = searchProviders,
            category = category,
        )

        if (searchProviders.isEmpty()) {
            Log.i(TAG, "Search providers are empty. Returning...")
            return@channelFlow
        }

        Log.d(TAG, "Searching with ${searchProviders.map { it.info.name }}")

        val query = query.replace(' ', '+').trim()
        val context = SearchContext(category = category, httpClient = httpClient)

        searchProviders.forEach { searchProvider ->
            Log.i(TAG, "Launching ${searchProvider.info.name} (${searchProvider.info.id})")

            launch {
                val httpClientResponse = httpClient.withExceptionHandler {
                    searchProvider.search(query = query, context = context)
                }
                Log.i(TAG, "Received response from ${searchProvider.info.name}")

                when (httpClientResponse) {
                    is HttpClientResponse.Error.NetworkError -> {
                        Log.i(TAG, "Got network error response")
                        send(TorrentsRepositoryResult(isNetworkError = true))
                    }

                    is HttpClientResponse.Ok -> {
                        Log.i(TAG, "Got ${httpClientResponse.result.size} results")

                        if (httpClientResponse.result.isNotEmpty()) {
                            val torrents = checkForBookmarkedTorrents(
                                torrents = httpClientResponse.result
                            )

                            send(TorrentsRepositoryResult(torrents = torrents))
                        }
                    }

                    // If needed, handle other cases too.
                    else -> {
                        Log.d(TAG, "Unhandled response ($httpClientResponse)")
                    }
                }
            }
        }
    }

    /** Checks and updates the bookmark state of the given torrents. */
    private suspend fun checkForBookmarkedTorrents(torrents: List<Torrent>) =
        torrents.map { torrent ->
            bookmarkedTorrentDao
                .findByName(name = torrent.name)
                ?.let { entity -> torrent.copy(id = entity.id, bookmarked = true) }
                ?: torrent
        }

    /**
     * Returns the search providers that is capable of handling the given
     * category.
     */
    private fun chooseSearchProviders(
        searchProviders: List<SearchProvider>,
        category: Category,
    ): List<SearchProvider> {
        if (category == Category.All) {
            return searchProviders
        }

        return searchProviders.filter {
            (it.info.specializedCategory == Category.All) || (category == it.info.specializedCategory)
        }
    }

    private companion object {
        private const val TAG = "TorrentsRepository"
    }
}

data class TorrentsRepositoryResult(
    val torrents: List<Torrent>? = null,
    /** Indicates whether network error happened or not. */
    val isNetworkError: Boolean = false,
)

private fun Torrent.toEntity() = BookmarkedTorrent(
    id = id,
    name = name,
    size = size,
    seeders = seeders.toInt(),
    peers = peers.toInt(),
    providerId = providerId,
    providerName = providerName,
    uploadDate = uploadDate,
    category = category?.name ?: "",
    descriptionPageUrl = descriptionPageUrl,
    magnetUri = magnetUri(),
)

private fun BookmarkedTorrent.toModel() = Torrent(
    id = id,
    name = name,
    size = size,
    seeders = seeders.toUInt(),
    peers = peers.toUInt(),
    providerId = providerId,
    providerName = providerName,
    uploadDate = uploadDate,
    category = if (category.isNotEmpty()) Category.valueOf(category) else null,
    descriptionPageUrl = descriptionPageUrl,
    infoHashOrMagnetUri = InfoHashOrMagnetUri.MagnetUri(uri = magnetUri),
    bookmarked = true,
)