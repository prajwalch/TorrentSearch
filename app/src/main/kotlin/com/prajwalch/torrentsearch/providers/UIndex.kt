package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.domain.models.Torrent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class UIndex : SearchProvider {
    override val info = SearchProviderInfo(
        id = "uindex",
        name = "UIndex",
        url = "https://uindex.org",
        specializedCategory = Category.All,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = true,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(info.url)
            append("/search.php")
            append("?search=$query")

            val categoryIndex = getCategoryIndex(category = context.category)
            append("&c=$categoryIndex")
        }

        val responseHtml = context.httpClient.get(url = requestUrl)
        val torrents = withContext(Dispatchers.Default) {
            parseHtml(html = responseHtml)
        }

        return torrents.orEmpty()
    }

    private fun getCategoryIndex(category: Category): Int = when (category) {
        Category.All, Category.Books -> 0
        Category.Anime -> 7
        Category.Apps -> 5
        Category.Games -> 3
        Category.Movies -> 1
        Category.Music -> 4
        Category.Porn -> 6
        Category.Series -> 2
        Category.Other -> 8
    }

    private fun parseHtml(html: String): List<Torrent>? {
        return Jsoup
            .parse(html)
            .selectFirst("table.maintable > tbody")
            ?.children()
            ?.mapNotNull { parseTableRow(tr = it) }
    }

    private fun parseTableRow(tr: Element): Torrent? {
        val categoryString = tr
            .selectFirst("td:nth-child(1)")
            ?.selectFirst("a")
            ?.ownText()
            ?: return null
        val category = getCategoryFromString(string = categoryString)

        // It contains magnet link, torrent name and upload date.
        val secondTd = tr.selectFirst("td:nth-child(2)") ?: return null
        val magnetUri = secondTd.selectFirst("a:nth-child(1)")?.attr("href") ?: return null
        // Anchor which contains a name and description page URL.
        val nameHref = secondTd.selectFirst("a:nth-child(2)") ?: return null
        val name = nameHref.ownText()
        val descriptionPageUrl = info.url + nameHref.attr("href")
        val uploadDate = secondTd.selectFirst("div")?.ownText() ?: return null

        val size = tr.selectFirst("td:nth-child(3)")?.ownText() ?: return null
        val seeders = tr
            .selectFirst("td:nth-child(4)")
            ?.selectFirst("span")
            ?.ownText()
            ?.filter { it != ',' }
            ?: return null
        val peers = tr
            .selectFirst("td:nth-child(5)")
            ?.selectFirst("span")
            ?.ownText()
            ?.filter { it != ',' }
            ?: return null

        return Torrent(
            name = name,
            size = size,
            seeders = seeders.toUIntOrNull() ?: 0u,
            peers = peers.toUIntOrNull() ?: 0u,
            providerName = info.name,
            uploadDate = uploadDate,
            category = category,
            descriptionPageUrl = descriptionPageUrl,
            infoHashOrMagnetUri = InfoHashOrMagnetUri.MagnetUri(uri = magnetUri),
        )
    }

    private fun getCategoryFromString(string: String): Category = when (string) {
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
}