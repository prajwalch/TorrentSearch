package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class MyPornClub : SearchProvider {
    override val info = SearchProviderInfo(
        id = "mypornclub",
        name = "MyPornClub",
        url = "https://myporn.club",
        specializedCategory = Category.Porn,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val formattedQuery = query.trim().replace("\\s+".toRegex(), "-")
        // TODO: Suffix can be used for sorting: /seeders, /latest, /hits, /views
        val url = "${info.url}/s/$formattedQuery/seeders"
        val responseHtml = context.httpClient.get(url)

        return withContext(Dispatchers.Default) {
            parseSearchResults(responseHtml, context)
        }
    }

    /** Parses the full HTML and returns a list of torrents. */
    private suspend fun parseSearchResults(html: String, context: SearchContext): List<Torrent> {
        val rows = Jsoup.parse(html).select("div.torrents_list > div.torrent_element")
        return rows.mapNotNull { parseRow(it, context) }
    }


    /** Parses a single search result row into a [Torrent] object. */
    private suspend fun parseRow(row: Element, context: SearchContext): Torrent? {
        val anchor = row.selectFirst("a[href^=\"/t/\"]") ?: return null
        val name = anchor.text().trim()
        val relativeDetailsUrl = anchor.attr("href")
        val descriptionPageUrl = info.url + relativeDetailsUrl

        val (magnetUri, fileDownloadLink) = extractMagnetUriAndFileDownloadLink(
            descriptionPageUrl = descriptionPageUrl,
            context = context,
        ) ?: return null
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)

        val size = row.select("div.torrent_element_info span").getOrNull(3)?.text().orEmpty()
        val seeders = row
            .select("div.torrent_element_info span")
            .getOrNull(9)
            ?.text()
            ?.toUIntOrNull()
            ?: 0u
        val peers = row
            .select("div.torrent_element_info span")
            .getOrNull(11)
            ?.text()
            ?.toUIntOrNull()
            ?: 0u

        val uploadDate = row.select("div.torrent_element_info span").getOrNull(1)?.text().orEmpty()

        return Torrent(
            infoHash = infoHash,
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            providerName = info.name,
            uploadDate = uploadDate,
            category = info.specializedCategory,
            descriptionPageUrl = descriptionPageUrl,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
    }


    /** Extracts magnet URI and file download link from the description page. */
    private suspend fun extractMagnetUriAndFileDownloadLink(
        descriptionPageUrl: String,
        context: SearchContext,
    ): Pair<String, String?>? {
        val html = context.httpClient.get(descriptionPageUrl)
        val linksDiv = Jsoup.parse(html).selectFirst("div.torrent_download_div") ?: return null

        val fileDownloadLink =
            linksDiv.selectFirst("a:nth-child(1)")?.attr("href")?.let { "https:$it" }
        val magnetLink = linksDiv.selectFirst("a:nth-child(2)")?.attr("href") ?: return null

        return Pair(magnetLink, fileDownloadLink)
    }

    private companion object {
        private val HASH_REGEX = Regex("""\[hash_info]:(\w{32,40})""")
    }
}