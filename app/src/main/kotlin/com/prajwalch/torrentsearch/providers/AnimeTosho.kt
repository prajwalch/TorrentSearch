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
import org.jsoup.nodes.TextNode

import java.time.Instant

class AnimeTosho : SearchProvider, TorrentDetailsProvider {
    override val id = "animetosho"
    override val name = "AnimeTosho"
    override val url = "https://animetosho.org"
    override val specializedCategory = Category.Anime
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = true

    private val resultsPageParser = AnimeToshoResultsPageParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = "$url/search?q=$query"
        val responseHtml = context.httpClient.get(url = requestUrl)

        return resultsPageParser.parse(responseHtml)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return AnimeToshoDetailsPageParser.parse(html = responseHtml, baseUrl = detailsPageUrl)
    }
}

private class AnimeToshoResultsPageParser(
//    private val baseUrl: String,
    private val providerName: String,
) {
    suspend fun parse(html: String): List<Torrent> = withContext(Dispatchers.Default) {
        Jsoup
            .parse(html)
            .select("div.home_list_entry")
            .mapNotNull { parseEntryDiv(it) }
    }

    /** Parses an individual result row into a [Torrent] object. */
    private fun parseEntryDiv(entryDiv: Element): Torrent? {
        val anchor = entryDiv.selectFirst("div.link > a") ?: return null
        val name = anchor.text()
        val descriptionPageUrl = anchor.attr("href")

        val size = entryDiv.selectFirst("div.size")?.ownText() ?: return null
        val (seeders, peers) = parseSeedsAndPeers(entryDiv)

        val uploadDate = parseUploadDate(entryDiv) ?: return null

        val links = entryDiv.selectFirst("div.links") ?: return null
        val fileDownloadLink = links.selectFirst("a.dllink")?.attr("href")
        val magnetUri = links.selectFirst("""a[href^="magnet:"]""")?.attr("href") ?: return null
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)

        return Torrent(
            infoHash = infoHash,
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            providerName = providerName,
            uploadDate = uploadDate,
            category = Category.Anime,
            descriptionPageUrl = descriptionPageUrl,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
    }

    /** Parses the upload date and converts "Today"/"Yesterday" into real dates. */
    private fun parseUploadDate(entryDiv: Element): Instant? {
        val raw = entryDiv
            .selectFirst("div.date")
            ?.attr("title")
            ?.removePrefix(DATE_PREFIX)
            ?.trim()
            ?: return null

        return when {
            raw.startsWith("Today") -> TorrentDateParser.getTodayDate()
            raw.startsWith("Yesterday") -> TorrentDateParser.getYesterdayDate()
            else -> {
                raw
                    .split(' ', limit = 2)
                    .firstOrNull()
                    ?.let { TorrentDateParser.parse(date = it, format = "d/M/yyyy") }
            }
        }
    }

    /** Extracts seeds and peers from the stats block. */
    private fun parseSeedsAndPeers(entryDiv: Element): Pair<UInt, UInt> {
        val span = entryDiv
            .selectFirst("div.links")
            ?.select("span")
            ?.firstOrNull { span -> span.hasAttr("title") }
            ?: return Pair(0u, 0u)
        val spanText = span.ownText()

        val match = STATS_REGEX.find(spanText)
        val seeds = match?.groupValues?.getOrNull(1)?.toUIntOrNull() ?: 0u
        val peers = match?.groupValues?.getOrNull(2)?.toUIntOrNull() ?: 0u

        return seeds to peers
    }

    private companion object {
        private const val DATE_PREFIX = "Date/time submitted: "
        private val STATS_REGEX = """\[(\d+)↑/(\d+)↓]""".toRegex()
    }
}

private object AnimeToshoDetailsPageParser {
    private const val TORRENT_NAME = "#title"
    private const val SIZE = """span[title^="File size:"]"""
    private const val SEEDERS = """td[title="Seeders"][align="right"]"""
    private const val PEERS = """td[title="Leechers"][align="right"]"""
    private const val UPLOAD_DATE = "#content > table:nth-of-type(1) > tbody > tr:nth-child(2) > td"
    private const val SCREENSHOT = "a.screenthumb"
    private const val MAGNET_URI = """a[href^="magnet:"]"""
    private const val FILE_DOWNLOAD_LINK = """a[href^="https://animetosho.org/storage/torrent"]"""

    suspend fun parse(html: String, baseUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, baseUrl)

            val name = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
            val magnetUriElem = html.selectFirst(MAGNET_URI) ?: return@withContext null
            val magnetUri = magnetUriElem.attr("href").takeIf { it.isNotBlank() }
                ?: return@withContext null
            val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)

            val unprocessedSize = html.selectFirst(SIZE)?.ownText()
                ?: magnetUriElem.nextSibling()?.takeIf { it is TextNode }?.nodeValue()
            val size = unprocessedSize
                ?.trim()
                ?.filterNot { it in setOf('(', ')', '|') }
                ?.takeIf { it.isNotBlank() }

            val seeders = html.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)?.ownText()
            val screenshotUrls = html.select(SCREENSHOT).map { it.attr("href") }
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("href")

            TorrentDetails(
                infoHash = infoHash,
                name = name,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = Category.Anime,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
                screenshotUrls = screenshotUrls,
            )
        }
}