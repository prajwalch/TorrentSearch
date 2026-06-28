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

import java.time.Instant

class ThirteenThirtySevenX : SearchProvider, TopTorrentsProvider, TorrentDetailsProvider {
    override val id = "1337x"
    override val name = "1337x"
    override val url = "https://1337x.to"
    override val supportedCategories = setOf(
        Category.Anime,
        Category.Apps,
        Category.Games,
        Category.Movies,
        Category.Music,
        Category.Other,
        Category.Porn,
        Category.Series,
    )
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val isCloudflareProtected = true
    override val enabledByDefault = false

    private val categoryMap = mapOf(
        Category.Anime to "Anime",
        Category.Apps to "Apps",
        Category.Games to "Games",
        Category.Movies to "Movies",
        Category.Music to "Music",
        Category.Other to "Other",
        Category.Porn to "XXX",
        Category.Series to "TV",
    )
    private val resultsPageParser = ThirteenThirtySevenXResultsPageParser(providerName = name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = if (context.category == Category.All) {
            "$url/search/$query/1/"
        } else {
            val categoryString = categoryMap[context.category]!!
            "$url/category-search/$query/$categoryString/1/"
        }
        val responseHtml = context.httpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/top-100"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val response = HttpClient.get(detailsPageUrl)
        return ThirteenThirtySevenXDetailsPageParser.parse(
            html = response,
            pageUrl = detailsPageUrl,
        )
    }
}

private class ThirteenThirtySevenXResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default.limitedParallelism(3)) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .map { async { parseListItem(it) } }
                .awaitAll()
                .filterNotNull()
        }

    private suspend fun parseListItem(listItem: Element): Torrent? {
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href") ?: return null
        val detailsPageHtml = HttpClient.get(detailsPageUrl)
        val torrentDetails = ThirteenThirtySevenXDetailsPageParser.parse(
            html = detailsPageHtml,
            pageUrl = detailsPageUrl,
        ) ?: return null

        val torrentName = listItem.selectFirst(TORRENT_NAME)?.ownText() ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()?.filter { it != ',' }
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)?.ownText()?.let(::parseDate)
        val category = listItem.selectFirst(CATEGORY)?.attr("href")
            ?.removePrefix("/sub/")
            ?.takeWhile { it != '/' }
            ?.let(::getCategoryFromId)

        return Torrent(
            infoHash = torrentDetails.infoHash,
            name = torrentName,
            size = size,
            seeders = seeders ?: 0U,
            peers = peers ?: 0U,
            uploadDate = uploadDate,
            providerName = providerName,
            category = category,
            descriptionPageUrl = detailsPageUrl,
            magnetUri = torrentDetails.magnetUri,
            fileDownloadLink = torrentDetails.fileDownloadLink
        )
    }

    private fun parseDate(date: String): Instant? {
        // Possible formats:
        //  12:45am
        //  8pm Jun. 2nd
        //  May. 27th '26
        val normalizedDate = date
            // 27th, 3rd -> 27, 3
            .replace(Regex("(\\d+)(st|nd|rd|th)"), "$1")
            // 8pm, 12:25am -> 8 pm, 12:45 am
            .replace(Regex("(?<=\\d)(am|pm)\\b"), " $1")
            // Jun. -> Jun
            .replace(".", "")
            // '26 -> 26
            .replace("'", "")

        return runCatching {
            // May 31 26 (normalized)
            TorrentDateParser.parse(date = normalizedDate, format = "MMM d yy")
        }.recoverCatching {
            // 8 pm Jun 2 (normalized)
            val currentYear = TorrentDateParser.getCurrentYear()
            // 8 pm Jun 2 2020
            val reconstructedDate = "$normalizedDate $currentYear"

            TorrentDateParser.parse(date = reconstructedDate, format = "h a MMM d yyyy")
        }.recoverCatching {
            //  12:45 am (normalized)
            TorrentDateParser.tryParseTime(normalizedDate)
        }.getOrNull()
    }

    private companion object {
        private const val LIST_ITEM = "table.table-list > tbody > tr"
        private const val TORRENT_NAME = "td.name > a:nth-child(2)"
        private const val SIZE = "td.size"
        private const val SEEDERS = "td.seeds"
        private const val PEERS = "td.leeches"
        private const val UPLOAD_DATE = "td.coll-date"
        private const val CATEGORY = "td.name > a:nth-child(1)"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}

