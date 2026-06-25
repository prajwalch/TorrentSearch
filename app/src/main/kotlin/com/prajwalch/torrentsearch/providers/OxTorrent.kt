package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class OxTorrent : SearchProvider, LatestTorrentsProvider, TopTorrentsProvider,
    TorrentDetailsProvider {
    override val id = "oxtorrent"
    override val name = "OxTorrent"
    override val url = "https://oxtorrent.co"
    override val supportedCategories = setOf(
        Category.Apps,
        Category.Books,
        Category.Games,
        Category.Movies,
        Category.Music,
        Category.Series,
    )
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val categoryMap = mapOf(
        Category.Apps to "logiciels",
        Category.Books to "ebook",
        Category.Games to "jeux-pc",
        Category.Movies to "films",
        Category.Music to "musique",
        Category.Series to "series",
    )
    private val resultsPageParser = OxTorrentResultsPageParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/recherche")
            categoryMap[context.category]?.let { append("/$it") }
            append("/$query")
        }
        val responseHtml = context.httpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return OxTorrentDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val categorySlug = categoryMap[category] ?: return emptyList()
        val requestUrl = "$url/torrents/$categorySlug"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/top"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }
}

private class OxTorrentResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .map { async { parseListItem(it) } }
                .awaitAll()
                .filterNotNull()
        }

    private suspend fun parseListItem(listItem: Element): Torrent? {
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href") ?: return null
        val detailsPageHtml = HttpClient.get(detailsPageUrl)
        val torrentDetails = OxTorrentDetailsPageParser.parse(
            html = detailsPageHtml,
            pageUrl = detailsPageUrl,
        ) ?: return null

        return Torrent(
            infoHash = torrentDetails.infoHash,
            name = torrentDetails.name,
            size = torrentDetails.size ?: "0 KB",
            seeders = torrentDetails.seeders ?: 0U,
            peers = torrentDetails.peers ?: 0U,
            uploadDate = torrentDetails.uploadDate,
            providerName = providerName,
            category = torrentDetails.category,
            magnetUri = torrentDetails.magnetUri,
            descriptionPageUrl = detailsPageUrl,
        )
    }

    private companion object {
        private const val LIST_ITEM = "table > tbody > tr"
        private const val DETAILS_PAGE_URL = "td:nth-child(1) > a"
    }
}

private object OxTorrentDetailsPageParser {
    private const val TORRENT_NAME = "div.title > a"
    private const val SIZE = "td:containsOwn(Poids du fichier:)"
    private const val SEEDERS = "td:containsOwn(Seeders:)"
    private const val PEERS = "td:containsOwn(Leechers:)"
    private const val UPLOAD_DATE = "td:containsOwn(Date d\\'ajout:)"
    private const val CATEGORY = "td:containsOwn(Catégories:)"
    private const val MAGNET_URL = "div.btn-magnet > a"
    private const val DESCRIPTION = "div#torrentsdesc"
    private const val POSTER_URL = "img.img-rounded"

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val torrentName = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URL)?.attr("href") ?: return@withContext null
            val size = html.selectFirst(SIZE)?.nextElementSibling()?.text()
            val seeders = html.selectFirst(SEEDERS)?.nextElementSibling()?.text()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.nextElementSibling()?.text()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)
                ?.nextElementSibling()
                ?.text()
                ?.let { TorrentDateParser.parse(date = it, format = "dd/MM/yyyy") }
            val category = html.selectFirst(CATEGORY)
                ?.nextElementSibling()
                ?.selectFirst("strong > a")
                ?.attr("href")
                ?.removePrefix("/torrents/")
                ?.let(::getCategoryFromRaw)
            val description = html.selectFirst(DESCRIPTION)?.html()
            val posterUrl = html.selectFirst(POSTER_URL)?.attr("abs:src")

            TorrentDetails(
                infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
                magnetUri = magnetUri,
                name = torrentName,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = category,
                description = description,
                posterUrl = posterUrl,
            )
        }
}

private fun getCategoryFromRaw(raw: String) = when (raw) {
    "ebook" -> Category.Books
    "films" -> Category.Movies
    "jeux-consoles" -> Category.Games
    "jeux-pc" -> Category.Games
    "logiciels" -> Category.Apps
    "musique" -> Category.Music
    "series" -> Category.Series
    else -> Category.Other
}