package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.TorrentDateParser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Provider implementation for [LimeTorrents](https://www.limetorrents.lol).
 *
 * Extracts torrent results from the HTML search page.
 * This provider uses InfoHash, not Magnet URIs.
 */
class LimeTorrents : SearchProvider, TorrentDetailsProvider, LatestTorrentsProvider,
    TopTorrentsProvider {
    override val id = "limetorrents"
    override val name = "LimeTorrents"
    override val url = "https://limetorrents.fun"
    override val supportedCategories = setOf(
        Category.Anime,
        Category.Apps,
        Category.Games,
        Category.Movies,
        Category.Music,
        Category.Series,
        Category.Other,
    )
    override val safetyStatus = SearchProviderSafetyStatus.Unsafe(
        reason = R.string.limetorrents_unsafe_reason,
    )
    override val enabledByDefault = false

    private val resultsPageParser = LimeTorrentsResultsPageParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val categoryString = getCategorySearchString(context.category)
        val requestUrl = "$url/search/$categoryString/$query/date/1/"

        val responseHtml = context.httpClient.get(url = requestUrl)
        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return LimeTorrentsDetailsPageParser.parse(responseHtml)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        if (category !in supportedCategories) return emptyList()

        val categoryString = getCategoryBrowseString(category)
        val requestUrl = "$url/browse-torrents/$categoryString/"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(
            html = responseHtml,
            pageUrl = requestUrl,
            searchCategory = category,
        )
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        if (category !in supportedCategories) return emptyList()

        val categoryString = getCategoryBrowseString(category)
        val requestUrl = "$url/cat_top/16/$categoryString/"
        val responseHtml = HttpClient.get(requestUrl)

        return resultsPageParser.parse(
            html = responseHtml,
            pageUrl = requestUrl,
            searchCategory = category,
        )
    }

    /** Returns the category string used by it. */
    private fun getCategorySearchString(category: Category): String = when (category) {
        Category.All, Category.Books, Category.Porn -> "all"
        Category.Anime -> "anime"
        Category.Apps -> "applications"
        Category.Games -> "games"
        Category.Movies -> "movies"
        Category.Music -> "music"
        Category.Series -> "tv"
        Category.Other -> "other"
    }

    private fun getCategoryBrowseString(category: Category): String = when (category) {
        Category.All -> throw IllegalStateException()
        Category.Anime -> "Anime"
        Category.Apps -> "Applications"
        Category.Books -> "Other-E-books"
        Category.Games -> "Games"
        Category.Movies -> "Movies"
        Category.Music -> "Music"
        Category.Porn -> throw IllegalStateException()
        Category.Series -> "TV-shows"
        Category.Other -> "Other"
    }
}

