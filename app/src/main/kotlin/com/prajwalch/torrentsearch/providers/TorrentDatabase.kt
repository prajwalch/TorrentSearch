package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.domain.model.SearchProviderSafety
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.provider.LatestTorrentsProvider
import com.prajwalch.torrentsearch.provider.SearchProvider
import com.prajwalch.torrentsearch.provider.SearchProviderId
import com.prajwalch.torrentsearch.provider.TopTorrentsProvider
import com.prajwalch.torrentsearch.provider.TorrentDetailsProvider
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class TorrentDatabase(private val networkClient: NetworkClient) :
    SearchProvider,
    TorrentDetailsProvider,
    LatestTorrentsProvider,
    TopTorrentsProvider {
    override val id = "torrentdatabase"
    override val name = "TorrentDatabase"
    override val url = "https://developify.ca"
    override val cloudflareSolverUrl = "$url/search?q=ubuntu"
    override val supportedCategories = setOf(
        Category.Apps,
        Category.Books,
        Category.Games,
        Category.Movies,
        Category.Music,
        Category.Porn,
        Category.Series,
    )
    override val safety = SearchProviderSafety.Safe
    override val isCloudflareProtected = true
    override val enabledByDefault = false

    private val categoryMap = mapOf(
        Category.Apps to "software",
        Category.Books to "e-books",
        Category.Games to "games",
        Category.Movies to "movies",
        Category.Music to "music",
        Category.Porn to "porn",
        Category.Series to "tv",
    )

    private val resultsPageParser = TdResultsPageParser(id, name)

    override suspend fun search(query: String, category: Category): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/search")
            append("?q=$query")

            categoryMap[category]?.let {
                append("&category=$it")
            }
        }

        val responseHtml = networkClient.getText(url = requestUrl)
        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = networkClient.getText(detailsPageUrl)
        return TdDetailsPageParser.parse(responseHtml)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/newest")

            if (category != Category.All) {
                categoryMap[category]
                    ?.let { if (category == Category.Books) it.replace("-", "") else it }
                    ?.let { append("_$it") }
            }
        }
        val responseHtml = networkClient.getText(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        if (category == Category.All) return getLastestTorrents()

        val categoryString = categoryMap[category]?.let {
            if (category == Category.Books) it.replace("-", "") else it
        } ?: return emptyList()

        val requestUrl = "$url/top_seeded_$categoryString"
        val responseHtml = networkClient.getText(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }
}

private class TdResultsPageParser(
    private val providerId: SearchProviderId,
    private val providerName: String,
) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(RESULT_LIST_ITEM)
                .mapNotNull(::parseListItem)
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val torrentNameElm = listItem.selectFirst(TORRENT_NAME) ?: return null
        val torrentName = torrentNameElm.ownText()
        val infoHash = torrentNameElm.attr("href")
            .removePrefix("/track/magnet/")
            .takeWhile { it != '?' }
        val magnetUri = TorrentUtils.createMagnetUri(infoHash).let {
            "$it&tr=$TORRENT_DATABASE_TRACKER_URL"
        }
        val descriptionPageUrl = listItem.selectFirst(DESCRIPTION_PAGE_URL)
            ?.attr("abs:href")
            ?.takeIf { it.isNotBlank() }
        val category = listItem.selectFirst(CATEGORY)?.ownText()?.let(::categoryFromRawString)
        val size = listItem.selectFirst(SIZE)?.ownText()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)
            ?.ownText()
            ?.let { TorrentDateParser.parse(date = it, format = "yyyy-MM-dd HH:mm:ss") }
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()
        val peers = listItem.selectFirst(PEERS)?.ownText()

        val torrentRemoteId = descriptionPageUrl?.takeLastWhile { it != '/' }
        val torrentId = TorrentUtils.createTorrentId(
            providerId = providerId,
            sourceId = torrentRemoteId ?: infoHash,
        )

        return Torrent(
            id = torrentId,
            name = torrentName,
            size = size,
            seeders = seeders?.toUIntOrNull(),
            peers = peers?.toUIntOrNull(),
            uploadDate = uploadDate,
            category = category,
            providerName = providerName,
            magnetUri = MagnetUri.Available(magnetUri),
            detailsPageUrl = descriptionPageUrl,
        )
    }

    private companion object {
        private const val TORRENT_DATABASE_TRACKER_URL = "https%3A%2F%2Fdevelopify.ca%2Fannounce"
        private const val RESULT_LIST_ITEM = "table.torrent-table > tbody > tr"
        private const val TORRENT_NAME = "td:nth-child(1) > a:nth-child(2)"
        private const val SIZE = "td.size-cell"
        private const val SEEDERS = "td:nth-child(5) > div > span:nth-child(1)"
        private const val PEERS = "td:nth-child(5) > div > span:nth-child(3)"
        private const val UPLOAD_DATE = "td.date-cell"
        private const val CATEGORY = "td:nth-child(2) > span.category-bubble"
        private const val DESCRIPTION_PAGE_URL = "td:nth-child(1) > a:nth-child(1)"
    }
}

