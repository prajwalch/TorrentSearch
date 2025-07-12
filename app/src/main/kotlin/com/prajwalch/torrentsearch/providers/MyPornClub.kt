package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.SearchContext
import com.prajwalch.torrentsearch.data.SearchProvider
import com.prajwalch.torrentsearch.data.SearchProviderId
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class MyPornClub(override val id: SearchProviderId) : SearchProvider {
    override fun specializedCategory(): Category = Category.Porn

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val formattedQuery = query.trim().replace("\\s+".toRegex(), "-")
        val url =
            "$BASE_URL/s/$formattedQuery/seeders" //TODO: Suffix can be used for sorting: /seeders, /latest, /hits, /views
        val html = try {
            context.httpClient.get(url)
        } catch (_: Exception) {
            return emptyList()
        }

        return withContext(Dispatchers.Default) {
            parseSearchResults(html, context)
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
        val descriptionPageUrl = BASE_URL + relativeDetailsUrl

        val infoHash = extractInfoHash(descriptionPageUrl, context) ?: return null

        val size = row.select("div.torrent_element_info span").getOrNull(3)?.text().orEmpty()
        val seeds =
            row.select("div.torrent_element_info span").getOrNull(9)?.text()?.toUIntOrNull() ?: 0u
        val peers =
            row.select("div.torrent_element_info span").getOrNull(11)?.text()?.toUIntOrNull() ?: 0u

        val uploadDate = row.select("div.torrent_element_info span").getOrNull(1)?.text().orEmpty()

        return Torrent(
            name = name,
            size = size,
            seeds = seeds,
            peers = peers,
            providerName = PROVIDER_NAME,
            uploadDate = uploadDate,
            category = Category.Porn,
            descriptionPageUrl = descriptionPageUrl,
            infoHashOrMagnetUri = InfoHashOrMagnetUri.InfoHash(infoHash)
        )
    }


    /** Extracts the info hash from the description page. */
    private suspend fun extractInfoHash(
        descriptionPageUrl: String,
        context: SearchContext,
    ): String? {
        return try {
            val html = context.httpClient.get(descriptionPageUrl)
            val infoDiv = Jsoup.parse(html).selectFirst("div.torrent_info_div > div") ?: return null

            // Example: [hash_info]:9b3efb2a550d42aff3e8ab1bb415e05535e440a9
            val text = infoDiv.ownText().trim()

            HASH_REGEX.find(text)?.groupValues?.getOrNull(1)
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        private const val BASE_URL = "https://myporn.club"
        private const val PROVIDER_NAME = "myporn.club"
        private val HASH_REGEX = Regex("""\[hash_info]:(\w{32,40})""")
    }
}