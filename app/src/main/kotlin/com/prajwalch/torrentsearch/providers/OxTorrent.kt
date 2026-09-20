package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.domain.model.SearchProviderSafety
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.provider.LatestTorrentsProvider
import com.prajwalch.torrentsearch.provider.MagnetUriProvider
import com.prajwalch.torrentsearch.provider.SearchProvider
import com.prajwalch.torrentsearch.provider.SearchProviderId
import com.prajwalch.torrentsearch.provider.TopTorrentsProvider
import com.prajwalch.torrentsearch.provider.TorrentDetailsProvider
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class OxTorrent(private val networkClient: NetworkClient) :
    SearchProvider,
    LatestTorrentsProvider,
    TopTorrentsProvider,
    MagnetUriProvider,
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
    override val safety = SearchProviderSafety.Safe
    override val isCloudflareProtected = true
    override val enabledByDefault = false

    private val categoryMap = mapOf(
        Category.Apps to "logiciels",
        Category.Books to "ebook",
        Category.Games to "jeux-pc",
        Category.Movies to "films",
        Category.Music to "musique",
        Category.Series to "series",
    )
    private val resultsPageParser = OxTorrentResultsPageParser(id, name)

    override suspend fun search(query: String, category: Category): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/recherche")
            categoryMap[category]?.let { append("/$it") }
            append("/$query")
        }
        val responseHtml = networkClient.getText(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = networkClient.getText(detailsPageUrl)
        return OxTorrentDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val categorySlug = categoryMap[category] ?: return emptyList()
        val requestUrl = "$url/torrents/$categorySlug"
        val responseHtml = networkClient.getText(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/top"
        val responseHtml = networkClient.getText(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getMagnetUri(url: String): String {
        val detailsPageHtml = networkClient.getText(url)
        return OxTorrentDetailsPageParser.extractMagnetUri(detailsPageHtml)
            ?: error("Failed to retrieve magnet URI from '$url'")
    }
}

private class OxTorrentResultsPageParser(
    private val providerId: SearchProviderId,
    private val providerName: String,
) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull(::parseListItem)
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)
            ?.attr("abs:href")
            ?: return null
        val name = listItem.selectFirst(TORRENT_NAME)?.ownText() ?: return null
        val torrentId = TorrentUtils.createTorrentId(
            providerId = providerId,
            sourceId = detailsPageUrl,
        )
        val size = listItem.selectFirst(SIZE)?.ownText()
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val category = listItem.selectFirst(CATEGORY)?.className()?.let(::getCategoryFromRaw)

        return Torrent(
            id = torrentId,
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            providerName = providerName,
            category = category,
            magnetUri = MagnetUri.RequiresFetch(detailsPageUrl),
            descriptionPageUrl = detailsPageUrl,
        )
    }

    private companion object {
        private const val LIST_ITEM = "table > tbody > tr"
        private const val TORRENT_NAME = "td:nth-child(1) > a"
        private const val SIZE = "td:nth-child(2)"
        private const val SEEDERS = "td:nth-child(3)"
        private const val PEERS = "td:nth-child(4)"
        private const val CATEGORY = "td:nth-child(1) > i"
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

    suspend fun extractMagnetUri(detailsPageHtml: String): String? =
        withContext(Dispatchers.Default) {
            Jsoup.parse(detailsPageHtml).selectFirst(MAGNET_URL)?.attr("href")
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