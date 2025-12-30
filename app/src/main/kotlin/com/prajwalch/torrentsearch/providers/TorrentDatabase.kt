package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.domain.models.Torrent
import com.prajwalch.torrentsearch.utils.DateUtils

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

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(info.url)
            append("/newest")
            append("?q=$query")

            val categoryId = getCategoryId(category = context.category)
            append("&category=$categoryId")
        }

        val responseHtml = context.httpClient.get(url = requestUrl)
        val torrents = withContext(Dispatchers.Default) {
            parseResponseHtml(html = responseHtml)
        }

        return torrents.orEmpty()
    }

    private fun getCategoryId(category: Category) = when (category) {
        Category.All,
        Category.Anime,
        Category.Other,
            -> ""

        Category.Apps -> "software"
        Category.Books -> "e-books"
        Category.Games -> "games"
        Category.Movies -> "movies"
        Category.Music -> "music"
        Category.Porn -> "porn"
        Category.Series -> "tv"
    }

    private fun parseResponseHtml(html: String): List<Torrent>? {
        return Jsoup
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
            ?.let { "${info.url}$it" }
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
            providerName = info.name,
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