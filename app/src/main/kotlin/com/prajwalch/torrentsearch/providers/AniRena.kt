package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.domain.models.Torrent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class AniRena : SearchProvider {
    override val info = SearchProviderInfo(
        id = "anirena",
        name = "AniRena",
        url = "https://anirena.com",
        specializedCategory = Category.Anime,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(info.url)
            append("/index.php")
            append("?t=2")
            append("&s=$query")
        }
        val responseHtml = context.httpClient.get(url = requestUrl)

        return withContext(Dispatchers.Default) {
            parseResponseHtml(html = responseHtml)
        }
    }

    private fun parseResponseHtml(html: String): List<Torrent> {
        return Jsoup
            .parse(html)
            .select("div.full2:not([id])")
            // Ignore header.
            .drop(1)
            .mapNotNull(::parseDetailsDiv)
    }

    private fun parseDetailsDiv(div: Element): Torrent? {
        require(div.hasClass("full2") && !div.hasAttr("id"))

        val tr = div.selectFirst("> table > tbody > tr") ?: return null

        val torrentName = tr
            .selectFirst("td.torrents_small_info_data1 > div:nth-child(1) > a")
            ?.ownText()
            ?: return null
        val magnetUri = tr
            .selectFirst("td.torrents_small_info_data2 > div > a:nth-child(2)")
            ?.attr("href")
            ?.let(InfoHashOrMagnetUri::MagnetUri)
            ?: return null
        val size = tr
            .selectFirst("td.torrents_small_size_data1")
            ?.ownText()
            ?: return null
        val seeders = tr
            .selectFirst("td.torrents_small_seeders_data1")
            ?.text()
            ?.toUIntOrNull()
            ?: 0U
        val peers = tr
            .selectFirst("td.torrents_small_leechers_data1")
            ?.text()
            ?.toUIntOrNull()
            ?: 0U

        /*
        val torrentIdContainingDiv = div.nextElementSibling() ?: return null
        val torrentId = torrentIdContainingDiv.attr("id").removePrefix("details")

        val uploadDate = getUploadDate(torrentId = torrentId, httpClient = /* Pass client */)
            ?.let(DateUtils::formatRFC1123Date)
            ?: return null
         */

        return Torrent(
            name = torrentName,
            size = size,
            seeders = seeders,
            peers = peers,
            // Getting upload date requires an additional request to
            // 'anirena.com/torrent_details.php?id={id}'. The ID can be found in
            // the 'id' attribute of the element next to given div as 'details{id}'.
            uploadDate = "0m ago",
            category = info.specializedCategory,
            providerName = info.name,
            descriptionPageUrl = "",
            infoHashOrMagnetUri = magnetUri,
        )
    }

    /*
    private suspend fun getUploadDate(torrentId: String, httpClient: HttpClient): String? {
        return withContext(Dispatchers.IO) {
            // https://www.anirena.com/torrent_details.php?id={id}
            val requestUrl = "${info.url}/torrent_details.php?id=$torrentId"

            httpClient.getJson(url = requestUrl)
                ?.asObject()
                ?.getString("UPLOADED")
        }
    }
     */
}