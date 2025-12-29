package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.domain.models.Torrent

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

        val nameHref = secondTd.selectFirst("a:nth-child(3)") ?: return null
        val name = nameHref.ownText()
        val descriptionPageUrl = nameHref.attr("href").let { "${info.url}$it" }

        val size = tr.selectFirst("td:nth-child(3)")?.ownText() ?: return null

        val fourthTd = tr.selectFirst("td:nth-child(4)") ?: return null
        val seeders = fourthTd.selectFirst("span:nth-child(1)")?.ownText() ?: return null
        val peers = fourthTd.selectFirst("span:nth-child(3)")?.ownText() ?: return null

        return Torrent(
            name = name,
            size = size,
            seeders = seeders.toUIntOrNull() ?: 0U,
            peers = peers.toUIntOrNull() ?: 0U,
            uploadDate = uploadDate,
            category = info.specializedCategory,
            descriptionPageUrl = descriptionPageUrl,
            providerName = info.name,
            infoHashOrMagnetUri = InfoHashOrMagnetUri.MagnetUri(magnetUri),
        )
    }
}