private class LimeTorrentsResultsPageParser(private val providerName: String) {
    suspend fun parse(
        html: String,
        pageUrl: String,
        searchCategory: Category? = null,
    ): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull { parseListItem(it, searchCategory) }
        }

    private fun parseListItem(listItem: Element, searchCategory: Category? = null): Torrent? {
        val torrentName = listItem.selectFirst(TORRENT_NAME)?.ownText() ?: return null
        val fileDownloadLink = listItem.selectFirst(FILE_DOWNLOAD_LINK)
            ?.attr("href")
            ?: return null
        // http://itorrents.net/torrent/7B6CAE55DB21441F06EA00A25C2A21E11752EEE2.torrent?title=...
        val infoHash = fileDownloadLink.removePrefix("http://itorrents.net/torrent/")
            .takeWhile { it != '.' }
            .lowercase()
        val size = listItem.selectFirst(SIZE)?.ownText()
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val (rawUploadDate, rawCategory) = listItem.selectFirst(UPLOAD_DATE_AND_CATEGORY)
            ?.ownText()
            ?.let(::parseDateAndCategory)
            ?: Pair(null, null)
        val uploadDate = rawUploadDate?.let(TorrentDateParser::tryParseRelative)
        val category = rawCategory?.let(::categoryFromRawString) ?: searchCategory
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")

        return Torrent(
            infoHash = infoHash,
            name = torrentName,
            size = size,
            seeders = seeders,
            peers = peers,
            providerName = providerName,
            uploadDate = uploadDate,
            category = category,
            descriptionPageUrl = detailsPageUrl,
            fileDownloadLink = fileDownloadLink,
        )
    }

    private fun parseDateAndCategory(text: String): Pair<String?, String?> {
        // Pair<Date, Category>
        if (!text.contains("- in")) return Pair(text.trim(), null)

        val (rawUploadDate, rawCategory) = text.split("-", limit = 2).map { it.trim() }
        return Pair(rawUploadDate, rawCategory.removePrefix("in ").removeSuffix("."))
    }

    private companion object {
        private const val LIST_ITEM = ".table2 > tbody > tr"
        private const val TORRENT_NAME = "td:nth-child(1) > div.tt-name > a:nth-child(2)"
        private const val SIZE = "td:nth-child(3)"
        private const val SEEDERS = "td.tdseed"
        private const val PEERS = "td.tdleech"
        private const val UPLOAD_DATE_AND_CATEGORY = "td:nth-child(2)"
        private const val FILE_DOWNLOAD_LINK = "td:nth-child(1) > div.tt-name > a:nth-child(1)"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}

private object LimeTorrentsDetailsPageParser {
    private const val INFO_HASH =
        "#content > div:nth-child(6) > div:nth-child(1) > div > table > tbody > tr:nth-child(1) > td:nth-child(2)"
    private const val NAME = "#content > h1"
    private const val SIZE =
        "#content > div:nth-child(6) > div:nth-child(1) > div > table > tbody > tr:nth-child(3) > td:nth-child(2)"
    private const val SEEDERS = "#content > span.greenish"
    private const val PEERS = "#content > span.reddish"
    private const val UPLOAD_DATE_AND_CATEGORY =
        "#content > div:nth-child(6) > div:nth-child(1) > div > table > tbody > tr:nth-child(2) > td:nth-child(2)"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""
    private const val FILE_DOWNLOAD_LINK =
        "#content > div:nth-child(6) > div:nth-child(1) > div > div:nth-child(7) > div > a"

    suspend fun parse(html: String): TorrentDetails? = withContext(Dispatchers.Default) {
        val html = Jsoup.parse(html)

        val infoHash = html.selectFirst(INFO_HASH)?.ownText()?.lowercase()
            ?: return@withContext null
        val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
        val name = html.selectFirst(NAME)?.ownText() ?: return@withContext null
        val size = html.selectFirst(SIZE)?.ownText()
        val seeders = html.selectFirst(SEEDERS)
            ?.ownText()
            ?.removePrefix("Seeders : ")
            ?.trim()
            ?.toUIntOrNull()
        val peers = html.selectFirst(PEERS)
            ?.ownText()
            ?.removePrefix("Leechers : ")
            ?.trim()
            ?.toUIntOrNull()
        val (rawUploadDate, rawCategory) = html.selectFirst(UPLOAD_DATE_AND_CATEGORY)
            ?.text()
            ?.split("in", limit = 2)
            ?: listOf(null, null)
        val uploadDate = rawUploadDate?.trim()?.let(TorrentDateParser::tryParseRelative)
        val category = rawCategory?.removeSuffix(".")?.let(::categoryFromRawString)
        val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("href")

        TorrentDetails(
            infoHash = infoHash,
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            uploadDate = uploadDate,
            category = category,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
    }
}

private fun categoryFromRawString(raw: String): Category = when (raw) {
    "TV" -> Category.Series
    "Movie" -> Category.Movies
    "Music" -> Category.Music
    "App" -> Category.Apps
    "E-book" -> Category.Books
    "Anime" -> Category.Anime
    "Games" -> Category.Games
    else -> Category.Other
}