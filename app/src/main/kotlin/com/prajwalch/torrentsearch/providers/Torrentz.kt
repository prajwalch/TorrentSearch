package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element

class Torrentz(private val networkClient: NetworkClient) : SearchProvider, LatestTorrentsProvider,
    TopTorrentsProvider,
    TorrentDetailsProvider {
    override val id = "torrentz"
    override val name = "Torrentz"
    override val url = "https://torrentz2.nz"
    override val supportedCategories = setOf(
        Category.Other,
        Category.Movies,
        Category.Series,
        Category.Anime,
        Category.Apps,
        Category.Games,
        Category.Music,
        Category.Books,
        Category.Porn,
    )
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val categoryMap = mapOf(
        Category.Other to 1,
        Category.Movies to 2,
        Category.Series to 3,
        Category.Anime to 4,
        Category.Apps to 5,
        Category.Games to 6,
        Category.Music to 7,
        Category.Books to 9,
        Category.Porn to 10,
    )
    private val resultsPageParser = TorrentzResultsPageParser(name, networkClient)

    override suspend fun search(query: String, category: Category): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/search")
            append("?q=$query")

            if (category != Category.All) {
                categoryMap[category]?.let { categoryId ->
                    append("&category=$categoryId")
                }
            }
        }
        val responseHtml = networkClient.getText(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/latest")

            if (category != Category.All) {
                categoryMap[category]?.let { categoryId ->
                    append("?category=$categoryId")
                }
            }
        }
        val responseHtml = networkClient.getText(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/trending")

            if (category != Category.All) {
                categoryMap[category]?.let { categoryId ->
                    append("?category=$categoryId")
                }
            }
        }
        val responseHtml = networkClient.getText(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = networkClient.getText(detailsPageUrl)
        return TorrentzDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }
}

private class TorrentzResultsPageParser(
    private val providerName: String,
    private val networkClient: NetworkClient,
) {
    private companion object {
        private const val LIST_ITEM = "div.results > dl"
        private const val DETAILS_PAGE_URL = "dt > a"
    }

    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Ksoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .map { async { parseListItem(it) } }
                .awaitAll()
                .filterNotNull()
        }

    private suspend fun parseListItem(listItem: Element): Torrent? {
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href") ?: return null
        val detailsPageHtml = networkClient.getText(detailsPageUrl)
        val torrentDetails = TorrentzDetailsPageParser.parse(
            html = detailsPageHtml,
            pageUrl = detailsPageUrl
        ) ?: return null

        return Torrent(
            infoHash = torrentDetails.infoHash,
            name = torrentDetails.name,
            size = torrentDetails.size,
            seeders = torrentDetails.seeders,
            peers = torrentDetails.peers,
            uploadDate = torrentDetails.uploadDate,
            category = torrentDetails.category,
            providerName = providerName,
            magnetUri = torrentDetails.magnetUri,
            fileDownloadLink = torrentDetails.fileDownloadLink,
            descriptionPageUrl = detailsPageUrl,
        )
    }
}

private object TorrentzDetailsPageParser {
    private const val TORRENT_NAME = "div.download > h2"
    private const val SIZE_LABEL = "td:containsOwn(Size)"
    private const val SEEDERS_LABEL = "td:containsOwn(Seeds)"
    private const val PEERS_LABEL = "td:containsOwn(Leechers)"
    private const val UPLOAD_DATE_LABEL = "td:containsOwn(Added)"
    private const val CATEGORY_ID = "div.download > p:last-child > a"
    private const val MAGNET_URI = """div.download a[href^="magnet:?"]"""
    private const val FILE_DOWNLOAD_LINK = """div.download a[href^="/dowload/torrent/"]"""

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Ksoup.parse(html, pageUrl)
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val torrentName = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
            val size = html.selectFirst(SIZE_LABEL)?.nextElementSibling()?.ownText()
            val seeders = html.selectFirst(SEEDERS_LABEL)
                ?.nextElementSibling()
                ?.ownText()
                ?.toUIntOrNull()
            val peers = html.selectFirst(PEERS_LABEL)
                ?.nextElementSibling()
                ?.ownText()
                ?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE_LABEL)
                ?.nextElementSibling()
                ?.ownText()
                ?.let { TorrentDateParser.parse(date = it, format = "M/d/yyyy") }
            val category = html.selectFirst(CATEGORY_ID)
                ?.attr("href")
                ?.takeLastWhile { it != '=' }
                ?.toIntOrNull()
                ?.let(::categoryFromId)
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")

            TorrentDetails(
                infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
                name = torrentName,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = category,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
            )
        }
}

private fun categoryFromId(id: Int): Category = when (id) {
    1 -> Category.Other
    2 -> Category.Movies
    3 -> Category.Series
    4 -> Category.Anime
    5 -> Category.Apps
    6 -> Category.Games
    7 -> Category.Music
    9 -> Category.Books
    10 -> Category.Porn
    else -> Category.Other
}