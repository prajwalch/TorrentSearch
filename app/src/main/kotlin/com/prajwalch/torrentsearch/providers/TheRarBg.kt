package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.network.HttpClientResponse
import com.prajwalch.torrentsearch.util.DateUtils
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/** Represents an information extracted from the table row. */
private data class TableRowParsedResult(
    /** Name of the torrent. */
    val name: String,
    /** URL of the page where the info hash is located. */
    val detailsPageUrl: String,
    /**
     * Torrent size.
     *
     * The value and the unit should be extracted later.
     */
    val size: String,
    /** Number of seeders. */
    val seeders: String,
    /** Number of peers. */
    val peers: String,
    /** Torrent upload date. */
    val uploadDate: String,
    /** Torrent category. */
    val category: String,
)

class TheRarBg : SearchProvider, TorrentDetailsProvider {
    override val id = "therarbag"
    override val name = "TheRarBg"
    override val url = "https://therarbg.com"
    override val specializedCategory = Category.All
    override val safetyStatus = SearchProviderSafetyStatus.Unsafe(
        reason = R.string.therarbg_unsafe_reason
    )
    override val enabledByDefault = false

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/get-posts")
            append("/keywords:$query")

            if (context.category != Category.All) {
                val category = categoryName(raw = context.category)
                append(":category:$category")
            }
        }

        val resultPageHtml = context.httpClient.get(requestUrl)
        val parsedRows = withContext(Dispatchers.Default) {
            parseResultPage(html = resultPageHtml)
        }

        return parsedRows?.let {
            processParsedRows(
                parsedRows = it,
                httpClient = context.httpClient
            )
        }.orEmpty()
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return TheRarBgDetailsPageParser.parse(responseHtml)
    }

    /** Returns the compatible category string. */
    private fun categoryName(raw: Category): String = when (raw) {
        Category.All -> ""
        Category.Anime -> "Anime"
        Category.Apps -> "Apps"
        Category.Books -> "Books"
        Category.Games -> "Games"
        Category.Movies -> "Movies"
        Category.Music -> "Music"
        Category.Porn -> "XXX"
        Category.Series -> "Tv"
        Category.Other -> "Other"
    }

    /** Parses the HTML and returns all the parsed rows where the data is present. */
    private fun parseResultPage(html: String): List<TableRowParsedResult>? {
        return Jsoup
            // Results are presented in a <table>, which is the only one present
            // in a entire document.
            .parse(html)
            // For some time saving, we can directly grab the table body, where the
            // results are listed.
            .selectFirst("table > tbody")
            // Grab all the children i.e. <tr>.
            ?.children()
            // Parse it and extract all the available but required data from it.
            ?.mapNotNull { tr -> parseTableRow(tr = tr) }
    }

    /** Parses and extracts the necessary information from the table row. */
    private fun parseTableRow(tr: Element): TableRowParsedResult? {
        assert(tr.hasClass("list-entry"))

        val nameAnchorElement = tr
            .selectFirst("td:nth-child(2)")
            ?.selectFirst("a")
            ?: return null
        val detailsPath = nameAnchorElement.attr("href")
        val torrentName = nameAnchorElement.ownText()

        val category = tr
            .selectFirst("td:nth-child(3)")
            ?.selectFirst("a")
            ?.ownText()
            ?: return null
        val uploadDate = tr.selectFirst("td:nth-child(4)")
            ?.selectFirst("div")
            ?.ownText()
            ?.let { DateUtils.formatYearMonthDay(it) }
            ?: return null
        val size = tr.selectFirst("td:nth-child(6)")?.ownText() ?: return null
        val seeders = tr.selectFirst("td:nth-child(7)")?.ownText() ?: return null
        val peers = tr.selectFirst("td:nth-child(8)")?.ownText() ?: return null

        return TableRowParsedResult(
            name = torrentName,
            detailsPageUrl = "$url$detailsPath",
            size = size,
            seeders = seeders,
            peers = peers,
            uploadDate = uploadDate,
            category = category,
        )
    }

    /**
     * Further processes the parsed table rows and returns the list of final
     * [Torrent] if process completes successfully.
     */
    private suspend fun processParsedRows(
        parsedRows: List<TableRowParsedResult>,
        httpClient: HttpClient,
    ): List<Torrent> = supervisorScope {
        parsedRows
            .map { async { processParsedRow(parsedResult = it, httpClient = httpClient) } }
            .map { httpClient.withExceptionHandler { it.await() } }
            .mapNotNull { it as? HttpClientResponse.Ok }
            .mapNotNull { it.result }
    }

    /**
     * Further processes the parsed table row and returns the fully constructed
     * [Torrent] if process completes successfully.
     */
    private suspend fun processParsedRow(
        parsedResult: TableRowParsedResult,
        httpClient: HttpClient,
    ): Torrent? {
        // 1. Get the info hash from the details page.
        val (magnetUri, fileDownloadLink) = extractMagnetUriAndFileDownloadLink(
            httpClient = httpClient,
            detailsPageUrl = parsedResult.detailsPageUrl,
        ) ?: return null
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)

        val category = categoryFromRawString(parsedResult.category)

        return Torrent(
            infoHash = infoHash,
            name = parsedResult.name,
            size = parsedResult.size,
            seeders = parsedResult.seeders.toUInt(),
            peers = parsedResult.peers.toUInt(),
            providerName = name,
            uploadDate = parsedResult.uploadDate,
            category = category,
            descriptionPageUrl = parsedResult.detailsPageUrl,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
    }

    /** Extracts and returns the info hash from the details page, if exists. */
    private suspend fun extractMagnetUriAndFileDownloadLink(
        httpClient: HttpClient,
        detailsPageUrl: String,
    ): Pair<String, String?>? {
        val detailsPageHtml = httpClient.get(url = detailsPageUrl)

        return withContext(Dispatchers.Default) {
            val html = Jsoup.parse(detailsPageHtml)

            val magnetUri = html.selectFirst("a.magnet-btn")?.attr("href")
                ?: return@withContext null
            val fileDownloadLink = html.selectFirst("a.torrent-btn")?.attr("href")

            Pair(magnetUri, fileDownloadLink)
        }
    }

}

