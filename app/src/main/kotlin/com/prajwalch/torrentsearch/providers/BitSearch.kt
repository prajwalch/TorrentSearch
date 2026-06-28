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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import java.time.format.DateTimeParseException

class BitSearch :
    SearchProvider,
    TorrentDetailsProvider,
    LatestTorrentsProvider,
    TopTorrentsProvider {
    override val id = "bitsearch"
    override val name = "BitSearch"
    override val url = "https://bitsearch.to"
    override val supportedCategories: Set<Category> = setOf(
        Category.Anime,
        Category.Apps,
        Category.Books,
        Category.Games,
        Category.Movies,
        Category.Music,
        Category.Porn,
        Category.Series,
        Category.Other,
    )
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false
    override val type = SearchProviderType.Builtin

    private val resultsPageParser = BitSearchResultsPageParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> =
        coroutineScope {
            val categoryId = getCategoryId(category = context.category)
            val searches = (1..5).map { page ->
                async(Dispatchers.IO) {
                    search(
                        query = query,
                        categoryId = categoryId,
                        httpClient = context.httpClient,
                        page = page,
                    )
                }
            }

            searches.awaitAll().flatten()
        }

    private suspend fun search(
        query: String,
        categoryId: Int?,
        httpClient: HttpClient,
        page: Int,
    ): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/search")
            append("?q=$query")
            append("&page=$page")
            append("&sortBy=seeders")

            // Weird way to handle 'Category.All'
            if (categoryId != null) {
                append("&category=$categoryId")
            }
        }

        val responseHtml = httpClient.get(url = requestUrl)
        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    private fun getCategoryId(category: Category): Int? = when (category) {
        Category.All -> null
        Category.Anime -> 4
        Category.Apps -> 5
        Category.Books -> 9
        Category.Games -> 6
        Category.Movies -> 2
        Category.Music -> 7
        Category.Porn -> 10
        Category.Series -> 3
        Category.Other -> 1
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return BitSearchDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/latest")
            getCategoryId(category)?.let { append("?category=$it") }
        }
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/trending")
            getCategoryId(category)?.let { append("?category=$it") }
        }
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }
}

private class BitSearchResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup
                .parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull { parseListItem(it) }
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val name = listItem.selectFirst(TORRENT_NAME)?.text() ?: return null
        val magnetUri = listItem.selectFirst(MAGNET_LINK)?.attr("href") ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()
        val peers = listItem.selectFirst(PEERS)?.ownText()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)?.ownText()?.let {
            try {
                TorrentDateParser.parse(date = it, format = "M/d/yyyy")
            } catch (_: DateTimeParseException) {
                TorrentDateParser.tryParseRelative(it)
            }
        }
        val category = listItem.selectFirst(CATEGORY)?.ownText()?.let(::categoryFromRawString)
        val fileDownloadLink = listItem.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = name,
            size = size,
            seeders = seeders?.toUIntOrNull(),
            peers = peers?.toUIntOrNull(),
            providerName = providerName,
            uploadDate = uploadDate,
            category = category,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
            descriptionPageUrl = detailsPageUrl,
        )
    }

    private companion object {
        private const val LIST_ITEM = "div.space-y-4 > div > div:nth-child(1)"
        private const val TORRENT_INFO = "> div:nth-child(1)"
        private const val DOWNLOAD_LINKS = "> div:nth-child(2)"
        private const val CATEGORY_AND_METADATA = "$TORRENT_INFO > div:nth-last-child(2)"
        private const val SWARM_STATS = "$TORRENT_INFO > div:nth-last-child(1)"
        private const val TORRENT_NAME = "$TORRENT_INFO h3"
        private const val SIZE = "$CATEGORY_AND_METADATA > span:nth-child(2) > span"
        private const val SEEDERS = "$SWARM_STATS > span:nth-child(1) > span:nth-child(2)"
        private const val PEERS = "$SWARM_STATS > span:nth-child(2) > span:nth-child(2)"
        private const val UPLOAD_DATE = "$CATEGORY_AND_METADATA > span:nth-child(3) > span"
        private const val CATEGORY = "$CATEGORY_AND_METADATA > span:nth-child(1) > span"
        private const val MAGNET_LINK = "$DOWNLOAD_LINKS > a:nth-child(2)"
        private const val FILE_DOWNLOAD_LINK = "$DOWNLOAD_LINKS > a:nth-child(1)"
        private const val DETAILS_PAGE_URL = "$TORRENT_NAME > a"
    }
}

