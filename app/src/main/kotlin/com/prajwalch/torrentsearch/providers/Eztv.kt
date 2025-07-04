package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.Category
import com.prajwalch.torrentsearch.data.SearchContext
import com.prajwalch.torrentsearch.data.SearchProvider
import com.prajwalch.torrentsearch.models.FileSize
import com.prajwalch.torrentsearch.models.Torrent
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class Eztv : SearchProvider {
    override fun specializedCategory() = Category.Series

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = "$URL$query"
        // Without setting that cookie, it returns results without magnet links.
        val responseHtml = context.httpClient.get(
            url = requestUrl,
            headers = mapOf("Cookie" to "layout=def_wlinks"),
        )

        return parseHtml(html = responseHtml)
    }

    /** Parses the HTML and returns all the extracted [Torrent]s. */
    private fun parseHtml(html: String): List<Torrent> {
        // There are exactly 6 tables in the entire document, only the last one
        // will contain the search results.
        val resultsTableBody = Jsoup
            .parse(html)
            .select("table")
            .last()
            ?.selectFirst("tbody")
            ?: return emptyList()

        val tableRows = resultsTableBody.children()
        // The first one is unimportant header while the second is the actual
        // table header. Note that, it doesn't use the <thead> instead uses the
        // <tr> itself.
        //
        // To make sure we select the correct table, check the header has
        // expected data <td> or not.
        val headerRow = tableRows[1] ?: return emptyList()
        if (!validateHeader(headerRow)) {
            return emptyList()
        }

        return tableRows
            .subList(fromIndex = 2, toIndex = tableRows.size)
            .mapNotNull { tr -> parseTableRow(tr = tr) }
    }

    /**
     * Returns `true` if the header has expected elements and data.
     *
     * Header layout:
     *
     *    Show | Episode Name | Dload | Size | Released | Seeds
     */
    private fun validateHeader(headerTr: Element): Boolean {
        // Checking only the first two would be sufficient? I guess?
        val allTd = headerTr.select("td")

        if (allTd.size != 6) {
            return false
        }

        // "Show"
        val firstTd = allTd.first() ?: return false
        // "Episode Name"
        val secondTd = allTd[1] ?: return false

        return (firstTd.ownText() == "Show") && (secondTd.ownText() == "Episode Name")
    }

    /** Parses the table row and returns the [Torrent] if parse succeed. */
    private fun parseTableRow(tr: Element): Torrent? {
        val torrentName = tr
            .selectFirst("td:nth-child(2)")
            ?.selectFirst("a")
            ?.ownText()
            ?: return null

        val dloadTd = tr.selectFirst("td:nth-child(3)") ?: return null
        val infoHash = parseInfoHash(dloadTd = dloadTd) ?: return null

        val size = tr.selectFirst("td:nth-child(4)")?.ownText() ?: return null
        // TODO: The date format used the results page is 'time ago'
        //       (e.g. '7h 8m', '1 week', '1 mo'). The format we want
        //       is present in the details page. Let's extract it in future.
        val uploadDate = tr.selectFirst("td:nth-child(5)")?.ownText() ?: return null

        // Some torrents will not have any seeds (no idea why), in that case
        // it will contain '-' text node, and in other case it will contain a
        // <font> with number of seeds as its text node.
        //
        // Additional note: It uses ',' separator (e.g. 1,000).
        val seedsTd = tr.selectFirst("td:nth-child(6)") ?: return null
        val seeds = seedsTd.selectFirst("font")?.ownText()?.filter { it != ',' } ?: "0"
        // TODO: Peers is not present in the results page, we have to grab it
        //       from the details page. Let's do that in future.
        val peers = 0u

        // File size parsing.
        val sizeValue = size.takeWhile { !it.isWhitespace() }.toFloatOrNull() ?: return null
        val sizeUnit = size.takeLastWhile { !it.isWhitespace() }

        return Torrent(
            name = torrentName,
            hash = infoHash,
            size = FileSize(value = sizeValue, unit = sizeUnit),
            seeds = seeds.toUIntOrNull() ?: 0u,
            peers = peers,
            providerName = NAME,
            uploadDate = uploadDate,
        )
    }

    /**
     * Parses and returns the info hash from given <td>, if it's the correct
     * element.
     *
     * Table data layout:
     *     <anchor with magnet uri><anchor which is not needed>
     */
    private fun parseInfoHash(dloadTd: Element): String? {
        val firstAnchor = dloadTd.firstElementChild() ?: return null

        val magnetUri = firstAnchor.attr("href")
        val magnetUriPrefix = "magnet:?xt=urn:btih:"

        if (!magnetUri.startsWith(magnetUriPrefix)) {
            return null
        }

        // TODO: Instead of extracting info hash when the magnet uri is already
        //       present, add support for optional magnetUri field to the `Torrent`.
        //
        // magnet:?xt=urn:btih:47c99aeaddb262f2bf3d7d350305182847cf58bc&dn=...&tr=...&tr=....
        val infoHash = magnetUri.removePrefix(magnetUriPrefix).takeWhile { it != '&' }

        return infoHash
    }

    private companion object {
        private const val URL = "https://eztvx.to/search/"
        private const val NAME = "eztvx.to"
    }
}