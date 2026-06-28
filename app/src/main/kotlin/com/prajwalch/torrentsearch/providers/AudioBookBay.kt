package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class AudioBookBay : SearchProvider, LatestTorrentsProvider, TorrentDetailsProvider {
    override val id = "audiobookbay"
    override val name = "AudioBookBay"
    override val url = "https://audiobookbay.lu"
    override val supportedCategories = setOf(Category.Books)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsPageParser = AudioBookBayResultsPageParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = "$url/?s=$query"
        val responseHtml = context.httpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val responseHtml = HttpClient.get(url)
        return resultsPageParser.parse(html = responseHtml, pageUrl = url)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return AudioBookBayDetailsPageParser.parse(responseHtml)
    }
}

private class AudioBookBayResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .map { async { parseListItem(it) } }
                .awaitAll()
                .filterNotNull()
        }

    private suspend fun parseListItem(listItem: Element): Torrent? {
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href") ?: return null
        val infoHash = getInfoHash(detailsPageUrl) ?: return null

        val torrentName = listItem.selectFirst(TORRENT_NAME)?.ownText() ?: return null
        val torrentInfo = listItem.selectFirst(TORRENT_INFO)?.wholeText()?.lines()

        val size = torrentInfo
            ?.find { line -> line.startsWith("File Size: ") }
            ?.substringAfter("File Size: ")
            ?.trim()
            ?.removeSuffix("s")
        val uploadDate = torrentInfo
            ?.find { line -> line.startsWith("Posted: ") }
            ?.substringAfter("Posted: ")
            ?.trim()
            ?.let { TorrentDateParser.parse(date = it, format = "d MMM yyyy") }

        return Torrent(
            infoHash = infoHash,
            name = torrentName,
            size = size,
            uploadDate = uploadDate,
            category = Category.Books,
            providerName = providerName,
            descriptionPageUrl = detailsPageUrl,
        )
    }

    private suspend fun getInfoHash(detailsPageUrl: String): String? {
        return HttpClient.get(detailsPageUrl)
            .let(Jsoup::parse)
            .selectFirst("td:containsOwn(Info Hash:)")
            ?.nextElementSibling()
            ?.ownText()
    }

    private companion object {
        private const val LIST_ITEM = "div.post"
        private const val TORRENT_NAME = "div.postTitle > h2 > a"
        private const val TORRENT_INFO = "div.postContent > p:nth-child(3)"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}

private object AudioBookBayDetailsPageParser {
    private const val TORRENT_NAME = "div.postTitle > h1"
    private const val SIZE = "td:containsOwn(File Size:)"
    private const val SIZE_ALT = "td:containsOwn(Combined File Size:)"
    private const val UPLOAD_DATE = "td:containsOwn(Creation Date:)"
    private const val UPLOADER = "div.postContent > div:nth-child(1) > p:nth-child(1) > a"
    private const val INFO_HASH = "td:containsOwn(Info Hash:)"
    private const val DESCRIPTION = "div.desc"
    private const val POSTER_URL = """img[itemprop="image"]"""

    suspend fun parse(html: String): TorrentDetails? = withContext(Dispatchers.Default) {
        val html = Jsoup.parse(html)

        val torrentName = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
        val infoHash = html.selectFirst(INFO_HASH)
            ?.nextElementSibling()
            ?.ownText() ?: return@withContext null
        val size = (html.selectFirst(SIZE) ?: html.selectFirst(SIZE_ALT))
            ?.nextElementSibling()
            ?.text()
            ?.removeSuffix("s")
        val uploadDate = html.selectFirst(UPLOAD_DATE)
            ?.nextElementSibling()
            ?.ownText()
            ?.trim()
            ?.let(TorrentDateParser::parseRFC1123)
        val uploader = html.selectFirst(UPLOADER)?.ownText()
        val description = html.selectFirst(DESCRIPTION)?.html()
        val posterUrl = html.selectFirst(POSTER_URL)?.attr("src")

        TorrentDetails(
            infoHash = infoHash,
            magnetUri = TorrentUtils.createMagnetUri(infoHash),
            name = torrentName,
            size = size,
            uploadDate = uploadDate,
            category = Category.Books,
            uploader = uploader,
            description = description,
            posterUrl = posterUrl,
        )
    }
}