private object BitSearchDetailsPageParser {
    private const val NAME =
        "body > div > main > div > main > div > div.lg\\:col-span-2.space-y-4.sm\\:space-y-6.lg\\:space-y-8 > div.bg-white.rounded-xl.shadow-lg.border.border-gray-200.overflow-hidden > div.bg-gradient-to-r.from-blue-600.to-purple-600.p-3.sm\\:p-4.text-white > div > div.flex.flex-col.sm\\:flex-row.gap-3.sm\\:gap-4 > div.flex-1.min-w-0.text-center.sm\\:text-left > h1"
    private const val SIZE =
        "body > div > main > div > main > div > div.lg\\:col-span-2.space-y-4.sm\\:space-y-6.lg\\:space-y-8 > div.bg-white.rounded-xl.shadow-lg.border.border-gray-200.overflow-hidden > div.bg-gradient-to-r.from-blue-600.to-purple-600.p-3.sm\\:p-4.text-white > div > div.grid.grid-cols-2.sm\\:grid-cols-4.gap-2.sm\\:gap-3 > div:nth-child(3) > div.text-lg.sm\\:text-xl.font-bold.text-white"
    private const val SEEDERS =
        "body > div > main > div > main > div > div.lg\\:col-span-2.space-y-4.sm\\:space-y-6.lg\\:space-y-8 > div.bg-white.rounded-xl.shadow-lg.border.border-gray-200.overflow-hidden > div.bg-gradient-to-r.from-blue-600.to-purple-600.p-3.sm\\:p-4.text-white > div > div.grid.grid-cols-2.sm\\:grid-cols-4.gap-2.sm\\:gap-3 > div:nth-child(1) > div.text-lg.sm\\:text-xl.font-bold.text-white"
    private const val PEERS =
        "body > div > main > div > main > div > div.lg\\:col-span-2.space-y-4.sm\\:space-y-6.lg\\:space-y-8 > div.bg-white.rounded-xl.shadow-lg.border.border-gray-200.overflow-hidden > div.bg-gradient-to-r.from-blue-600.to-purple-600.p-3.sm\\:p-4.text-white > div > div.grid.grid-cols-2.sm\\:grid-cols-4.gap-2.sm\\:gap-3 > div:nth-child(2) > div.text-lg.sm\\:text-xl.font-bold.text-white"
    private const val UPLOAD_DATE =
        "body > div > main > div > main > div > div.lg\\:col-span-2.space-y-4.sm\\:space-y-6.lg\\:space-y-8 > div.flex.flex-col.gap-6 > div:nth-child(1) > div.p-6 > div.grid.grid-cols-1.md\\:grid-cols-2.gap-8 > div.space-y-4 > div > div:nth-child(1) > div.text-sm.font-bold.text-gray-900"
    private const val LAST_CHECKED =
        "body > div > main > div > main > div > div.lg\\:col-span-2.space-y-4.sm\\:space-y-6.lg\\:space-y-8 > div.flex.flex-col.gap-6 > div:nth-child(1) > div.p-6 > div.grid.grid-cols-1.md\\:grid-cols-2.gap-8 > div.space-y-4 > div > div:nth-child(2) > div.text-sm.font-bold.text-gray-900"
    private const val CATEGORY =
        "body > div > main > div > main > div > div.lg\\:col-span-2.space-y-4.sm\\:space-y-6.lg\\:space-y-8 > div.bg-white.rounded-xl.shadow-lg.border.border-gray-200.overflow-hidden > div.bg-gradient-to-r.from-blue-600.to-purple-600.p-3.sm\\:p-4.text-white > div > div.flex.flex-col.sm\\:flex-row.gap-3.sm\\:gap-4 > div.flex-1.min-w-0.text-center.sm\\:text-left > div.flex.flex-wrap.justify-center.sm\\:justify-start.gap-1\\.5.sm\\:gap-2.mb-2.sm\\:mb-3 > span.inline-flex.items-center.px-2.sm\\:px-3.py-1.rounded-full.text-xs.font-semibold.bg-white.bg-opacity-20.text-white.backdrop-blur-sm"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""
    private const val FILE_DOWNLOAD_LINK = """a[href^="/download/torrent/"]"""
    private const val DATE_FORMAT = "M/d/yyyy"

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val name = html.selectFirst(NAME)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
            val size = html.selectFirst(SIZE)?.ownText()
            val seeders = html.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)?.ownText()
                ?.let { TorrentDateParser.parse(date = it, format = DATE_FORMAT) }
            val lastChecked = html.selectFirst(LAST_CHECKED)?.ownText()
                ?.let { TorrentDateParser.parse(date = it, format = DATE_FORMAT) }
            val category = html.selectFirst(CATEGORY)?.ownText()?.let(::categoryFromRawString)
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")

            TorrentDetails(
                infoHash = infoHash,
                name = name,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = category,
                lastChecked = lastChecked,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
            )
        }
}

private fun categoryFromRawString(raw: String): Category = when (raw) {
    "Other",
    "Other/Audio",
    "Other/Video",
    "Other/Image",
    "Other/Document",
    "Other/Program",
    "Other/Android",
    "Other/DiskImage",
    "Other/Source Code",
    "Other/Database",
    "Other/Archive",
        -> Category.Other

    "Movies",
    "Movies/Dub/Dual Audio",
        -> Category.Movies

    "TV" -> Category.Series

    "Anime",
    "Anime/Dub/Dual Audio",
    "Anime/Subbed",
    "Anime/Raw",
        -> Category.Anime

    "Softwares",
    "Softwares/Windows",
    "Softwares/Mac",
    "Softwares/Android",
        -> Category.Apps

    "Games",
    "Games/PC",
    "Games/Mac",
    "Games/Linux",
    "Games/Android",
        -> Category.Games

    "Music",
    "Music/mp3",
    "Music/Lossless",
    "Music/Album",
    "Music/Video",
        -> Category.Music

    "AudioBook",
    "Ebook/Course",
        -> Category.Books

    "XXX" -> Category.Porn

    else -> Category.Other
}