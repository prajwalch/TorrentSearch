package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.network.HttpClientResponse

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

class TheRarBg(val id: SearchProviderId) : SearchProvider {
    override val info = SearchProviderInfo(
        id = id,
        name = "TheRarBg",
        url = "https://therarbg.com"
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        var requestUrl = "${info.url}/get-posts/keywords:$query"

        if (context.category != Category.All) {
            val category = getCategoryString(category = context.category)
            requestUrl = "$requestUrl:category:$category"
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

    /** Returns the compatible category string. */
    private fun getCategoryString(category: Category): String = when (category) {
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
            ?: return null
        val size = tr.selectFirst("td:nth-child(6)")?.ownText() ?: return null
        val seeders = tr.selectFirst("td:nth-child(7)")?.ownText() ?: return null
        val peers = tr.selectFirst("td:nth-child(8)")?.ownText() ?: return null

        return TableRowParsedResult(
            name = torrentName,
            detailsPageUrl = "${info.url}$detailsPath",
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
        val infoHash = extractInfoHash(
            httpClient = httpClient,
            detailsPageUrl = parsedResult.detailsPageUrl,
        ) ?: return null

        val category = getCategoryFromString(string = parsedResult.category)

        return Torrent(
            name = parsedResult.name,
            size = parsedResult.size,
            seeders = parsedResult.seeders.toUInt(),
            peers = parsedResult.peers.toUInt(),
            providerId = info.id,
            providerName = info.name,
            uploadDate = parsedResult.uploadDate,
            category = category,
            descriptionPageUrl = parsedResult.detailsPageUrl,
            infoHashOrMagnetUri = InfoHashOrMagnetUri.InfoHash(infoHash),
        )
    }

    /** Extracts and returns the info hash from the details page, if exists. */
    private suspend fun extractInfoHash(httpClient: HttpClient, detailsPageUrl: String): String? {
        val detailsPageHtml = httpClient.get(url = detailsPageUrl)

        return withContext(Dispatchers.Default) {
            Jsoup.parse(detailsPageHtml).selectFirst(".info-hash-value")?.ownText()
        }
    }

    /** Returns the [Category] that matches the string extracted from page. */
    private fun getCategoryFromString(string: String): Category = when (string) {
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
}