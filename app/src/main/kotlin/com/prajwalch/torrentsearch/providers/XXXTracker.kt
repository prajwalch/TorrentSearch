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

class XXXTracker :
    SearchProvider,
    TorrentDetailsProvider,
    LatestTorrentsProvider,
    TopTorrentsProvider {
    override val id = "xxxtracker"
    override val name = "XXXTracker"
    override val url = "https://xxxtor.com"
    override val supportedCategories = setOf(Category.Porn)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsPageParser = XXXTrackerResultsPageParser(providerName = name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/b.php")
            append("?search=$query")
        }
        val responseHtml = context.httpClient.get(url = requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return XXXTrackerDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/b.php"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/top"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    // We need to access this during bookmarks migration.
    companion object {
        // Russian month abbreviations from the provider are incompatible with
        // Java's locale data, so we normalize them to English before parsing.
        private val monthMap = mapOf(
            "Янв" to "Jan",
            "Фев" to "Feb",
            "Мар" to "Mar",
            "Апр" to "Apr",
            "Май" to "May",
            "Июн" to "Jun",
            "Июл" to "Jul",
            "Авг" to "Aug",
            "Сен" to "Sep",
            "Окт" to "Oct",
            "Ноя" to "Nov",
            "Дек" to "Dec",
        )

        fun normalizeUploadDate(uploadDate: String): String {
            val (day, russianMonth, year) = uploadDate.split(' ', limit = 3)
            val englishMonth = monthMap[russianMonth]!!

            return "$day $englishMonth $year"
        }
    }
}

private class XXXTrackerResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                // First item is used as a header.
                .drop(1)
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
            ?.let(XXXTracker::normalizeUploadDate)
            ?.let { TorrentDateParser.parse(date = it, format = "dd MMM yy") }
        val fileDownloadLink = listItem.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size ?: "0 KB",
            seeders = seeders ?: 0U,
            peers = peers ?: 0U,
            uploadDate = uploadDate,
            category = Category.Porn,
            descriptionPageUrl = detailsPageUrl ?: "",
            providerName = providerName,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
    }

    private companion object {
        private const val LIST_ITEM = "table > tbody > tr"
        private const val TORRENT_NAME = "td:nth-child(2) > a:nth-child(3)"
        private const val SIZE = "td:nth-child(3)"
        private const val SEEDERS = "td:nth-child(4) > span:nth-child(1)"
        private const val PEERS = "td:nth-child(4) > span:nth-child(2)"
        private const val UPLOAD_DATE = "td:nth-child(1)"
        private const val MAGNET_URI = "td:nth-child(2) > a:nth-child(1)"
        private const val FILE_DOWNLOAD_LINK = "td:nth-child(2) > a:nth-child(2)"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}

private object XXXTrackerDetailsPageParser {
    private const val NAME = "#content > h1"
    private const val SIZE = "#details > tbody > tr:nth-last-child(2) > td:nth-child(2)"
    private const val SEEDERS = "#details > tbody > tr:nth-last-child(7) > td:nth-child(2)"
    private const val PEERS = "#details > tbody > tr:nth-last-child(6) > td:nth-child(2)"
    private const val UPLOAD_DATE = "#details > tbody > tr:nth-last-child(3) > td:nth-child(2)"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""
    private const val FILE_DOWNLOAD_LINK = "#download > a:nth-child(1)"
    private const val POSTER_URL = "#details > tbody > tr:nth-child(1) > td:nth-child(2) > img"
    private const val DESCRIPTION = "#details > tbody > tr:nth-child(1) > td:nth-child(2)"

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val name = html.selectFirst(NAME)?.text() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
            val size = html.selectFirst(SIZE)?.text()?.takeWhile { it != '(' }?.trim()
            val seeders = html.selectFirst(SEEDERS)?.text()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.text()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)
                ?.ownText()
                ?.takeWhile { !it.isWhitespace() }
                ?.trim()
                ?.let { TorrentDateParser.parse(date = it, format = "dd-MM-yyyy") }
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
            val posterUrl = html.selectFirst(POSTER_URL)?.attr("src")
            val description = html.selectFirst(DESCRIPTION)
                // Remove poster and two new lines after the poster from description.
                ?.apply { select("> *:lt(3)").remove() }
                ?.html()
                ?.let(TorrentUtils.HtmlToMarkdownConverter::convert)

            TorrentDetails(
                infoHash = infoHash,
                name = name,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = Category.Porn,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
                description = description,
                posterUrl = posterUrl,
            )
        }
}