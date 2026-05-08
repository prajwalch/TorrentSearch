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

class FileMood : SearchProvider, TorrentDetailsProvider {
    override val id = "filemood"
    override val name = "FileMood"
    override val url = "https://filemood.com"
    override val supportedCategories = setOf(Category.Other)
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
                .select(LIST_ITEM)
                .mapNotNull(::parseListItem)
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val torrentName = listItem.selectFirst(NAME)?.text() ?: return null
        val size = listItem.selectFirst(SIZE)?.text()
        val (seeders, peers) = listItem.selectFirst(SEEDERS_PEERS)?.text()
            ?.split('/')
            ?: listOf(null, null)
        val descriptionPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")
        val infoHash = descriptionPageUrl
            ?.removeSuffix(".html")
            ?.takeLastWhile { it != '-' }
            ?.lowercase()
            ?.trim()

        return Torrent(
            infoHash = infoHash ?: return null,
            name = torrentName,
            size = size ?: "0 KB",
            seeders = seeders?.toUIntOrNull() ?: 0U,
            peers = peers?.toUIntOrNull() ?: 0U,
            category = Category.Other,
            providerName = providerName,
            descriptionPageUrl = descriptionPageUrl,
        )
    }

    private companion object {
        private const val LIST_ITEM = "table > tbody > tr:has(a.btn-success)"
        private const val NAME = "> td.dn-title"
        private const val SIZE = "td.dn-size"
        private const val SEEDERS_PEERS = "td.dn-status"
        private const val DETAILS_PAGE_URL = "td.dn-btn > div > a"
    }
}

private object FileMoodDetailsPageParser {
    private const val TORRENT_NAME =
        "div.well > table:nth-child(1) > tbody > tr:nth-child(1) > td > h1 > b"
    private const val SIZE =
        "div.well > table:nth-child(3) > tbody > tr:nth-child(2) > td:nth-child(2) > p > b"
    private const val LAST_CHECKED =
        "div.well > table:nth-child(3) > tbody > tr:nth-child(4) > td:nth-child(2) > p > b"
    private const val INFO_HASH =
        "div.well > table:nth-child(3) > tbody > tr:nth-child(5) > td:nth-child(2) > p"

    suspend fun parse(html: String): TorrentDetails? = withContext(Dispatchers.Default) {
        val html = Jsoup.parse(html)

        val torrentName = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
        val infoHash = html.selectFirst(INFO_HASH)
            ?.ownText()
            ?.lowercase()
            ?.trim()
            ?: return@withContext null
        val magnetUri = TorrentUtils.createMagnetUri(infoHash)
        val size = html.selectFirst(SIZE)?.ownText()
        val lastChecked = html.selectFirst(LAST_CHECKED)?.ownText()
            ?.takeIf { it.isNotBlank() }
            ?.takeWhile { !it.isWhitespace() }
            ?.let { TorrentDateParser.parse(date = it, format = "yyyy-MM-dd") }

        TorrentDetails(
            infoHash = infoHash,
            name = torrentName,
            size = size,
            category = Category.Other,
            lastChecked = lastChecked,
            magnetUri = magnetUri,
        )
    }
}