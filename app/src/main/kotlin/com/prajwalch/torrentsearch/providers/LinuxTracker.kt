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

class LinuxTracker(private val networkClient: NetworkClient) :
    SearchProvider,
    LatestTorrentsProvider,
    TopTorrentsProvider,
    MagnetUriProvider,
    TorrentDetailsProvider {
    override val id = "linuxtracker"
    override val name = "LinuxTracker"
    override val url = "https://linuxtracker.org"
    override val supportedCategories = setOf(Category.Apps)
    override val safety = SearchProviderSafety.Safe
    override val enabledByDefault = false

    private val resultsPageParser = LinuxTrackerResultsPageParser(id, name)

    override suspend fun search(query: String, category: Category): List<Torrent> {
        val requestUrl = "$url/index.php?page=torrents&search=$query&category=0&active=0"
        val responseHtml = networkClient.getText(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = networkClient.getText(detailsPageUrl)
        return LinuxTrackerDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/torrents/"
        val responseHtml = networkClient.getText(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        return getLastestTorrents(category)
    }

    override suspend fun getMagnetUri(url: String): String {
        val detailsPageHtml = networkClient.getText(url)

        return LinuxTrackerDetailsPageParser.extractMagnetUri(detailsPageHtml)
            ?: error("Failed to retrieve magnet URI from '$url'")
    }
}

private class LinuxTrackerResultsPageParser(
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
        val torrentName = listItem.selectFirst(TORRENT_NAME)?.ownText() ?: return null
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)
            ?.attr("abs:href")
            ?.takeIf { it.isNotBlank() }
            ?: return null

        // https://linuxtracker.org/torrents/5b1e0d988fc7a0c9e99bd852071681a59974b39f/
        val torrentRemoteId = detailsPageUrl.removeSuffix("/").takeLastWhile { it != '/' }
        val torrentId = TorrentUtils.createTorrentId(
            providerId = providerId,
            sourceId = torrentRemoteId,
        )

        val size = listItem.selectFirst(SIZE)?.ownText()
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)
            ?.ownText()
            ?.trim()
            ?.let { TorrentDateParser.parse(date = it, format = "MMM d, yyyy") }

        return Torrent(
            id = torrentId,
            name = torrentName,
            size = size,
            seeders = seeders,
            peers = peers,
            uploadDate = uploadDate,
            category = Category.Apps,
            providerName = providerName,
            magnetUri = MagnetUri.RequiresFetch(detailsPageUrl),
            detailsPageUrl = detailsPageUrl,
        )
    }

    private companion object {
        private const val LIST_ITEM = "table.torrent-table > tbody > tr"
        private const val TORRENT_NAME = "td.torrent-name-cell > a.torrent-name"
        private const val SIZE = "td:nth-child(4)"
        private const val SEEDERS = "td.seeds > strong"
        private const val PEERS = "td.leeches > strong"
        private const val UPLOAD_DATE = "td:nth-child(3)"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}

private object LinuxTrackerDetailsPageParser {
    private const val TORRENT_NAME = "div.detail-title-block > h2"
    private const val SIZE = "dl.torrent-meta > div:nth-child(3) > dd"
    private const val SEEDERS = "div.detail-stats strong.seeds"
    private const val PEERS = "div.detail-stats strong.leeches"
    private const val UPLOAD_DATE = "dl.torrent-meta > div:nth-child(2) > dd"
    private const val DESCRIPTION = "div.torrent-description"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""
    private const val FILE_DOWNLOAD_LINK = "a.torrent-action.primary-action"
    private const val SCREENSHOT = "div.torrent-screenshot-grid img"

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val dom = Jsoup.parse(html, pageUrl)

            val torrentName = dom.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
            val magnetUri = dom.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val size = dom.selectFirst(SIZE)?.ownText()
            val seeders = dom.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
            val peers = dom.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
            val uploadDate = dom.selectFirst(UPLOAD_DATE)
                ?.ownText()
                ?.let { TorrentDateParser.parse(date = it, format = "MMM d, yyyy HH:mm") }
            val description = dom.selectFirst(DESCRIPTION)?.html()
            val fileDownloadLink = dom.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
            val screenshotUrls = dom.select(SCREENSHOT).map { it.attr("abs:src") }

            TorrentDetails(
                infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
                name = torrentName,
                magnetUri = magnetUri,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = Category.Apps,
                fileDownloadLink = fileDownloadLink,
                description = description,
                screenshotUrls = screenshotUrls,
            )
        }

    suspend fun extractMagnetUri(detailsPageHtml: String): String? =
        withContext(Dispatchers.Default) {
            Jsoup.parse(detailsPageHtml).selectFirst(MAGNET_URI)?.attr("href")
        }
}