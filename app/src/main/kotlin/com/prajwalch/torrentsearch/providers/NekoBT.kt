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

// TODO: Use JSON API
//       https://nekobt.to/search?query=one
//       https://nekobt.to/api/v1/torrents/search?sort_by=seeders (top)
//       https://nekobt.to/api/v1/torrents/search?sort_by=rss (latest)
class NekoBT : SearchProvider, LatestTorrentsProvider, TopTorrentsProvider, TorrentDetailsProvider {
    override val id = "nekobt"
    override val name = "NekoBT"
    override val url = "https://nekobt.to"
    override val supportedCategories = setOf(Category.Anime)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsPageParser = NekoBTResultsPageParser(providerName = name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = "$url/search?query=$query"
        val responseHtml = context.httpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/search?sort-by=latest"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/search?sort-by=seeders"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return NekoBTDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }
}

private class NekoBTResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull(::parseListItem)
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val torrentName = listItem.selectFirst(TORRENT_NAME)
            ?.selectFirst("span > span:nth-child(1)")
            ?.ownText()
            ?: return null
        val magnetUri = listItem.selectFirst(MAGNET_URI)?.attr("href") ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)
            ?.ownText()
            ?.let { TorrentDateParser.parse(date = it, format = "yyyy-MM-dd HH:mm:ss") }
        val fileDownloadLink = listItem.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size,
            seeders = seeders,
            peers = peers,
            providerName = providerName,
            uploadDate = uploadDate,
            category = Category.Anime,
            descriptionPageUrl = detailsPageUrl,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
    }

    private companion object {
        private const val LIST_ITEM = "table.table > tbody > tr"
        private const val TORRENT_NAME = "td:nth-child(3) > div:nth-child(1) > div > a"
        private const val SIZE = "td:nth-child(5) > span"
        private const val SEEDERS = "td:nth-child(7) > span"
        private const val PEERS = "td:nth-child(8) > span"
        private const val UPLOAD_DATE = "td:nth-child(6) > span"
        private const val MAGNET_URI = "td:nth-child(4) > div > a:nth-child(1)"
        private const val FILE_DOWNLOAD_LINK = "td:nth-child(4) > div > a:nth-child(2)"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}

private object NekoBTDetailsPageParser {
    private const val INFO_CARD =
        "div.grid > div:nth-child(1) > div.card:nth-child(1) > div.card-body"
    private const val TORRENT_NAME = "$INFO_CARD > h2.card-title > div > span > span:nth-child(1)"
    private const val SIZE = """$INFO_CARD > div:nth-child(2) > span[data-tip="Total Size"]"""
    private const val SEEDERS = """$INFO_CARD > div:nth-child(2) > span[data-tip="Seeders"]"""
    private const val PEERS = """$INFO_CARD > div:nth-child(2) > span[data-tip="Leechers"]"""
    private const val UPLOAD_DATE = "$INFO_CARD > div:nth-child(2) span:nth-child(8)"
    private const val UPLOADER = """$INFO_CARD > div:nth-child(2) > span[data-tip="Uploader"] > a"""
    private const val MAGNET_URI = """$INFO_CARD a[href^="magnet:?"]"""
    private const val FILE_DOWNLOAD_LINK = """$INFO_CARD a[href^="/api/v1/torrents"]"""
    private const val DESCRIPTION =
        "div.grid > div:nth-child(1) > div.card:nth-last-child(3) div.markdown"
    private const val POSTER_URL = """img[alt^="Banner for "]"""
    // TODO: Extract
    // private const val SCREENSHOT_URLS = ""

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
                ?.let { TorrentDateParser.parse(date = it, format = "yyyy-MM-dd HH:mm:ss") }
            val uploader = html.selectFirst(UPLOADER)?.ownText()
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
            val description = html.selectFirst(DESCRIPTION)?.html()
            val posterUrl = html.selectFirst(POSTER_URL)?.attr("abs:src")

            TorrentDetails(
                infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
                name = torrentName,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = Category.Anime,
                uploader = uploader,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
                description = description,
                posterUrl = posterUrl,
            )
        }
}