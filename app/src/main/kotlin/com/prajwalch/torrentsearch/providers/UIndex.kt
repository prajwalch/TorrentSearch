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

class UIndex : SearchProvider, TorrentDetailsProvider {
    override val id = "uindex"
    override val name = "UIndex"
    override val url = "https://uindex.org"
    override val specializedCategory = Category.All
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = true

    private val categoryMap = mapOf(
        Category.All to 0,
        Category.Books to 0,
        Category.Anime to 7,
        Category.Apps to 5,
        Category.Games to 3,
        Category.Movies to 1,
        Category.Music to 4,
        Category.Porn to 6,
        Category.Series to 2,
        Category.Other to 8,
    )

    private val resultsPageParser = UIndexResultsPageParser(
        providerName = name,
        baseUrl = url,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/search.php")
            append("?search=$query")
            append("&c=${categoryMap[context.category]}")
        }
        val responseHtml = context.httpClient.get(url = requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl).orEmpty()
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return UIndexDetailsPageParser.parse(responseHtml, detailsPageUrl)
    }
}

private class UIndexResultsPageParser(
    private val providerName: String,
    private val baseUrl: String,
) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent>? =
        withContext(Dispatchers.Default) {
            Jsoup
                .parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull { parseListItem(listItem = it) }
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val name = listItem.selectFirst(NAME)?.text() ?: return null
        val magnetUri = listItem.selectFirst(MAGNET_URI)?.attr("href") ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.filter { it != ',' }?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.filter { it != ',' }?.toUIntOrNull()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)?.ownText()
        val category = listItem.selectFirst(CATEGORY)?.ownText()?.let(::categoryFromRawString)
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = name,
            size = size ?: "0 KB",
            seeders = seeders ?: 0u,
            peers = peers ?: 0u,
            providerName = providerName,
            uploadDate = uploadDate ?: "0 min ago",
            category = category,
            descriptionPageUrl = detailsPageUrl ?: "",
            magnetUri = magnetUri,
        )
    }

    private companion object {
        private const val LIST_ITEM = "table.sr-table > tbody > tr"
        private const val NAME = "td.sr-col-name > a.sr-torrent-link"
        private const val SIZE = "td.sr-col-size"
        private const val SEEDERS = "td.sr-col-seeders > span.sr-seed"
        private const val PEERS = "td.sr-col-leechers > span.sr-leech"
        private const val UPLOAD_DATE = "td.sr-col-uploaded"
        private const val CATEGORY = "td.sr-col-cat > a.sr-cat-badge"
        private const val MAGNET_URI = "td.sr-col-name > a.sr-magnet"
        private const val DETAILS_PAGE_URL = NAME
    }
}

private object UIndexDetailsPageParser {
    private const val NAME = ".dt-title"
    private const val SIZE =
        "#content > div.dt-info-card > div > div:nth-child(1) > div:nth-child(2) > span.dt-info-value"
    private const val SEEDERS = ".dt-seed"
    private const val PEERS = ".dt-leech"
    private const val UPLOAD_DATE =
        "#content > div.dt-info-card > div > div:nth-child(1) > div:nth-child(3) > span.dt-info-value"
    private const val CATEGORY = ".sr-cat-badge"
    private const val PEERS_UPDATED =
        "#content > div.dt-info-card > div > div:nth-child(2) > div:nth-child(3) > span.dt-info-value"
    private const val DESCRIPTION = ".dt-descr-body"
    private const val THUMBNAIL = ".tmdb-poster > img"
    private const val PREVIEW_IMAGE = "img.torrent-img"
    private const val MAGNET_URI = "a.dt-download-btn"

    suspend fun parse(responseHtml: String, baseUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(responseHtml, baseUrl)

            // Required data. Return early as possible.
            val name = html.selectFirst(NAME)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)

            val size = html.selectFirst(SIZE)?.ownText()
            val seeders = html.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)?.ownText()
            val category = html.selectFirst(CATEGORY)?.ownText()?.let(::categoryFromRawString)
            val lastChecked = html.selectFirst(PEERS_UPDATED)?.text()?.takeIf { it.isNotBlank() }
            val description = html.selectFirst(DESCRIPTION)?.let {
                TorrentUtils.HtmlToMarkdownConverter.convert(it)
            }
            val thumbnailUrl = html.selectFirst(THUMBNAIL)?.attr("abs:src")
            val previewImageUrls = html.select(PREVIEW_IMAGE).map { it.attr("src") }

            TorrentDetails(
                infoHash = infoHash,
                name = name,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = category,
                lastChecked = lastChecked,
                magnetUri = magnetUri,
                description = description,
                posterUrl = thumbnailUrl,
                screenshotUrls = previewImageUrls,
            )
        }
}

private fun categoryFromRawString(raw: String): Category = when (raw) {
    "Anime" -> Category.Anime
    "Apps" -> Category.Apps
    "Games" -> Category.Games
    "Movies" -> Category.Movies
    "Music" -> Category.Music
    "XXX" -> Category.Porn
    "TV" -> Category.Series
    "Other" -> Category.Other
    else -> Category.Other
}