package com.prajwalch.torrentsearch.data

import com.prajwalch.torrentsearch.database.BookmarkedTorrentDao
import com.prajwalch.torrentsearch.database.entities.BookmarkedTorrent
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.network.HttpClientResponse
import com.prajwalch.torrentsearch.providers.SearchContext
import com.prajwalch.torrentsearch.providers.SearchProvider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope

class TorrentsRepository(
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

    /** Starts a search for the given query. */
    suspend fun search(
        query: String,
        category: Category,
        providers: List<SearchProvider>,
    ): TorrentsRepositoryResult {
        val query = query.replace(' ', '+').trim()
        val context = SearchContext(category = category, httpClient = httpClient)

        return supervisorScope {
            val results = chooseSearchProviders(providers = providers, category = category)
                .map { async(Dispatchers.IO) { it.search(query = query, context = context) } }
                .map { httpClient.withExceptionHandler { it.await() } }

            // Check for network error.
            if (results.all { it is HttpClientResponse.Error.NetworkError }) {
                return@supervisorScope TorrentsRepositoryResult(isNetworkError = true)
            }

            val torrents = results
                .mapNotNull { httpClientResponse -> httpClientResponse as? HttpClientResponse.Ok }
                .flatMap { httpClientOkResponse -> httpClientOkResponse.result }
                .map { torrent ->
                    val bookmarkedInfo = bookmarkedTorrentDao.findByName(torrent.name)

                    bookmarkedInfo
                        ?.let { info -> torrent.copy(id = info.id, bookmarked = true) }
                        ?: torrent
                }

            TorrentsRepositoryResult(torrents = torrents)
        }
    }

    /**
     * Returns the search providers that is capable of handling the given
     * category.
     */
    private fun chooseSearchProviders(
        providers: List<SearchProvider>,
        category: Category,
    ): List<SearchProvider> {
        if (category == Category.All) {
            return providers
        }

        return providers.filter {
            (it.info.specializedCategory == Category.All) || (category == it.info.specializedCategory)
        }
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