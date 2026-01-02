package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.domain.models.Torrent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class FileMood : SearchProvider {
    override val info = SearchProviderInfo(
        id = "filemood",
        name = "FileMood",
        url = "https://filemood.com",
        specializedCategory = Category.Other,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(info.url)
            append("/result")
            append("?q=$query")
            append("+in%3Atitle")
        }
        val responseHtml = context.httpClient.get(url = requestUrl)

        return withContext(Dispatchers.Default) {
            parseResponseHtml(html = responseHtml)
        }
    }

    private fun parseResponseHtml(html: String): List<Torrent> {
        return Jsoup
            .parse(html)
            .select("table > tbody > tr:has(a.btn-success)")
            .mapNotNull { parseTableRow(it) }
    }

    private fun parseTableRow(tr: Element): Torrent? {
        val torrentName = tr.selectFirst("td.dn-title")?.text() ?: return null
        val size = tr.selectFirst("td.dn-size")?.text() ?: return null
        val (seeders, peers) = tr.selectFirst("td.dn-status")
            ?.text()
            ?.split('/')
            ?: return null
        val uploadDate = "0m ago"
        // #result-main-center > div > div > table:nth-child(2) > tbody > tr:nth-child(1) > td.dn-btn > div > a
        val descriptionPageUrl = tr.selectFirst("td.dn-btn > div > a")
            ?.attr("href")
            ?.let { "${info.url}$it" }
            ?: return null
        val infoHash = descriptionPageUrl
            .removeSuffix(".html")
            .takeLastWhile { it != '-' }
            .let(InfoHashOrMagnetUri::InfoHash)

        return Torrent(
            name = torrentName,
            size = size,
            seeders = seeders.toUIntOrNull() ?: 0U,
            peers = peers.toUIntOrNull() ?: 0U,
            uploadDate = uploadDate,
            category = null,
            providerName = info.name,
            descriptionPageUrl = descriptionPageUrl,
            infoHashOrMagnetUri = infoHash,
        )
    }
}