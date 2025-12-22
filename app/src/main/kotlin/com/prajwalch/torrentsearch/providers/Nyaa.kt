package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.utils.DateUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class Nyaa : SearchProvider {
    override val info = SearchProviderInfo(
        id = "nyaasi",
        name = "Nyaa",
        url = "https://nyaa.si",
        specializedCategory = Category.Anime,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = true,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append("${info.url}/")
            // Filter = No filter (0)
            append("?f=0")
            // Category = Anime (1_0)
            append("&c=1_0")
            append("&q=$query")
        }

        val responseHtml = context.httpClient.get(url = requestUrl)
        val torrents = withContext(Dispatchers.Default) {
            parseResultPage(html = responseHtml)
        }

        return torrents.orEmpty()
    }

    /**
     * Parses the result page and returns all the extracted torrents, otherwise
     * returns `null` if the page has unexpected layout.
     */
    private fun parseResultPage(html: String): List<Torrent>? {
        return Jsoup
            .parse(html)
            .selectFirst("table.torrent-list > tbody")
            ?.children()
            ?.mapNotNull { tr -> parseTableRow(tr = tr) }
    }

    /**
     * Parses the row and returns the fully constructed [Torrent], if extraction
     * completes successfully, otherwise `null` if the row has unexpected layout.
     */
    private fun parseTableRow(tr: Element): Torrent? {
        val anchorElement = tr
            .selectFirst("td:nth-child(2)")
            ?.selectFirst("a:nth-child(2)")
            ?: return null
        val name = anchorElement.ownText()

        val descriptionPagePath = anchorElement.attr("href")
        val descriptionPageUrl = "${info.url}$descriptionPagePath"

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
            name = name,
            size = size,
            seeders = seeders.toUIntOrNull() ?: 0u,
            peers = peers.toUIntOrNull() ?: 0u,
            providerId = info.id,
            providerName = info.name,
            uploadDate = uploadDate,
            category = info.specializedCategory,
            descriptionPageUrl = descriptionPageUrl,
            infoHashOrMagnetUri = InfoHashOrMagnetUri.MagnetUri(magnetUri),
        )
    }
}