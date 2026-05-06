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

class Nyaa : SearchProvider, TorrentDetailsProvider {
    override val id = "nyaasi"
    override val name = "Nyaa"
    override val url = "https://nyaa.si"
    override val specializedCategory = Category.Anime
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = true

    private val resultsPageParser = NyaaResultsPageParser(
        baseUrl = url,
        providerName = name,
        specializedCategory = specializedCategory,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append("$url/")
            // Filter = No filter (0)
            append("?f=0")
            // Category = Anime (1_0)
            append("&c=1_0")
            append("&q=$query")
        }

        val responseHtml = context.httpClient.get(url = requestUrl)
        return resultsPageParser.parse(responseHtml).orEmpty()
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return NyaaDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }
}

private class NyaaResultsPageParser(
    private val baseUrl: String,
    private val providerName: String,
    private val specializedCategory: Category,
) {
    /**
     * Parses the result page and returns all the extracted torrents, otherwise
     * returns `null` if the page has unexpected layout.
     */
    suspend fun parse(html: String): List<Torrent>? = withContext(Dispatchers.Default) {
        Jsoup
            .parse(html)
            .selectFirst("table.torrent-list > tbody")
            ?.children()
            ?.mapNotNull { tr -> parseTableRow(tr = tr) }
    }

    /**
     * Parses the row and returns the fully constructed [Torrent], if extraction
     * completes successfully, otherwise `null` if the row has unexpected layout.
     */
    private fun parseTableRow(tr: Element): Torrent? {
        val anchorElement = tr
            .selectFirst("td:nth-child(2)")
            ?.selectFirst("a:nth-child(2)")
            ?: return null
        val name = anchorElement.ownText()

        val descriptionPagePath = anchorElement.attr("href")
        val descriptionPageUrl = "$baseUrl$descriptionPagePath"

        val downloadLinks = tr.selectFirst("td:nth-child(3)") ?: return null
        val fileDownloadLink = downloadLinks
            .selectFirst("a:nth-child(1)")
            ?.attr("href")
            .let { "$baseUrl$it" }
        val magnetUri = downloadLinks
            .selectFirst("a:nth-child(2)")
            ?.attr("href")
            ?: return null
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
        val size = tr.selectFirst("td:nth-child(4)")?.ownText() ?: return null
        val uploadDate = tr
            .selectFirst("td:nth-child(5)")
            ?.attr("data-timestamp")
            ?.toLongOrNull()
            ?.let(TorrentDateParser::epochSecondToInstant)
        val seeders = tr.selectFirst("td:nth-child(6)")?.ownText() ?: return null
        val peers = tr.selectFirst("td:nth-child(7)")?.ownText() ?: return null

        return Torrent(
            infoHash = infoHash,
            name = name,
            size = size,
            seeders = seeders.toUIntOrNull() ?: 0u,
            peers = peers.toUIntOrNull() ?: 0u,
            providerName = providerName,
            uploadDate = uploadDate,
            category = specializedCategory,
            descriptionPageUrl = descriptionPageUrl,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
    }
}

private object NyaaDetailsPageParser {
    private const val TORRENT_NAME = "body > div > div:nth-child(1) > div.panel-heading > h3"
    private const val SIZE =
        "body > div > div:nth-child(1) > div.panel-body > div:nth-child(4) > div:nth-child(2)"
    private const val SEEDERS =
        "body > div > div:nth-child(1) > div.panel-body > div:nth-child(2) > div:nth-child(4)"
    private const val PEERS =
        "body > div > div:nth-child(1) > div.panel-body > div:nth-child(2) > div:nth-child(4)"
    private const val UPLOAD_DATE =
        "body > div > div:nth-child(1) > div.panel-body > div:nth-child(1) > div:nth-child(4)"
    private const val UPLOADER =
        "body > div > div:nth-child(1) > div.panel-body > div:nth-child(2) > div:nth-child(2)"
    private const val DESCRIPTION = "#torrent-description"
    private const val MAGNET_URI = """a[href^="magnet:"]"""
    private const val FILE_DOWNLOAD_LINK = """a[href^="/download"]"""

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val name = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)

            val size = html.selectFirst(SIZE)?.ownText()
            val seeders = html.selectFirst(SEEDERS)?.text()?.trim()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.text()?.trim()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)
                ?.attr("data-timestamp")
                ?.toLongOrNull()
                ?.let(TorrentDateParser::epochSecondToInstant)
            val uploader = html.selectFirst(UPLOADER)?.text()?.trim()
            val description = html.selectFirst(DESCRIPTION)?.html()
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")

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
                description = description,
            )
        }
}