private object TdDetailsPageParser {
    private const val TORRENT_NAME = "article.torrent-detail-card > header > h1"
    private const val SIZE = "div.detail-stat-grid > div:nth-child(1) > strong"
    private const val SEEDERS = "div.detail-stat-grid > div:nth-child(2) > strong"
    private const val PEERS = "div.detail-stat-grid > div:nth-child(3) > strong"
    private const val UPLOAD_DATE = "dl.detail-metadata > div:nth-child(1) > dd"
    private const val CATEGORY = "span.category-bubble"
    private const val UPLOADER = "div.detail-uploader > a"
    private const val LAST_CHECKED = "dl.detail-metadata > div:nth-child(2) > dd"
    private const val DESCRIPTION = "div.torrent-info-content"
    private const val MAGNET_URI = "#downloadMagnetBtn"

    suspend fun parse(html: String): TorrentDetails? = withContext(Dispatchers.Default) {
        val dom = Jsoup.parse(html)

        val name = dom.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
        val magnetUri = dom.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)

        val size = dom.selectFirst(SIZE)?.ownText()?.let(FileSizeUtils::normalizeSize)
        val seeders = dom.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = dom.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val uploadDate = dom.selectFirst(UPLOAD_DATE)
            ?.ownText()
            ?.let { rawDate ->
                runCatching {
                    TorrentDateParser.parse(date = rawDate, format = "yyyy-MM-dd HH:mm:ssxxx")
                }.recoverCatching {
                    TorrentDateParser.parse(date = rawDate, format = "yyyy-MM-dd HH:mm:ss")
                }
            }
            ?.getOrNull()
        val category = dom.selectFirst(CATEGORY)
            ?.attr("data-category")
            ?.let(::categoryFromRawString)
        val uploader = dom.selectFirst(UPLOADER)?.ownText()
        val lastChecked = dom.selectFirst(LAST_CHECKED)
            ?.ownText()
            ?.let {
                runCatching {
                    TorrentDateParser.parse(date = it, format = "yyyy-MM-dd HH:mm:ss")
                }
            }
            ?.getOrNull()
        val description = dom.selectFirst(DESCRIPTION)?.html()

        TorrentDetails(
            infoHash = infoHash,
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            uploadDate = uploadDate,
            category = category,
            uploader = uploader,
            lastChecked = lastChecked,
            magnetUri = magnetUri,
            description = description,
        )
    }
}

private fun categoryFromRawString(raw: String) = when (raw.lowercase()) {
    "software" -> Category.Apps
    "e-books", "ebooks", "audiobooks" -> Category.Books
    "games" -> Category.Games
    "movies" -> Category.Movies
    "music" -> Category.Music
    "porn", "xxx" -> Category.Porn
    "tv" -> Category.Series
    else -> when {
        raw.startsWith("games") -> Category.Games
        raw.startsWith("movies") -> Category.Movies
        raw.startsWith("music") -> Category.Music
        raw.startsWith("software") -> Category.Apps
        raw.startsWith("tv") -> Category.Series
        else -> Category.Other
    }
}