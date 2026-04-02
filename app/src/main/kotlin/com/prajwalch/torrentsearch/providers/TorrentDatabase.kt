package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.DateUtils
import com.prajwalch.torrentsearch.util.FileSizeUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class TorrentDatabase : SearchProvider {
    override val info = SearchProviderInfo(
        id = "torrentdatabase",
        name = "TorrentDatabase",
        url = "https://developify.ca",
        specializedCategory = Category.All,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
    )

    private val categoryMap = mapOf(
        Category.All to "",
        Category.Anime to "",
        Category.Other to "",
        Category.Apps to "software",
        Category.Books to "e-books",
        Category.Games to "games",
        Category.Movies to "movies",
        Category.Music to "music",
        Category.Porn to "porn",
        Category.Series to "tv",
    )

    private val resultsPageParser = TdResultsPageParser(
        baseUrl = info.url,
        providerName = info.name,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(info.url)
            append("/newest")
            append("?q=$query")
            append("&category=${categoryMap[context.category]}")
        }

        val responseHtml = context.httpClient.get(url = requestUrl)
        return resultsPageParser.parse(responseHtml).orEmpty()
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return TdDetailsPageParser.parse(responseHtml, detailsPageUrl)
    }
}

private class TdResultsPageParser(
    private val baseUrl: String,
    private val providerName: String,
) {
    suspend fun parse(html: String): List<Torrent>? = withContext(Dispatchers.Default) {
        Jsoup
            .parse(html)
            .selectFirst("table.torrent-table > tbody")
            ?.select("tr")
            ?.mapNotNull { parseTableRow(it) }
    }

    private fun parseTableRow(tr: Element): Torrent? {
        val magnetLinkHref = tr.selectFirst("td.title-cell > a.magnet-link") ?: return null
        val torrentName = magnetLinkHref.ownText()
        val magnetUri = magnetLinkHref.attr("href")

        val descriptionPageUrl = tr
            .selectFirst("td.title-cell > a.info-button")
            ?.attr("href")
            ?.let { "$baseUrl$it" }
            ?: return null

        val categoryId = tr.selectFirst("span.category-bubble")?.ownText() ?: return null
        val category = getCategoryFromId(id = categoryId)

        val size = tr.selectFirst("td.size-cell")?.ownText() ?: return null
        val uploadDate = tr
            .selectFirst("td.date-cell")
            ?.ownText()
            ?.split(' ')
            ?.firstOrNull()
            ?.let(DateUtils::formatYearMonthDay)
            ?: return null

        val statsCell = tr.selectFirst("td:nth-child(5)") ?: return null
        val seeders = statsCell.selectFirst("> div > span:nth-child(1)")?.ownText() ?: return null
        val peers = statsCell.selectFirst("> div > span:nth-child(3)")?.ownText() ?: return null

        return Torrent(
            name = torrentName,
            size = size,
            seeders = seeders.toUIntOrNull() ?: 0U,
            peers = peers.toUIntOrNull() ?: 0U,
            uploadDate = uploadDate,
            category = category,
            providerName = providerName,
            descriptionPageUrl = descriptionPageUrl,
            infoHashOrMagnetUri = InfoHashOrMagnetUri.MagnetUri(magnetUri),
        )
    }

    private fun getCategoryFromId(id: String) = when (id) {
        "Software" -> Category.Apps
        "E-Books", "AudioBooks" -> Category.Books
        "Games" -> Category.Games
        "Movies" -> Category.Movies
        "Music" -> Category.Music
        "Porn" -> Category.Porn
        "TV" -> Category.Series
        else -> Category.Other
    }
}

private object TdDetailsPageParser {
    private const val TORRENT_NAME =
        "body > div.main-container > div.container.mt-5 > div.card.shadow-sm.mb-5.torrent-detail-card > div.card-header.d-flex.justify-content-between.align-items-center.torrent-cat-header > h4"
    private const val SIZE =
        "body > div.main-container > div.container.mt-5 > div.card.shadow-sm.mb-5.torrent-detail-card > div.card-body > div.row.mb-4 > div:nth-child(1) > ul > li:nth-child(2) > strong.db-value"
    private const val SEEDERS =
        "body > div.main-container > div.container.mt-5 > div.card.shadow-sm.mb-5.torrent-detail-card > div.card-body > div.row.mb-4 > div.col-lg-6.text-lg-end > ul > li:nth-child(1) > strong.text-success"
    private const val PEERS =
        "body > div.main-container > div.container.mt-5 > div.card.shadow-sm.mb-5.torrent-detail-card > div.card-body > div.row.mb-4 > div.col-lg-6.text-lg-end > ul > li:nth-child(2) > strong.text-danger"
    private const val UPLOAD_DATE =
        "body > div.main-container > div.container.mt-5 > div.card.shadow-sm.mb-5.torrent-detail-card > div.card-body > div.row.mb-4 > div:nth-child(1) > ul > li:nth-child(3) > strong.db-value"
    private const val CATEGORY = ".cat-badge"
    private const val UPLOADER =
        "body > div.main-container > div.container.mt-5 > div.card.shadow-sm.mb-5.torrent-detail-card > div.card-body > div.row.mb-4 > div:nth-child(1) > ul > li:nth-child(4) > a"
    private const val LAST_CHECKED =
        "body > div.main-container > div.container.mt-5 > div.card.shadow-sm.mb-5.torrent-detail-card > div.card-body > div.row.mb-4 > div:nth-child(1) > ul > li:nth-child(5) > strong.db-value"
    private const val DESCRIPTION =
        "body > div.main-container > div.container.mt-5 > div.card.shadow-sm.mb-5.torrent-info-card > div.card-body.torrent-info-content"
    private const val MAGNET_URI = ".magnet-link"

    suspend fun parse(html: String, baseUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html)

            val name = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null

            val size = html.selectFirst(SIZE)?.ownText()?.let(FileSizeUtils::normalizeSize)
                ?: "0 KB"
            val seeders = html.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull() ?: 1U
            val peers = html.selectFirst(PEERS)?.ownText()?.toUIntOrNull() ?: 1U
            val uploadDate = html.selectFirst(UPLOAD_DATE)?.ownText() ?: "0 min ago"
            val category = html.selectFirst(CATEGORY)?.ownText()
            val uploader = html.selectFirst(UPLOADER)?.ownText()
            val lastChecked = html.selectFirst(LAST_CHECKED)?.ownText()
            val description = html.selectFirst(DESCRIPTION)?.html()

            TorrentDetails(
                name = name,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = category,
                uploader = uploader,
                lastChecked = lastChecked,
                description = description,
                infoHashOrMagnetUri = InfoHashOrMagnetUri.MagnetUri(magnetUri),
            )
        }
}