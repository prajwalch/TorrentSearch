package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class Mikan : SearchProvider, TorrentDetailsProvider {
    override val id = "mikanproject"
    override val name = "Mikan"
    override val url = "https://mikanani.me"
    override val supportedCategories = setOf(Category.Anime)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsPageParser = MikanResultsPageParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = "$url/Home/Search?searchstr=$query"
        val responseHtml = context.httpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return MikanDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }
}

private class MikanResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull(::parseListItem)
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val torrentName = listItem.selectFirst(TORRENT_NAME)?.ownText() ?: return null
        val magnetUri = listItem.selectFirst(MAGNET_URI)?.attr("data-clipboard-text") ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()?.let(FileSizeUtils::normalizeSize)
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)
            ?.ownText()
            ?.let { TorrentDateParser.parse(date = it, format = "yyyy/MM/dd HH:mm") }
        val fileDownloadLink = listItem.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size ?: "0 KB",
            seeders = 0U,
            peers = 0U,
            uploadDate = uploadDate,
            category = Category.Anime,
            providerName = providerName,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
            descriptionPageUrl = detailsPageUrl ?: "",
        )
    }

    private companion object {
        private const val LIST_ITEM = "tr.js-search-results-row"
        private const val TORRENT_NAME = "td:nth-child(2) > a:nth-child(1)"
        private const val SIZE = "td:nth-child(3)"
        private const val UPLOAD_DATE = "td:nth-child(4)"
        private const val MAGNET_URI = "td:nth-child(2) > a[data-clipboard-text]"
        private const val FILE_DOWNLOAD_LINK = "td:nth-child(5) > a"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}

private object MikanDetailsPageParser {
    private const val TORRENT_NAME = "p.episode-title"
    private const val TORRENT_INFO = "p.bangumi-info"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""
    private const val FILE_DOWNLOAD_LINK = """a[href^="/Download/"]"""
    private const val DESCRIPTION = "div.episode-desc"
    private const val SIZE_PREFIX = "文件大小："
    private const val UPLOAD_DATE_PREFIX = "发布日期："

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)
            val torrentName = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
            val description = html.selectFirst(DESCRIPTION)?.html()

            val infos = html.select(TORRENT_INFO).mapNotNull { it.ownText() }
            val size = infos.find { it.startsWith(SIZE_PREFIX) }
                ?.removePrefix(SIZE_PREFIX)
                ?.trim()
                ?.let(FileSizeUtils::normalizeSize)
            val uploadDate = infos.find { it.startsWith(UPLOAD_DATE_PREFIX) }
                ?.removePrefix(UPLOAD_DATE_PREFIX)
                ?.trimEnd()
                ?.let { TorrentDateParser.parse(date = it, format = "yyyy/MM/dd HH:mm") }

            TorrentDetails(
                infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
                name = torrentName,
                size = size,
                uploadDate = uploadDate,
                category = Category.Anime,
                magnetUri = magnetUri,
                description = description,
                fileDownloadLink = fileDownloadLink,
            )
        }
}