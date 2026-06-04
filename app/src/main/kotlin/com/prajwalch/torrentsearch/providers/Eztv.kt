package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class Eztv : SearchProvider {
    override val id = "eztvx"
    override val name = "Eztv"
    override val url = "https://eztvx.to"
    override val cloudflareSolverUrl = "$url/home"
    override val supportedCategories = setOf(Category.Series)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val isCloudflareProtected = true
    override val enabledByDefault = true

    private val resultsPageParser = EztvResultsPageParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = "$url/search/$query"
        // Without setting that cookie, it returns results without magnet links.
        val responseHtml = context.httpClient.get(
            url = requestUrl,
            headers = mapOf("Cookie" to "layout=def_wlinks"),
        )

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }
}

private class EztvResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .drop(2)
                .mapNotNull(::parseListItem)
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val torrentName = listItem.selectFirst(TORRENT_NAME)?.ownText() ?: return null
        val magnetUri = listItem.selectFirst(MAGNET_URI)?.attr("href") ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val fileDownloadLink = listItem.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size ?: "0 KB",
            seeders = seeders ?: 0U,
            peers = 0u,
            category = Category.Series,
            providerName = providerName,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
            descriptionPageUrl = detailsPageUrl ?: "",
        )
    }

    private companion object {
        private const val LIST_ITEM = "table:last-of-type > tbody > tr"
        private const val TORRENT_NAME = "td:nth-child(2) > a.epinfo"
        private const val SIZE = "td:nth-child(4)"
        private const val SEEDERS = "td:nth-child(6)"
        private const val MAGNET_URI = "td:nth-child(3) > a.magnet"
        private const val FILE_DOWNLOAD_LINK = "td:nth-child(3) > a:nth-child(2)"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}