package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class Nyaa : SearchProvider, TorrentDetailsProvider, LatestTorrentsProvider, TopTorrentsProvider {
    override val id = "nyaasi"
    override val name = "Nyaa"
    override val url = "https://nyaa.si"
    override val supportedCategories = setOf(
        Category.Anime,
        Category.Apps,
        Category.Books,
        Category.Games,
        Category.Music,
        Category.Series,
    )
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = true

    private val resultsPageParser = NyaaResultsPageParser(providerName = name)
    private val categoryMap = mapOf(
        Category.All to "0_0",
        Category.Anime to "1_0",
        Category.Apps to "6_1",
        Category.Books to "3_0",
        Category.Games to "6_2",
        Category.Music to "2_0",
        Category.Series to "4_0",
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append("$url/")
            // Filter = No filter (0)
            append("?f=0")
            val categoryId = categoryMap[context.category] ?: categoryMap[Category.All]!!
            append("&c=$categoryId")
            append("&q=$query")
        }

        val responseHtml = context.httpClient.get(url = requestUrl)
        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return NyaaDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val categoryId = categoryMap[category] ?: categoryMap[Category.All]!!
        val requestUrl = "$url?c=$categoryId"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        val categoryId = categoryMap[category] ?: categoryMap[Category.All]!!
        val requestUrl = "$url?s=seeders&o=desc&c=$categoryId"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }
}

private class NyaaResultsPageParser(private val providerName: String) {
    /**
     * Parses the result page and returns all the extracted torrents, otherwise
     * returns `null` if the page has unexpected layout.
     */
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull(::parseListItem)
        }

    /**
     * Parses the row and returns the fully constructed [Torrent], if extraction
     * completes successfully, otherwise `null` if the row has unexpected layout.
     */
    private fun parseListItem(listItem: Element): Torrent? {
        val torrentName = listItem.selectFirst(TORRENT_NAME)?.ownText() ?: return null
        val magnetUri = listItem.selectFirst(MAGNET_URI)?.attr("href") ?: return null
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")
        val fileDownloadLink = listItem.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
        val size = listItem.selectFirst(SIZE)?.ownText()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)
            ?.attr("data-timestamp")
            ?.toLongOrNull()
            ?.let(TorrentDateParser::epochSecondToInstant)
        val category = listItem.selectFirst(CATEGORY)
            ?.attr("href")
            ?.removePrefix("/?c=")
            ?.let(::categoryFromId)
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()

        return Torrent(
            infoHash = infoHash,
            name = torrentName,
            size = size ?: "0 KB",
            seeders = seeders ?: 0u,
            peers = peers ?: 0u,
            providerName = providerName,
            uploadDate = uploadDate,
            category = category,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
            descriptionPageUrl = detailsPageUrl ?: "",
        )
    }

    private companion object {
        private const val LIST_ITEM = "table.torrent-list > tbody > tr"
        private const val TORRENT_NAME = "td:nth-child(2) > a:not(.comments)"
        private const val SIZE = "td:nth-child(4)"
        private const val SEEDERS = "td:nth-child(6)"
        private const val PEERS = "td:nth-child(7)"
        private const val UPLOAD_DATE = "td:nth-child(5)"
        private const val CATEGORY = "td:nth-child(1) > a"
        private const val MAGNET_URI = "td:nth-child(3) > a:nth-child(2)"
        private const val FILE_DOWNLOAD_LINK = "td:nth-child(3) > a:nth-child(1)"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}

private object NyaaDetailsPageParser {
    private const val TORRENT_INFO_CARD = ".container > div:nth-child(1)"
    private const val TORRENT_INFO_CARD_BODY = "$TORRENT_INFO_CARD > div.panel-body"
    private const val TORRENT_NAME = "$TORRENT_INFO_CARD > div.panel-heading > h3"
    private const val SIZE = "$TORRENT_INFO_CARD_BODY > div:nth-child(4) > div:nth-child(2)"
    private const val SEEDERS = "$TORRENT_INFO_CARD_BODY > div:nth-child(2) > div:nth-child(4)"
    private const val PEERS = "$TORRENT_INFO_CARD_BODY > div:nth-child(3) > div:nth-child(4)"
    private const val UPLOAD_DATE = "$TORRENT_INFO_CARD_BODY > div:nth-child(1) > div:nth-child(4)"
    private const val CATEGORY = "$TORRENT_INFO_CARD_BODY > div:nth-child(1) > div:nth-child(2)"
    private const val UPLOADER = "$TORRENT_INFO_CARD_BODY > div:nth-child(2) > div:nth-child(2)"
    private const val DESCRIPTION = "#torrent-description"
    private const val MAGNET_URI = """a[href^="magnet:"]"""
    private const val FILE_DOWNLOAD_LINK = """a[href^="/download"]"""

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val name = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)

            val size = html.selectFirst(SIZE)?.ownText()
            val seeders = html.selectFirst(SEEDERS)?.text()?.trim()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.text()?.trim()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)
                ?.attr("data-timestamp")
                ?.toLongOrNull()
                ?.let(TorrentDateParser::epochSecondToInstant)
            val category = html.selectFirst(CATEGORY)
                ?.let {
                    val categoryId = it.selectFirst("a:nth-child(1)")
                        ?.attr("href")
                        ?.removePrefix("/?c=")
                    val subCategoryId = it.selectFirst("a:nth-child(2)")
                        ?.attr("href")
                        ?.removePrefix("/?c=")

                    if (subCategoryId == "6_2") {
                        Category.Games
                    } else {
                        categoryId?.let(::categoryFromId)
                    }
                }
            val uploader = html.selectFirst(UPLOADER)?.text()?.trim()
            val description = html.selectFirst(DESCRIPTION)?.html()
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")

            TorrentDetails(
                infoHash = infoHash,
                name = name,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = category,
                uploader = uploader,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
                description = description,
            )
        }
}

private fun categoryFromId(id: String): Category = when (id) {
    "1_0", "1_1", "1_2", "1_3", "1_4" -> Category.Anime
    "2_0", "2_1", "2_2" -> Category.Music
    "3_0", "3_1", "3_2", "3_3" -> Category.Books
    "4_0", "4_1", "4_2", "4_3", "4_4" -> Category.Series
    "5_0", "5_1", "5_2" -> Category.Other
    "6_0", "6_1" -> Category.Apps
    "6_2" -> Category.Games
    else -> Category.Other
}