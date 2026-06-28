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

class LinuxTracker : SearchProvider, LatestTorrentsProvider, TopTorrentsProvider,
    TorrentDetailsProvider {
    override val id = "linuxtracker"
    override val name = "LinuxTracker"
    override val url = "https://linuxtracker.org"
    override val supportedCategories = setOf(Category.Apps)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsPageParser = LinuxTrackerResultsPageParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = "$url/index.php?page=torrents&search=$query&category=0&active=0"
        val responseHtml = context.httpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return LinuxTrackerDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/index.php?page=torrents&search=&category=0&active=0"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        return getLastestTorrents(category)
    }
}

private class LinuxTrackerResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull(::parseListItem)
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val torrentName = listItem.selectFirst(TORRENT_NAME)?.ownText() ?: return null
        val magnetUri = listItem.selectFirst(MAGNET_URI)?.attr("href") ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)
            ?.ownText()
            ?.trim()
            ?.let { TorrentDateParser.parse(date = it, format = "dd/MM/yyyy") }
        val fileDownloadLink = listItem.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size,
            seeders = seeders,
            peers = peers,
            uploadDate = uploadDate,
            category = Category.Apps,
            providerName = providerName,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
            descriptionPageUrl = detailsPageUrl,
        )
    }

    private companion object {
        private const val LIST_ITEM =
            """table.lista[width="100%"] > tbody > tr:has(a[href^="index.php?page=torrent-details&id="][title])"""
        private const val TORRENT_NAME = """a[href^="index.php?page=torrent-details&id="][title]"""
        private const val SIZE = "td:nth-child(2) > table > tbody > tr:nth-child(2) > td"
        private const val SEEDERS = "td:nth-child(2) > table > tbody > tr:nth-child(3) > td"
        private const val PEERS = "td:nth-child(2) > table > tbody > tr:nth-child(4) > td"
        private const val UPLOAD_DATE = "td:nth-child(2) > table > tbody > tr > td"
        private const val MAGNET_URI = """a[href^="magnet:?"]"""
        private const val FILE_DOWNLOAD_LINK = """a[href^="index.php?page=downloadcheck&id="]"""
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}

private object LinuxTrackerDetailsPageParser {
    private const val TORRENT_NAME = """span[itemprop="name"]"""
    private const val SIZE = "td:containsOwn(Size)"
    private const val PEERS_STATS = "td:containsOwn(peers)"
    private const val UPLOAD_DATE = "td:containsOwn(AddDate)"
    private const val UPLOADER = "td:containsOwn(Uploader)"
    private const val DESCRIPTION = """span[itemprop="blogPost"]"""
    private const val MAGNET_URI = """a[href^="magnet:?"]"""

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)
            val torrentName = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val size = html.selectFirst(SIZE)?.nextElementSibling()?.ownText()
            val uploadDate = html.selectFirst(UPLOAD_DATE)
                ?.nextElementSibling()
                ?.ownText()
                ?.let { TorrentDateParser.parse(date = it, format = "dd/MM/yyyy") }
            val uploader = html.selectFirst(UPLOADER)?.nextElementSibling()?.text()
            val description = html.selectFirst(DESCRIPTION)?.html()
            val peersStats = html.selectFirst(PEERS_STATS)?.nextElementSibling()?.ownText()
            val seeders =
                peersStats?.removePrefix("seeds: ")?.takeWhile { it != ',' }?.toUIntOrNull()
            val peers = peersStats
                ?.dropWhile { it != ',' }
                ?.drop(2)
                ?.removePrefix("leechers: ")
                ?.takeWhile { !it.isWhitespace() }
                ?.toUIntOrNull()

            TorrentDetails(
                infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
                name = torrentName,
                magnetUri = magnetUri,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = Category.Apps,
                uploader = uploader,
                description = description,
            )
        }
}