package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

import kotlin.io.encoding.Base64

class BlueRoms : SearchProvider, TorrentDetailsProvider {
    override val id = "blueroms"
    override val name = "BlueRoms"
    override val url = "https://www.blueroms.ws"
    override val supportedCategories = setOf(Category.Games)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsPageParser = BlueRomsResultsPageParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = "$url/search?g=0&p=0&q=$query"
        val responseHtml = context.httpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return BlueRomsDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }
}

private class BlueRomsResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .map { async { parseListItem(it) } }
                .awaitAll()
                .filterNotNull()
        }

    suspend fun parseListItem(listItem: Element): Torrent? {
        val downloadPageLink = listItem.selectFirst(DOWNLOAD_PAGE_URL)
            ?.attr("abs:href") ?: return null
        val magnetUri = getMagnetUri(downloadPageLink) ?: return null

        val gameName = listItem.selectFirst(GAME_NAME)?.ownText() ?: return null
        val platform = listItem.selectFirst(PLATFORM)
            ?.nextElementSibling()
            ?.ownText()
        val torrentName = platform?.let { "$gameName - $it" } ?: gameName

        val size = listItem.selectFirst(SIZE)
            ?.nextSibling()
            ?.takeIf { it is TextNode }
            ?.nodeValue()
            ?.trim()
            ?.let(FileSizeUtils::normalizeSize)
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size,
            category = Category.Games,
            providerName = providerName,
            magnetUri = magnetUri,
            descriptionPageUrl = detailsPageUrl,
        )
    }

    private companion object {
        private const val LIST_ITEM = "div.row > div.col-xs-12 > div.card"
        private const val GAME_NAME = "h4.card-title > a"
        private const val SIZE = "strong:containsOwn(Size:)"
        private const val PLATFORM = "strong:containsOwn(Platform:)"
        private const val DOWNLOAD_PAGE_URL = "div.card-footer > a"
        private const val DETAILS_PAGE_URL = GAME_NAME
    }
}

private object BlueRomsDetailsPageParser {
    private const val GAME_NAME = "h3.custom-title"
    private const val PLATFORM = "strong:containsOwn(Platform:)"
    private const val SIZE = "strong:containsOwn(Files Size:)"
    private const val POSTER_URL = """img[src^="/static/game/"]:not(.img-thumbnail)"""
    private const val SCREENSHOT_URL =
        "div.panel:has(div.panel-heading:has(h3:containsOwn(Screenshots))) img.img-thumbnail"
    private const val DOWNLOAD_PAGE_LINK = """a[href^="/download/"]"""

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)
            val downloadPageUrl = html.selectFirst(DOWNLOAD_PAGE_LINK)
                ?.attr("abs:href")
                ?: return@withContext null
            val magnetUri = getMagnetUri(downloadPageUrl) ?: return@withContext null
            val gameName = html.selectFirst(GAME_NAME)?.ownText() ?: return@withContext null
            val platform = html.selectFirst(PLATFORM)?.parent()?.ownText()?.trim()
            val torrentName = platform?.let { "$gameName - $it" } ?: gameName
            val size = html.selectFirst(SIZE)
                ?.parent()
                ?.ownText()
                ?.trim()
                ?.let(FileSizeUtils::normalizeSize)
            val posterUrl = html.selectFirst(POSTER_URL)?.attr("abs:src")
            val screenshotUrls = html.select(SCREENSHOT_URL).mapNotNull {
                it.attr("abs:src").takeIf { url -> url.isNotBlank() }
            }

            TorrentDetails(
                infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
                magnetUri = magnetUri,
                name = torrentName,
                size = size,
                posterUrl = posterUrl,
                screenshotUrls = screenshotUrls,
            )
        }
}

private suspend fun getMagnetUri(downloadPageUrl: String): String? {
    val downloadPageHtml = withContext(Dispatchers.IO) { HttpClient.get(downloadPageUrl) }
    val encodedMagnetUri = withContext(Dispatchers.Default) {
        Jsoup.parse(downloadPageHtml)
            .selectFirst("button#magnet-button")
            ?.attr("data-link")
            ?.takeIf { it.isNotBlank() }
    } ?: return null

    return String(Base64.decode(encodedMagnetUri), Charsets.UTF_8)
}