private object TheRarBgDetailsPageParser {
    private const val NAME = "div.postContL > h4:has(+ div.table-responsive)"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""

    suspend fun parse(html: String): TorrentDetails? = withContext(Dispatchers.Default) {
        val html = Jsoup.parse(html)

        val name = html.selectFirst(NAME)?.ownText() ?: return@withContext null
        val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)

        val detailRows = html.select("table.detailTable > tbody > tr")
            .mapNotNull { tr ->
                val label = tr.selectFirst("th")?.ownText() ?: return@mapNotNull null
                val value = tr.selectFirst("td") ?: return@mapNotNull null
                label to value
            }
            .toMap()
        val size = detailRows["Size:"]?.ownText()
        val seedersPeers = detailRows["Peers:"]?.ownText()?.trim()?.split(',')
        val seeders = seedersPeers?.firstOrNull()?.removePrefix("Seeders: ")?.toUIntOrNull()
        val peers =
            seedersPeers?.lastOrNull()?.trim()?.removePrefix("Leechers: ")?.toUIntOrNull()
        val uploadDate = detailRows["Added:"]?.ownText()
        val category = detailRows["Category:"]?.text()?.let(::categoryFromRawString)
        val uploader = detailRows["Uploader:"]?.text()
        val description = detailRows["Description:"]?.wholeText()

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
            description = description,
        )
    }
}

/** Returns the [Category] that matches the string extracted from page. */
private fun categoryFromRawString(raw: String): Category = when (raw) {
    "Anime" -> Category.Anime
    "Apps" -> Category.Apps
    "Books" -> Category.Books
    "Games" -> Category.Games
    "Movies" -> Category.Movies
    "Music" -> Category.Music
    "XXX" -> Category.Porn
    "Tv" -> Category.Series
    "Other" -> Category.Other
    else -> Category.Other
}