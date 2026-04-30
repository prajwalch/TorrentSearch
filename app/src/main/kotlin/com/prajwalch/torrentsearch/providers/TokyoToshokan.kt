package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.DateUtils
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class TokyoToshokan : SearchProvider, TorrentDetailsProvider {
    override val id = "tokyotoshokan"
    override val name = "TokyoToshokan"
    override val url = "https://tokyotosho.info"
    override val specializedCategory = Category.Anime
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = true

    private val resultsPageParser = TokyoToshokanResultsPageParser(
        providerName = name,
        providerSpecializedCategory = specializedCategory,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/search.php")
            append("?terms=$query")
            // Type = Anime (1)
            append("&type=1")
            // Match query with torrent name.
            append("&searchName=true")
        }

        val responseHtml = context.httpClient.get(url = requestUrl)
        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl).orEmpty()
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return TokyoToshokanDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }
}

private class TokyoToshokanResultsPageParser(
    private val providerName: String,
    private val providerSpecializedCategory: Category,
) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent>? =
        withContext(Dispatchers.Default) {
            Jsoup
                .parse(html, pageUrl)
                .selectFirst("table.listing > tbody")
                ?.children()
                ?.drop(1)
                ?.zipWithNext()
                ?.mapNotNull { (tr1, tr2) -> parseTableRow(tr1, tr2) }
        }

    private fun parseTableRow(tr1: Element, tr2: Element): Torrent? {
        // First tr's td contains magnet URI, torrent name and description page URL.
        //
        // Magnet URI and torrent name.
        val tr1SecondTd = tr1.selectFirst("td:nth-child(2)") ?: return null
        val magnetUri = tr1SecondTd
            .selectFirst("a:nth-child(1)")
            ?.attr("href")
            ?: return null
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)

        val nameAnchor = tr1SecondTd.selectFirst("a:nth-child(2)") ?: return null
        val torrentName = nameAnchor.text().replace(oldValue = " ", newValue = "")
        val fileDownloadLink = if (nameAnchor.attr("type") == "application/x-bittorrent") {
            nameAnchor.attr("href")
        } else {
            null
        }

        val descriptionPageUrl = tr1
            .selectFirst("td:nth-child(3)")
            ?.select("a")
            ?.last()
            ?.attr("abs:href")
            ?: return null

        // Second tr contains size, upload date, seeders and peers.
        //
        // Size and upload date.
        val tr2FirstTd = tr2.selectFirst("td:nth-child(1)") ?: return null
        val (sizeWithPrefix, uploadDateWithPrefix) = tr2FirstTd
            .ownText()
            .split('|')
            .drop(1)
            .map { it.trim() }

        val size = sizeWithPrefix.removePrefix("Size: ").let(FileSizeUtils::normalizeSize)
        val uploadDate = uploadDateWithPrefix
            .removePrefix("Date: ")
            .split(' ')
            .first()
            .let { DateUtils.formatYearMonthDay(it) }

        // Seeders and peers.
        val tr2SecondTd = tr2.selectFirst("td:nth-child(2)") ?: return null
        val seeders = tr2SecondTd.selectFirst("span:nth-child(1)")?.ownText() ?: return null
        val peers = tr2SecondTd.selectFirst("span:nth-child(2)")?.ownText() ?: return null

        return Torrent(
            infoHash = infoHash,
            name = torrentName,
            size = size,
            seeders = seeders.toUIntOrNull() ?: 0u,
            peers = peers.toUIntOrNull() ?: 0u,
            providerName = providerName,
            uploadDate = uploadDate,
            category = providerSpecializedCategory,
            descriptionPageUrl = descriptionPageUrl,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
    }
}

private object TokyoToshokanDetailsPageParser {
    private const val INFO_HASH = "#main > div.details > ul > li:nth-child(18)"
    private const val NAME = "#main > div.details > ul > li:nth-child(6) > a"
    private const val SIZE = "#main > div.details > ul > li:nth-child(10)"
    private const val SEEDERS = "#main > div.details > ul > li:nth-child(20)"
    private const val PEERS = "#main > div.details > ul > li:nth-child(22)"
    private const val UPLOAD_DATE = "#main > div.details > ul > li:nth-child(8)"

    //    private const val CATEGORY = "#main > div.details > ul > li:nth-child(2)"
    private const val UPLOADER = "#main > div.details > ul > li:nth-child(28)"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val nameAnchor = html.selectFirst(NAME) ?: return@withContext null
            val name = nameAnchor.text()
            val fileDownloadLink = if (nameAnchor.attr("type") == "application/x-bittorrent") {
                nameAnchor.attr("abs:href")
            } else {
                null
            }
            val infoHash = html.selectFirst(INFO_HASH)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href")
                ?: TorrentUtils.createMagnetUri(infoHash)
            val size = html.selectFirst(SIZE)?.ownText()?.let(FileSizeUtils::normalizeSize)
            val seeders = html.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)?.ownText()
//            val category = html.selectFirst(CATEGORY)?.text()
            val uploader = html.selectFirst(UPLOADER)?.ownText()

            TorrentDetails(
                infoHash = infoHash,
                name = name,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = Category.Anime,
                uploader = uploader,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
            )
        }
}