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

class TorrentDownloads : SearchProvider, LatestTorrentsProvider, TopTorrentsProvider,
    TorrentDetailsProvider {
    override val id = "torrentdownloads"
    override val name = "TorrentDownloads"
    override val url = "https://torrentdownloads.pro"
    override val cloudflareSolverUrl = "$url/search/?s_cat=0&search=ubuntu"
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val supportedCategories = setOf(
        Category.Anime,
        Category.Apps,
        Category.Books,
        Category.Games,
        Category.Movies,
        Category.Music,
        Category.Series,
        Category.Other,
    )
    override val isCloudflareProtected = true
    override val enabledByDefault = true

    private val resultsPageParser = TorrentDownloadsResultsPageParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/search")

            val categoryId = getCategoryId(category = context.category)
            append("/?s_cat=$categoryId")
            append("&search=$query")
        }
        val responseHtml = context.httpClient.get(url = requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    private fun getCategoryId(category: Category) = when (category) {
        Category.All -> 0
        Category.Anime -> 1
        Category.Apps -> 7
        Category.Books -> 2
        Category.Games -> 3
        Category.Movies -> 4
        Category.Music -> 5
        Category.Series -> 8
        Category.Porn, Category.Other -> 9
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = when (category) {
            Category.Anime -> "$url/view/today/Anime.html"
            Category.Apps -> "$url/view/today/Software.html"
            Category.Books -> "$url/view/today/Books.html"
            Category.Games -> "$url/view/today/Games.html"
            Category.Movies -> "$url/view/today/Movies.html"
            Category.Music -> "$url/view/today/Music.html"
            Category.Series -> "$url/view/today/TV_Shows.html"
            Category.Other -> "$url/view/today/Other.html"
            else -> "$url/most-active"
        }
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        val requestUrl = when (category) {
            Category.Anime -> "$url/view/popular/Anime.html"
            Category.Apps -> "$url/view/popular/Software.html"
            Category.Books -> "$url/view/popular/Books.html"
            Category.Games -> "$url/view/popular/Games.html"
            Category.Movies -> "$url/view/popular/Movies.html"
            Category.Music -> "$url/view/popular/Music.html"
            Category.Series -> "$url/view/popular/TV_Shows.html"
            Category.Other -> "$url/view/popular/Other.html"
            else -> "$url/most-seeded"
        }
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return TorrentDownloadsDetailsPageParser.parse(
            html = responseHtml,
            pageUrl = detailsPageUrl,
        )
    }
}

private class TorrentDownloadsResultsPageParser(private val providerName: String) {
    private companion object {
        private const val LIST_ITEM_CONTAINER = "div.inner_container"
        private const val LIST_ITEM = "div.grey_bar3"
        private const val TORRENT_NAME = "p:nth-child(1) > a:nth-child(2)"
        private const val SIZE = "span:nth-child(5)"
        private const val SEEDERS = "span:nth-child(4)"
        private const val PEERS = "span:nth-child(3)"
        private const val CATEGORY = "p:nth-child(1) > img:nth-child(1)"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }

    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default.limitedParallelism(3)) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM_CONTAINER)
                .last()
                ?.select(LIST_ITEM)
                ?.drop(2)
                ?.map { async { parseListItem(it) } }
                ?.awaitAll()
                ?.filterNotNull()
                .orEmpty()
        }

    private suspend fun parseListItem(listItem: Element): Torrent? {
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)
            ?.attr("abs:href")
            ?: return null
        val detailsPageHtml = HttpClient.get(detailsPageUrl)
        val torrentDetails = TorrentDownloadsDetailsPageParser.parse(
            html = detailsPageHtml,
            pageUrl = detailsPageUrl,
        ) ?: return null

        val size = torrentDetails.size
            ?: listItem.selectFirst(SIZE)?.ownText()
        val seeders = torrentDetails.seeders
            ?: listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = torrentDetails.peers
            ?: listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val uploadDate = torrentDetails.uploadDate
        val category = torrentDetails.category
            ?: listItem.selectFirst(CATEGORY)
                ?.attr("src")
                ?.let(::getCategoryFromCategoryIconUrl)

        return Torrent(
            infoHash = torrentDetails.infoHash,
            name = torrentDetails.name,
            size = size,
            seeders = seeders,
            peers = peers,
            providerName = providerName,
            uploadDate = uploadDate,
            category = category,
            descriptionPageUrl = detailsPageUrl,
            magnetUri = torrentDetails.magnetUri,
        )
    }
}

private object TorrentDownloadsDetailsPageParser {
    private const val TORRENT_NAME = "div.inner_container > h1.titl_1 > span"
    private const val SIZE = "div.inner_container > div:nth-child(13) > p"
    private const val SEEDERS = "div.inner_container > div:nth-child(15) > p"
    private const val PEERS = "div.inner_container > div:nth-child(16) > p"
    private const val UPLOAD_DATE = "div.inner_container > div:nth-child(19) > p"
    private const val CATEGORY = "div.inner_container > h1:nth-child(1) > img"
    private const val LAST_CHECKED = "div.inner_container > div:nth-child(18) > p"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""
    private const val FILE_DOWNLOAD_LINK = "u.download > li:nth-child(2) > a"
    private const val DATE_FORMAT = "uuuu-MM-dd HH:mm:ss"

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val torrentName = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val size = html.selectFirst(SIZE)?.ownText()
            val seeders = html.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)
                ?.ownText()
                ?.trim()
                ?.runCatching { TorrentDateParser.parse(date = this, format = DATE_FORMAT) }
                ?.getOrNull()
            val category = html.selectFirst(CATEGORY)
                ?.attr("src")
                ?.let(::getCategoryFromCategoryIconUrl)
            val lastChecked = html.selectFirst(LAST_CHECKED)
                ?.ownText()
                ?.removeSuffix(" ()")
                ?.trim()
                ?.runCatching { TorrentDateParser.parse(date = this, format = DATE_FORMAT) }
                ?.getOrNull()
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")

            TorrentDetails(
                infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
                name = torrentName,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = category,
                lastChecked = lastChecked,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink
            )
        }
}

private fun getCategoryFromCategoryIconUrl(url: String): Category =
    when (url.removePrefix("/templates/new/images/icons/")) {
        "menu_icon0.png" -> Category.All
        "menu_icon1.png" -> Category.Anime
        "menu_icon2.png" -> Category.Books
        "menu_icon3.png" -> Category.Games
        "menu_icon4.png" -> Category.Movies
        "menu_icon5.png" -> Category.Music
        "menu_icon7.png" -> Category.Apps
        "menu_icon8.png" -> Category.Series
        "menu_icon9.png" -> Category.Other
        else -> Category.Other
    }