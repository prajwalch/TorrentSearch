package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.Category
import com.prajwalch.torrentsearch.data.SearchContext
import com.prajwalch.torrentsearch.data.SearchProvider
import com.prajwalch.torrentsearch.models.FileSize
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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
    val seeds: String,
    /** Number of peers. */
    val peers: String,
    /** Torrent upload date. */
    val uploadDate: String,
)

class TheRarBg : SearchProvider {
    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        var requestUrl = "$BASE_URL/get-posts/keywords:$query"

        if (context.category != Category.All) {
            val category = getCategoryString(category = context.category)
            requestUrl = "$requestUrl:category:$category"
        }

        val resultsPageHtml = context.httpClient.get(requestUrl)
        return parseAndExtractTorrents(httpClient = context.httpClient, html = resultsPageHtml)
    }

    /** Returns the compatible category string. */
    private fun getCategoryString(category: Category): String {
        return when (category) {
            Category.All -> ""
            Category.Anime -> "Anime"
            Category.Apps -> "Apps"
            Category.Books -> "Books"
            Category.Games -> "Games"
            Category.Movies -> "Movies"
            Category.Music -> "Music"
            Category.Porn -> "XXX"
            Category.Series -> "Tv"
        }
    }

    /** Parses the HTML and returns all the extracted torrents from it. */
    private suspend fun parseAndExtractTorrents(
        httpClient: HttpClient,
        html: String,
    ): List<Torrent> = coroutineScope {
        // Results are presented in a <table>, which is the only one present
        // in a entire document.
        //
        // For some time saving, we can directly grab the table body, where the
        // results are listed.
        val resultsTableBody = Jsoup
            .parse(html)
            .selectFirst("table > tbody")
            ?: return@coroutineScope emptyList()

        resultsTableBody
            // Grab all the children i.e. <tr>.
            .children()
            // Parse it and extract all the available but required data from it.
            .mapNotNull { tr -> parseTableRow(tr = tr) }
            // The info hash is not presented in a table, we have to grab it
            // from the details page. Therefore, extract it and construct `Torrent`.
            .map { parsedResult ->
                async {
                    processParsedTableRow(httpClient = httpClient, parsedResult = parsedResult)
                }
            }
            .awaitAll()
            .filterNotNull()
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

        val uploadDate = tr.selectFirst("td:nth-child(4)")
            ?.selectFirst("div")
            ?.ownText()
            ?: return null
        val size = tr.selectFirst("td:nth-child(6)")?.ownText() ?: return null
        val seeds = tr.selectFirst("td:nth-child(7)")?.ownText() ?: return null
        val peers = tr.selectFirst("td:nth-child(8)")?.ownText() ?: return null

        return TableRowParsedResult(
            name = torrentName,
            detailsPageUrl = "$BASE_URL$detailsPath",
            size = size,
            seeds = seeds,
            peers = peers,
            uploadDate = uploadDate,
        )
    }

    /**
     * Further processes the parsed table row and returns the fully
     * constructed [Torrent] if process succeed.
     */
    private suspend fun processParsedTableRow(
        httpClient: HttpClient,
        parsedResult: TableRowParsedResult,
    ): Torrent? {
        // 1. Get the info hash from the details page.
        val infoHash = extractInfoHash(
            httpClient = httpClient,
            detailsPageUrl = parsedResult.detailsPageUrl,
        ) ?: return null

        // 2. Construct torrent data.
        val sizeValue = parsedResult.size.takeWhile { !it.isWhitespace() }.toFloat()
        val sizeUnit = parsedResult.size.takeLastWhile { !it.isWhitespace() }

        return Torrent(
            name = parsedResult.name,
            hash = infoHash,
            size = FileSize(value = sizeValue, unit = sizeUnit),
            seeds = parsedResult.seeds.toUInt(),
            peers = parsedResult.peers.toUInt(),
            providerName = NAME,
            uploadDate = parsedResult.uploadDate,
        )
    }

    /** Extracts and returns the info hash from the details page, if exists. */
    private suspend fun extractInfoHash(httpClient: HttpClient, detailsPageUrl: String): String? {
        val detailsPageHtml = httpClient.get(url = detailsPageUrl)
        return Jsoup.parse(detailsPageHtml).selectFirst(".info-hash-value")?.ownText()
    }

    private companion object {
        private const val BASE_URL = "https://therarbg.com"
        private const val NAME = "therarbg.com"
    }
}