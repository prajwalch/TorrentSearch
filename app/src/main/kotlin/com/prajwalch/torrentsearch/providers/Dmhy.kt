package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.domain.models.Torrent
import com.prajwalch.torrentsearch.utils.DateUtils
import com.prajwalch.torrentsearch.utils.FileSizeUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class Dmhy : SearchProvider {
    override val info = SearchProviderInfo(
        id = "dmhy",
        name = "dmhy",
        url = "https://share.dmhy.org",
        specializedCategory = Category.All,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(info.url)
            append("/topics")
            append("/list")
            append("?keyword=$query")
        }
        val responseHtml = context.httpClient.get(url = requestUrl)

        return withContext(Dispatchers.Default) {
            parseResponseHtml(html = responseHtml)
        }
    }

    private fun parseResponseHtml(html: String): List<Torrent> {
        return Jsoup
            .parse(html)
            .select("table tbody tr:has(a[href^=\"magnet:?\"])")
            .mapNotNull(::parseTableRow)
    }

    private fun parseTableRow(tr: Element): Torrent? {
        val uploadDate = tr
            .selectFirst("td:nth-child(1) span")
            ?.ownText()
            ?.split(' ')
            ?.firstOrNull()
            ?.replace("/", "-")
            ?.let(DateUtils::formatYearMonthDay)
            ?: return null
        val category = tr
            .selectFirst("td:nth-child(2) a")
            ?.attr("href")
            ?.removePrefix("/topics/list/sort_id/")
            ?.let(::getCategoryFromId)
            ?: return null

        val titleHref = tr
            .selectFirst("td.title a")
            ?: return null
        val torrentName = titleHref.ownText()
        val descriptionPageUrl = titleHref.attr("href").let { "${info.url}$it" }

        val magnetUri = tr
            .selectFirst("a[href^=\"magnet:\"]")
            ?.attr("href")
            ?.let(InfoHashOrMagnetUri::MagnetUri)
            ?: return null
        val size = tr
            .selectFirst("td:nth-child(5)")
            ?.ownText()
            ?.let(FileSizeUtils::normalizeSize)
            ?: return null
        val seeders = tr
            .selectFirst("td:nth-child(6)")
            ?.ownText()
            ?.toUIntOrNull()
            ?: 0u
        val peers = tr
            .selectFirst("td:nth-child(7)")
            ?.ownText()
            ?.toUIntOrNull()
            ?: 0u

        return Torrent(
            name = torrentName,
            size = size,
            seeders = seeders,
            peers = peers,
            uploadDate = uploadDate,
            category = category,
            providerName = info.name,
            descriptionPageUrl = descriptionPageUrl,
            infoHashOrMagnetUri = magnetUri,
        )
    }

    private fun getCategoryFromId(id: String) = when (id) {
        "2", "7", "31" -> Category.Anime
        "3" -> Category.Books
        "41", "42" -> Category.Series
        "4", "43", "44", "15" -> Category.Music
        "6" -> Category.Series
        "9", "17", "18", "19", "20", "21" -> Category.Games
        else -> Category.Other
    }
}