package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.GetTorrentDetailsResponse
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.DateUtils
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class BitSearch : SearchProvider {
    override val info = SearchProviderInfo(
        id = "bitsearch",
        name = "BitSearch",
        url = "https://bitsearch.to",
        specializedCategory = Category.All,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
        type = SearchProviderType.Builtin,
    )

    private val resultsPageParser = BitSearchResultsPageParser(info.name)

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
            append(info.url)
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
        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl).orEmpty()
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

    override suspend fun getDetails(detailsPageUrl: String): GetTorrentDetailsResponse {
        val responseHtml = HttpClient.get(detailsPageUrl)

        return BitSearchDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
            ?.let(GetTorrentDetailsResponse::Success)
            ?: GetTorrentDetailsResponse.DetailsNotFound
    }
}

private data class TorrentInfo(
    val torrentName: String,
    val size: String,
    val seeders: UInt,
    val peers: UInt,
    val category: Category,
    val uploadDate: String,
    val descriptionPageUrl: String,
)

private class BitSearchResultsPageParser(
    private val providerName: String,
) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent>? =
        withContext(Dispatchers.Default) {
            Jsoup
                .parse(html, pageUrl)
                .selectFirst("div.space-y-4")
                ?.children()
                ?.mapNotNull { parseInfoDiv(it) }
        }

    private fun parseInfoDiv(div: Element): Torrent? {
        val innerDivChildren = div
            .find { it.hasClass("flex items-start justify-between") }
            ?.children()
            ?: return null

        val torrentInfoDiv = innerDivChildren.getOrNull(0) ?: return null
        val torrentInfo = parseTorrentInfoDiv(div = torrentInfoDiv) ?: return null

        val downloadLinksDiv = innerDivChildren.getOrNull(1) ?: return null
        val magnetUri = parseMagnetUri(downloadLinksDiv = downloadLinksDiv) ?: return null

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentInfo.torrentName,
            size = torrentInfo.size,
            seeders = torrentInfo.seeders,
            peers = torrentInfo.peers,
            providerName = providerName,
            uploadDate = torrentInfo.uploadDate,
            category = torrentInfo.category,
            descriptionPageUrl = torrentInfo.descriptionPageUrl,
            magnetUri = magnetUri,
        )
    }


    private fun parseTorrentInfoDiv(div: Element): TorrentInfo? {
        // First child contains torrent name and description page URL which
        // are found inside the same <a> tag.
        val torrentNameAnchor = div
            .selectFirst("div:nth-child(1) > h3 > a")
            ?: return null
        val torrentName = torrentNameAnchor.ownText()
        val descriptionPageUrl = torrentNameAnchor.attr("abs:href")

        // Second child contains category, size and upload date respectively
        // wrapped inside a <span> tag which in-turn contains two tags i.e.
        // <i> for icon and another <span> for actual text.
        val categoryAndStatsDiv = div.selectFirst("div:nth-child(2)") ?: return null
        val category = categoryAndStatsDiv
            .selectFirst("> span:nth-child(1) > span")
            ?.ownText()
            ?.let(::categoryFromRawString)
            ?: return null
        val size = categoryAndStatsDiv
            .selectFirst("> span:nth-child(2) > span")
            ?.ownText()
            ?: return null
        val uploadDate = categoryAndStatsDiv
            .selectFirst("> span:nth-child(3) > span")
            ?.ownText()
            ?.let { DateUtils.formatMonthDayYear(it) }
            ?: return null

        // Third child contains seeders, leechers and download count (no use for us)
        // respectively wrapped inside a <span> tag which in-turn contains three tags
        // i.e. <i> for icon, <span> for actual data text (this is what we need) and
        // lastly another <span> for labeling like 'seeders'.
        val seedersAndPeersDiv = div.selectFirst("div:nth-child(3)") ?: return null
        val seeders = seedersAndPeersDiv
            .selectFirst("span:nth-child(1) > span")
            ?.ownText()
            ?.toUIntOrNull()
            ?: return null
        val peers = seedersAndPeersDiv
            .selectFirst("span:nth-child(2) > span")
            ?.ownText()
            ?.toUIntOrNull()
            ?: return null

        return TorrentInfo(
            torrentName = torrentName,
            size = size,
            seeders = seeders,
            peers = peers,
            category = category,
            uploadDate = uploadDate,
            descriptionPageUrl = descriptionPageUrl,
        )
    }

    private fun parseMagnetUri(downloadLinksDiv: Element): MagnetUri? {
        return downloadLinksDiv
            .select("a[href]")
            .last()
            ?.attr("href")
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
            val lastChecked = html.selectFirst(LAST_CHECKED)?.ownText()
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