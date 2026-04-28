package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.GetTorrentDetailsResponse
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class XXXClub : SearchProvider {
    override val info = SearchProviderInfo(
        id = "xxxclub",
        name = "XXXClub",
        url = "https://xxxclub.to",
        specializedCategory = Category.Porn,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = "${info.url}/torrents/search/all/$query"

        val responseHtml = context.httpClient.get(url = requestUrl)
        val torrents = withContext(Dispatchers.Default) {
            parseHtml(html = responseHtml, httpClient = context.httpClient)
        }

        return torrents.orEmpty()
    }

    override suspend fun getDetails(detailsPageUrl: String): GetTorrentDetailsResponse {
        val responseHtml = HttpClient.get(detailsPageUrl)

        return XXXClubDetailsPageParser.parse(responseHtml, detailsPageUrl)
            ?.let(GetTorrentDetailsResponse::Success)
            ?: GetTorrentDetailsResponse.DetailsNotFound
    }

    private suspend fun parseHtml(html: String, httpClient: HttpClient): List<Torrent>? {
        return Jsoup
            .parse(html)
            .selectFirst("ul.tsearch")
            ?.children()
            ?.drop(1)
            ?.mapNotNull { li -> parseLi(li = li, httpClient = httpClient) }
    }

    private suspend fun parseLi(li: Element, httpClient: HttpClient): Torrent? {
        val torrentNameAnchor = li
            .selectFirst("span:nth-child(2) > a:nth-child(2)") ?: return null

        val torrentName = torrentNameAnchor.text()
        val descriptionPageUrl = info.url + torrentNameAnchor.attr("href")

        // 05 Aug 2025 07:23:05
        val uploadDateRaw = li.selectFirst("span.adde")?.ownText() ?: return null
        val uploadDate = parseUploadDate(raw = uploadDateRaw)

        val size = li.selectFirst("span.siz")?.ownText() ?: return null
        val seeders = li.selectFirst("span.see")?.ownText() ?: return null
        val peers = li.selectFirst("span.lee")?.ownText() ?: return null

        val (magnetUri, fileDownloadLink) = withContext(Dispatchers.IO) {
            extractMagnetUriAndFileDownloadLink(
                httpClient = httpClient,
                descriptionPageUrl = descriptionPageUrl,
            )
        } ?: return null
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)

        return Torrent(
            infoHash = infoHash,
            name = torrentName,
            size = size,
            seeders = seeders.toUIntOrNull() ?: 0u,
            peers = peers.toUIntOrNull() ?: 0u,
            providerName = info.name,
            uploadDate = uploadDate,
            category = info.specializedCategory,
            descriptionPageUrl = descriptionPageUrl,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
    }

    private fun parseUploadDate(raw: String): String {
        val lastSpaceIndex = raw
            .indexOfLast { ch -> ch == ' ' }
            .takeIf { it != -1 }
            ?: return raw

        return raw.substring(0..lastSpaceIndex).trim()
    }

    private suspend fun extractMagnetUriAndFileDownloadLink(
        httpClient: HttpClient,
        descriptionPageUrl: String,
    ): Pair<String, String?>? {
        val responseHtml = httpClient.get(url = descriptionPageUrl)

        return withContext(Dispatchers.Default) {
            val html = Jsoup.parse(responseHtml)

            val magnetUri = html.selectFirst("""a[href^="magnet:"]""")?.attr("href")
                ?: return@withContext null
            val fileDownloadLink =
                html.selectFirst("""a[href^="/torrents/download"]""")?.attr("href")

            Pair(magnetUri, fileDownloadLink)
        }
    }
}

private object XXXClubDetailsPageParser {
    private const val NAME = "body > div > div.middle > div.main-content > div > h1"
    private const val SIZE = "div.detailsdescr > ul > li:nth-child(2) > span:nth-child(3)"
    private const val SEEDERS = "div.detailsdescr font.see"
    private const val PEERS = "div.detailsdescr font.lee"
    private const val UPLOAD_DATE = "div.detailsdescr > ul > li:nth-child(3) > span:nth-child(3)"

    //    private const val CATEGORY = "div.detailsdescr > ul > li:nth-child(1) > span:nth-child(3)"
    private const val UPLOADER = "div.detailsdescr > ul > li:nth-child(6) > span:nth-child(3)"
    private const val LAST_CHECKED = "div.detailsdescr > ul > li:nth-child(5) > span:nth-child(3)"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""
    private const val FILE_DOWNLOAD_LINK =
        "div.detailsdescr > ul > li.downloadboxlist > span:nth-child(1) > a"
    private const val DESCRIPTION = "div.description"
    private const val POSTER_URL = "img.detailsposter"

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val name = html.selectFirst(NAME)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
            val size = html.selectFirst(SIZE)?.ownText()
            val seeders = html.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)?.ownText()
//            val category = html.selectFirst(CATEGORY)?.text()
            val uploader = html.selectFirst(UPLOADER)?.ownText()
            val lastChecked = html.selectFirst(LAST_CHECKED)?.ownText()
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
            val description = html.selectFirst(DESCRIPTION)
                ?.html()
                ?.let(TorrentUtils.HtmlToMarkdownConverter::convert)
            val posterUrl = html.selectFirst(POSTER_URL)?.attr("src")

            TorrentDetails(
                infoHash = infoHash,
                name = name,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = Category.Porn,
                uploader = uploader,
                lastChecked = lastChecked,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
                description = description,
                posterUrl = posterUrl,
            )
        }
}