private object ThirteenThirtySevenXDetailsPageParser {
    private const val CONTAINER = "div.box-info.torrent-detail-page"
    private const val INFO_CONTAINER = "$CONTAINER > div:nth-child(2) > div:nth-child(1)"
    private const val INFO_COL_FIRST = "$INFO_CONTAINER > ul:nth-child(2)"
    private const val INFO_COL_SECOND = "$INFO_CONTAINER > ul:nth-child(3)"
    private const val TORRENT_NAME = "$CONTAINER > div.box-info-heading > h1"
    private const val CATEGORY = "$INFO_COL_FIRST > li:nth-child(1) > span"
    private const val SIZE = "$INFO_COL_FIRST > li:nth-child(4) > span"
    private const val UPLOADER = "$INFO_COL_FIRST > li:nth-child(5) > span"
    private const val LAST_CHECKED = "$INFO_COL_SECOND > li:nth-child(2) > span"
    private const val UPLOAD_DATE = "$INFO_COL_SECOND > li:nth-child(3) > span"
    private const val SEEDERS = "$INFO_COL_SECOND > li:nth-child(4) > span"
    private const val PEERS = "$INFO_COL_SECOND > li:nth-child(5) > span"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""
    private const val FILE_DOWNLOAD_LINK = """a[href^="https://itorrents.org"]"""
    private const val DESCRIPTION = "div#description"
    private const val POSTER_URL = "div.torrent-image > img"

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val torrentName = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val size = html.selectFirst(SIZE)?.ownText()?.filter { it != ',' }
            val seeders = html.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)
                ?.ownText()
                ?.let(TorrentDateParser::tryParseRelative)
            val category = html.selectFirst(CATEGORY)?.ownText()?.let(::getCategoryFromString)
            val uploader = html.selectFirst(UPLOADER)?.text()
            val lastChecked = html.selectFirst(LAST_CHECKED)
                ?.ownText()
                ?.let(TorrentDateParser::tryParseRelative)
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
                category = category,
                uploader = uploader,
                lastChecked = lastChecked,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
                description = description,
                posterUrl = posterUrl,
            )
        }
}

// Thank you Jackett ;)
// https://github.com/Jackett/Jackett/blob/master/src/Jackett.Common/Definitions/1337x.yml#L32
private fun getCategoryFromId(id: String): Category = when (id) {
    "28", "78", "79", "80", "81" -> Category.Anime
    "22", "23", "24", "25", "26", "27", "53", "58", "59", "60", "68", "69" -> Category.Music
    "1", "2", "3", "4", "42", "54", "55", "66", "70", "73", "76" -> Category.Movies
    "5", "6", "7", "41", "71", "74", "75", "9" -> Category.Series
    "18", "19", "20", "21", "56", "57" -> Category.Apps
    "10", "11", "12", "13", "14", "15", "16", "17", "43", "44", "45", "46", "72", "77", "82",
        -> Category.Games

    "48", "49", "50", "51", "67" -> Category.Porn
    "33", "34", "35", "36", "37", "38", "39", "40", "47", "52" -> Category.Other
    else -> Category.Other
}

private fun getCategoryFromString(raw: String): Category = when (raw) {
    "Anime" -> Category.Anime
    "Apps" -> Category.Apps
    "Games" -> Category.Games
    "Movies" -> Category.Movies
    "Music" -> Category.Music
    "Other" -> Category.Other
    "XXX" -> Category.Porn
    "TV", "Documentaries" -> Category.Series
    else -> Category.Other
}