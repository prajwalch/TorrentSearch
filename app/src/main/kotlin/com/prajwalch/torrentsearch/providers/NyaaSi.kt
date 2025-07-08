package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.Category
import com.prajwalch.torrentsearch.data.SearchContext
import com.prajwalch.torrentsearch.data.SearchProvider
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.utils.prettyDate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class NyaaSi : SearchProvider {
    override fun specializedCategory() = Category.Anime

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val queryParams = "?f=0&c=0_0&q=$query"
        val requestUrl = "$BASE_URL/?$queryParams"

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
        val descriptionPageUrl = "$BASE_URL$descriptionPagePath"

        val magnetUri = tr
            .selectFirst("td:nth-child(3)")
            ?.selectFirst("a:nth-child(2)")
            ?.attr("href")
            ?: return null
        val size = tr.selectFirst("td:nth-child(4)")?.ownText() ?: return null

        val uploadDateEpochSeconds = tr
            .selectFirst("td:nth-child(5)")
            ?.attr("data-timestamp")
            ?: return null
        val uploadDate = prettyDate(uploadDateEpochSeconds.toLong())

        val seeds = tr.selectFirst("td:nth-child(6)")?.ownText() ?: return null
        val peers = tr.selectFirst("td:nth-child(7)")?.ownText() ?: return null

        return Torrent(
            name = name,
            size = size,
            seeds = seeds.toUIntOrNull() ?: 0u,
            peers = peers.toUIntOrNull() ?: 0u,
            providerName = NAME,
            uploadDate = uploadDate,
            descriptionPageUrl = descriptionPageUrl,
            infoHashOrMagnetUri = InfoHashOrMagnetUri.MagnetUri(magnetUri),
        )
    }

    private companion object {
        private const val BASE_URL = "https://nyaa.si"
        private const val NAME = "nyaa.si"
    }
}