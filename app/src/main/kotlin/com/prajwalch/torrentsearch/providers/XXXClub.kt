package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.domain.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient

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

        val magnetUri = withContext(Dispatchers.IO) {
            extractMagnetUri(
                httpClient = httpClient,
                descriptionPageUrl = descriptionPageUrl,
            )
        } ?: return null

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

    private fun parseUploadDate(raw: String): String {
        val lastSpaceIndex = raw
            .indexOfLast { ch -> ch == ' ' }
            .takeIf { it != -1 }
            ?: return raw

        return raw.substring(0..lastSpaceIndex).trim()
    }

    private suspend fun extractMagnetUri(
        httpClient: HttpClient,
        descriptionPageUrl: String,
    ): String? {
        val responseHtml = httpClient.get(url = descriptionPageUrl)

        return Jsoup
            .parse(responseHtml)
            .selectFirst("a.mg-link")
            ?.attr("href")
    }
}