package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.MagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.utils.DateUtils

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
        val torrents = withContext(Dispatchers.Default) {
            parseResponseHtml(html = responseHtml)
        }

        return torrents.orEmpty()
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

    private fun parseResponseHtml(html: String): List<Torrent>? {
        return Jsoup
            .parse(html)
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
            name = torrentInfo.torrentName,
            size = torrentInfo.size,
            seeders = torrentInfo.seeders,
            peers = torrentInfo.peers,
            providerId = info.id,
            providerName = info.name,
            uploadDate = torrentInfo.uploadDate,
            category = torrentInfo.category,
            descriptionPageUrl = torrentInfo.descriptionPageUrl,
            infoHashOrMagnetUri = InfoHashOrMagnetUri.MagnetUri(uri = magnetUri),
        )
    }

    private fun parseTorrentInfoDiv(div: Element): TorrentInfo? {
        // First child contains torrent name and description page URL which
        // are found inside the same <a> tag.
        val torrentNameAnchor = div
            .selectFirst("div:nth-child(1) > h3 > a")
            ?: return null
        val torrentName = torrentNameAnchor.ownText()
        val descriptionPageUrl = "${info.url}${torrentNameAnchor.attr("href")}"

        // Second child contains category, size and upload date respectively
        // wrapped inside a <span> tag which in-turn contains two tags i.e.
        // <i> for icon and another <span> for actual text.
        val categoryAndStatsDiv = div.selectFirst("div:nth-child(2)") ?: return null
        val category = categoryAndStatsDiv
            .selectFirst("> span:nth-child(1) > span")
            ?.ownText()
            ?.let { getCategoryFromId(it) }
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

    private fun getCategoryFromId(categoryId: String): Category = when (categoryId) {
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

    private fun parseMagnetUri(downloadLinksDiv: Element): MagnetUri? {
        return downloadLinksDiv
            .select("a[href]")
            .last()
            ?.attr("href")
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