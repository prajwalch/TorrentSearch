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

class XXXTracker : SearchProvider {
    override val info = SearchProviderInfo(
        id = "xxxtracker",
        name = "XXXTracker",
        url = "https://xxxtor.com",
        specializedCategory = Category.Porn,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(info.url)
            append("/b.php")
            append("?search=$query")
        }

        val responseHtml = context.httpClient.get(url = requestUrl)
        val torrents = withContext(Dispatchers.Default) {
            parseResponseHtml(html = responseHtml)
        }

        return torrents.orEmpty()
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return XXXTrackerDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }

    private fun parseResponseHtml(html: String): List<Torrent>? {
        return Jsoup
            .parse(html)
            .selectFirst("table > tbody")
            ?.select("tr")
            // First tr is used for heading.
            ?.drop(1)
            ?.mapNotNull { parseTr(it) }
    }

    private fun parseTr(tr: Element): Torrent? {
        val uploadDate = tr.selectFirst("td:nth-child(1)")?.ownText() ?: return null

        val secondTd = tr.selectFirst("td:nth-child(2)") ?: return null
        val magnetUri = secondTd.selectFirst("a:nth-child(1)")?.attr("href") ?: return null
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
        val fileDownloadLink =
            secondTd.selectFirst("a:nth-child(2)")?.attr("href")?.let { "${info.url}$it" }

        val nameHref = secondTd.selectFirst("a:nth-child(3)") ?: return null
        val name = nameHref.ownText()
        val descriptionPageUrl = nameHref.attr("href").let { "${info.url}$it" }

        val size = tr.selectFirst("td:nth-child(3)")?.ownText() ?: return null

        val fourthTd = tr.selectFirst("td:nth-child(4)") ?: return null
        val seeders = fourthTd.selectFirst("span:nth-child(1)")?.ownText() ?: return null
        val peers = fourthTd.selectFirst("span:nth-child(3)")?.ownText() ?: return null

        return Torrent(
            infoHash = infoHash,
            name = name,
            size = size,
            seeders = seeders.toUIntOrNull() ?: 0U,
            peers = peers.toUIntOrNull() ?: 0U,
            uploadDate = uploadDate,
            category = info.specializedCategory,
            descriptionPageUrl = descriptionPageUrl,
            providerName = info.name,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
    }
}

private object XXXTrackerDetailsPageParser {
    private const val NAME = "#content > h1"
    private const val SIZE = "#details > tbody > tr:nth-last-child(2) > td:nth-child(2)"
    private const val SEEDERS = "#details > tbody > tr:nth-last-child(7) > td:nth-child(2)"
    private const val PEERS = "#details > tbody > tr:nth-last-child(6) > td:nth-child(2)"
    private const val UPLOAD_DATE = "#details > tbody > tr:nth-last-child(3) > td:nth-child(2)"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""
    private const val FILE_DOWNLOAD_LINK = "#download > a:nth-child(1)"
    private const val POSTER_URL = "#details > tbody > tr:nth-child(1) > td:nth-child(2) > img"
    private const val DESCRIPTION = "#details > tbody > tr:nth-child(1) > td:nth-child(2)"

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val name = html.selectFirst(NAME)?.text() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
            val size = html.selectFirst(SIZE)?.text()?.takeWhile { it != '(' }?.trim()
            val seeders = html.selectFirst(SEEDERS)?.text()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.text()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)?.text()
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
            val posterUrl = html.selectFirst(POSTER_URL)?.attr("src")
            val description = html.selectFirst(DESCRIPTION)
                // Remove poster and two new lines after the poster from description.
                ?.apply { select("> *:lt(3)").remove() }
                ?.html()

            TorrentDetails(
                infoHash = infoHash,
                name = name,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = Category.Porn.name,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
                description = description,
                posterUrl = posterUrl,
            )
        }
}