package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class FileMood : SearchProvider, TorrentDetailsProvider {
    override val id = "filemood"
    override val name = "FileMood"
    override val url = "https://filemood.com"
    override val specializedCategory = Category.Other
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsPageParser = FileMoodResultsPageParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/result")
            append("?q=$query")
            append("+in%3Atitle")
        }
        val responseHtml = context.httpClient.get(url = requestUrl)
        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return FileMoodDetailsPageParser.parse(responseHtml)
    }
}

private class FileMoodResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup
                .parse(html, pageUrl)
                .select("table > tbody > tr:has(a.btn-success)")
                .mapNotNull { parseTableRow(it) }
        }

    private fun parseTableRow(tr: Element): Torrent? {
        val torrentName = tr.selectFirst("td.dn-title")?.text() ?: return null
        val size = tr.selectFirst("td.dn-size")?.text() ?: return null
        val (seeders, peers) = tr.selectFirst("td.dn-status")
            ?.text()
            ?.split('/')
            ?: return null
        val uploadDate = "0m ago"
        // #result-main-center > div > div > table:nth-child(2) > tbody > tr:nth-child(1) > td.dn-btn > div > a
        val descriptionPageUrl = tr.selectFirst("td.dn-btn > div > a")
            ?.attr("abs:href")
            ?: return null
        val infoHash = descriptionPageUrl
            .removeSuffix(".html")
            .takeLastWhile { it != '-' }
            .lowercase()
            .trim()

        return Torrent(
            infoHash = infoHash,
            name = torrentName,
            size = size,
            seeders = seeders.toUIntOrNull() ?: 0U,
            peers = peers.toUIntOrNull() ?: 0U,
            uploadDate = uploadDate,
            category = null,
            providerName = providerName,
            descriptionPageUrl = descriptionPageUrl,
        )
    }
}

private object FileMoodDetailsPageParser {
    private const val NAME =
        "#content-main-center > div > div > div.well > table:nth-child(1) > tbody > tr:nth-child(1) > td > h1 > b"
    private const val SIZE =
        "#content-main-center > div > div > div.well > table:nth-child(3) > tbody > tr:nth-child(2) > td:nth-child(2) > p > b"
    private const val LAST_CHECKED =
        "#content-main-center > div > div > div.well > table:nth-child(3) > tbody > tr:nth-child(4) > td:nth-child(2) > p > b"
    private const val INFO_HASH =
        "#content-main-center > div > div > div.well > table:nth-child(3) > tbody > tr:nth-child(5) > td:nth-child(2) > p"

    suspend fun parse(html: String): TorrentDetails? = withContext(Dispatchers.Default) {
        val html = Jsoup.parse(html)

        val name = html.selectFirst(NAME)?.ownText() ?: return@withContext null
        val infoHash = html.selectFirst(INFO_HASH)
            ?.ownText()
            ?.lowercase()
            ?.trim()
            ?: return@withContext null
        val magnetUri = TorrentUtils.createMagnetUri(infoHash)
        val size = html.selectFirst(SIZE)?.ownText()
        val lastChecked = html.selectFirst(LAST_CHECKED)?.ownText()?.takeIf { it.isNotBlank() }

        TorrentDetails(
            infoHash = infoHash,
            name = name,
            size = size,
            category = Category.Other,
            lastChecked = lastChecked,
            magnetUri = magnetUri,
        )
    }
}