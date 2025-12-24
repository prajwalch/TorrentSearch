package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.utils.DateUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class Sukebei : SearchProvider {
    override val info = SearchProviderInfo(
        id = "sukebeinyaa",
        name = "Sukebei",
        url = "https://sukebei.nyaa.si",
        specializedCategory = Category.Porn,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append("${info.url}/")
            // Filter = No filter (0)
            append("?f=0")
            // Category = All categories (0_0)
            append("&c=0_0")
            append("&q=$query")
        }

        val responseHtml = context.httpClient.get(url = requestUrl)
        val torrents = withContext(Dispatchers.Default) {
            parseHtml(html = responseHtml)
        }

        return torrents.orEmpty()
    }

    private fun parseHtml(html: String): List<Torrent>? {
        return Jsoup
            .parse(html)
            .selectFirst("table.torrent-list > tbody")
            ?.children()
            ?.mapNotNull { tr -> parseTableRow(tr = tr) }
    }

    private fun parseTableRow(tr: Element): Torrent? {
        val nameAnchorElem = tr.selectFirst("td:nth-child(2)")?.selectFirst("a") ?: return null

        val torrentName = nameAnchorElem.ownText()
        val descriptionPageUrl = info.url + nameAnchorElem.attr("href")
        val magnetUri = tr
            .selectFirst("td:nth-child(3)")
            ?.selectFirst("a:nth-child(2)")
            ?.attr("href")
            ?: return null
        val size = tr.selectFirst("td:nth-child(4)")?.ownText() ?: return null
        val uploadDate = tr
            .selectFirst("td:nth-child(5)")
            ?.attr("data-timestamp")
            ?.let { DateUtils.formatEpochSecond(it.toLong()) }
            ?: return null
        val seeders = tr.selectFirst("td:nth-child(6)")?.ownText() ?: return null
        val peers = tr.selectFirst("td:nth-child(7)")?.ownText() ?: return null

        return Torrent(
            name = torrentName,
            size = size,
            seeders = seeders.toUIntOrNull() ?: 0u,
            peers = peers.toUIntOrNull() ?: 0u,
            providerName = info.name,
            uploadDate = uploadDate,
            category = info.specializedCategory,
            descriptionPageUrl = descriptionPageUrl,
            infoHashOrMagnetUri = InfoHashOrMagnetUri.MagnetUri(magnetUri),
        )
    }
}