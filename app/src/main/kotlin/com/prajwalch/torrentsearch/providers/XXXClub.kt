package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class XXXClub :
    SearchProvider,
    TorrentDetailsProvider,
    LatestTorrentsProvider,
    TopTorrentsProvider {
    override val id = "xxxclub"
    override val name = "XXXClub"
    override val url = "https://xxxclub.to"
    override val supportedCategories = setOf(Category.Porn)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsPageParser = XXXClubResultsPageParser(providerName = name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = "${url}/torrents/search/all/$query"
        val responseHtml = context.httpClient.get(url = requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return XXXClubDetailsPageParser.parse(responseHtml, detailsPageUrl)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/torrents/browse/all"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/torrents/top100"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }
}

private class XXXClubResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .selectFirst(LIST_ITEM_CONTAINER)
                ?.select(LIST_ITEM)
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
        val torrentDetails = XXXClubDetailsPageParser.parse(
            html = detailsPageHtml,
            pageUrl = detailsPageUrl,
        ) ?: return null

        val name = listItem.selectFirst(NAME)?.text() ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)
            ?.ownText()
            ?.let { TorrentDateParser.parse(date = it, format = "dd MMM yyyy HH:mm:ss") }

        return Torrent(
            infoHash = torrentDetails.infoHash,
            name = name,
            size = size ?: "0 KB",
            seeders = seeders ?: 0u,
            peers = peers ?: 0u,
            providerName = providerName,
            uploadDate = uploadDate,
            category = Category.Porn,
            descriptionPageUrl = detailsPageUrl,
            magnetUri = torrentDetails.magnetUri,
            fileDownloadLink = torrentDetails.fileDownloadLink,
        )
    }

    private companion object {
        private const val LIST_ITEM_CONTAINER = "div.browsetableinside, div.divtableinside"
        private const val LIST_ITEM = "ul > li"
        private const val NAME = """span:nth-child(2) > a[href^="/torrents/details"]"""
        private const val SIZE = "span.siz"
        private const val SEEDERS = "span.see"
        private const val PEERS = "span.lee"
        private const val UPLOAD_DATE = "span.adde"
        private const val DETAILS_PAGE_URL = NAME
    }
}

private object XXXClubDetailsPageParser {
    private const val NAME = "body > div > div.middle > div.main-content > div > h1"
    private const val SIZE = "div.detailsdescr > ul > li:nth-child(2) > span:nth-child(3)"
    private const val SEEDERS = "div.detailsdescr font.see"
    private const val PEERS = "div.detailsdescr font.lee"
    private const val UPLOAD_DATE = "div.detailsdescr > ul > li:nth-child(3) > span:nth-child(3)"

    //    private const val CATEGORY = "div.detailsdescr > ul > li:nth-child(1) > span:nth-child(3)"
    private const val UPLOADER = "div.detailsdescr > ul > li:nth-child(6) > span:nth-child(3)"
    private const val LAST_CHECKED = "div.detailsdescr > ul > li:nth-child(5) > span:nth-child(3)"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""
    private const val FILE_DOWNLOAD_LINK =
        "div.detailsdescr > ul > li.downloadboxlist > span:nth-child(1) > a"
    private const val DESCRIPTION = "div.description"
    private const val POSTER_URL = "img.detailsposter"
    private const val DATE_FORMAT = "dd MMM yyyy HH:mm:ss"

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)
            val htmlToMarkdownConverter = FlexmarkHtmlConverter.builder().build()

            val name = html.selectFirst(NAME)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
            val size = html.selectFirst(SIZE)?.ownText()
            val seeders = html.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)
                ?.ownText()
                ?.let { TorrentDateParser.parse(date = it, format = DATE_FORMAT) }
//            val category = html.selectFirst(CATEGORY)?.text()
            val uploader = html.selectFirst(UPLOADER)?.ownText()
            val lastChecked = html.selectFirst(LAST_CHECKED)
                ?.ownText()
                // Some torrents will have "Pending" status.
                ?.let { runCatching { TorrentDateParser.parse(date = it, format = DATE_FORMAT) } }
                ?.getOrNull()
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
            val description = html.selectFirst(DESCRIPTION)
                ?.html()
                ?.let(htmlToMarkdownConverter::convert)
            val posterUrl = html.selectFirst(POSTER_URL)?.attr("src")

            TorrentDetails(
                infoHash = infoHash,
                name = name,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = Category.Porn,
                uploader = uploader,
                lastChecked = lastChecked,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
                description = description,
                posterUrl = posterUrl,
            )
        }
}