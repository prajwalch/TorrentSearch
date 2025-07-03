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
    override fun name() = "therarbg.com"

    override suspend fun search(
        query: String,
        context: SearchContext,
    ): List<Torrent> = coroutineScope {
        var requestUrl = "$BASE_URL/get-posts/keywords:$query"

        if (context.category != Category.All) {
            val category = getCategoryString(category = context.category)
            requestUrl = "$requestUrl:category:$category"
        }

        val resultsPageHtml = context.httpClient.get(requestUrl)
        val resultsTableBody = Jsoup
            .parse(resultsPageHtml)
            .selectFirst("table > tbody")
            ?: return@coroutineScope emptyList()

        resultsTableBody
            .children()
            .map { tr -> parseTableRow(tr = tr) }
            .map { parsedResult ->
                async {
                    processTableRowParsedResult(
                        httpClient = context.httpClient,
                        parsedResult = parsedResult
                    )
                }
            }
            .awaitAll()
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

    /** Parses and extracts the necessary information from the table row. */
    private fun parseTableRow(tr: Element): TableRowParsedResult {
        assert(tr.hasClass("list-entry"))

        val nameAnchorElement = tr.selectFirst("td:nth-child(2)")!!.selectFirst("a")!!
        val detailsPath = nameAnchorElement.attr("href")
        val torrentName = nameAnchorElement.ownText()

        val uploadDate = tr.selectFirst("td:nth-child(4)")!!.selectFirst("div")!!.ownText()
        val size = tr.selectFirst("td:nth-child(6)")!!.ownText()
        val seeds = tr.selectFirst("td:nth-child(7)")!!.ownText()
        val peers = tr.selectFirst("td:nth-child(8)")!!.ownText()

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
     * Further processes the table row parsed result and returns the fully
     * constructed [Torrent].
     */
    private suspend fun processTableRowParsedResult(
        httpClient: HttpClient,
        parsedResult: TableRowParsedResult,
    ): Torrent {
        // 1. Get the info hash from the details page.
        val detailsPageHtml = httpClient.get(url = parsedResult.detailsPageUrl)
        val infoHash = Jsoup.parse(detailsPageHtml).selectFirst(".info-hash-value")!!.ownText()

        // 2. Construct torrent data.
        val sizeValue = parsedResult.size.takeWhile { !it.isWhitespace() }.toFloat()
        val sizeUnit = parsedResult.size.takeLastWhile { !it.isWhitespace() }

        return Torrent(
            name = parsedResult.name,
            hash = infoHash,
            size = FileSize(value = sizeValue, unit = sizeUnit),
            seeds = parsedResult.seeds.toUInt(),
            peers = parsedResult.peers.toUInt(),
            providerName = name(),
            uploadDate = parsedResult.uploadDate,
        )
    }

    private companion object {
        private const val BASE_URL = "https://therarbg.com"
    }
}