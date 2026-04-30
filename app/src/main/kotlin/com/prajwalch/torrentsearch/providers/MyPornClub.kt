package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.GetTorrentDetailsResponse
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class MyPornClub : SearchProvider {
    override val id = "mypornclub"
    override val name = "MyPornClub"
    override val url = "https://myporn.club"
    override val specializedCategory = Category.Porn
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsPageParser = MyPornClubResultsPageParser(
        providerName = name,
        providerSpecializedCategory = specializedCategory,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val formattedQuery = query.trim().replace("%20", "-")
        // TODO: Suffix can be used for sorting: /seeders, /latest, /hits, /views
        val url = "$url/s/$formattedQuery/seeders"
        val responseHtml = context.httpClient.get(url)

        return resultsPageParser.parse(html = responseHtml, pageUrl = url)
    }

    override suspend fun getDetails(detailsPageUrl: String): GetTorrentDetailsResponse {
        val responseHtml = HttpClient.get(detailsPageUrl)

        return MyPornClubDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
            ?.let(GetTorrentDetailsResponse::Success)
            ?: GetTorrentDetailsResponse.DetailsNotFound
    }
}

private class MyPornClubResultsPageParser(
    private val providerName: String,
    private val providerSpecializedCategory: Category,
) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup
                .parse(html, pageUrl)
                .select("div.torrents_list > div.torrent_element")
                .mapNotNull { parseRow(it) }
        }


    /** Parses a single search result row into a [Torrent] object. */
    private suspend fun parseRow(row: Element): Torrent? {
        val anchor = row.selectFirst("div.torrent_element_text_div > a:nth-child(2)") ?: return null
        val name = anchor.text().trim()
        val descriptionPageUrl = anchor.attr("abs:href")

        val (magnetUri, fileDownloadLink) = extractMagnetUriAndFileDownloadLink(descriptionPageUrl)
            ?: return null
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
            providerName = providerName,
            uploadDate = uploadDate,
            category = providerSpecializedCategory,
            descriptionPageUrl = descriptionPageUrl,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
    }


    /** Extracts magnet URI and file download link from the description page. */
    private suspend fun extractMagnetUriAndFileDownloadLink(
        descriptionPageUrl: String,
    ): Pair<String, String?>? {
        val html = HttpClient.get(descriptionPageUrl)
        val linksDiv = Jsoup.parse(html, descriptionPageUrl)
            .selectFirst("div.torrent_download_div")
            ?: return null
        val fileDownloadLink = linksDiv.selectFirst("a.td_btn")?.attr("abs:href")
        val magnetLink = linksDiv.selectFirst("a.md_btn")?.attr("href") ?: return null

        return Pair(magnetLink, fileDownloadLink)
    }
}

private object MyPornClubDetailsPageParser {
    private const val INFO_HASH = "div.torrent_info_div > div:nth-child(1)"
    private const val NAME = "div.torrent_text"
    private const val SIZE = "div.torrent_info_div span.tsize_span"
    private const val SEEDERS = "div.torrent_info_div span.teiv_seeders"
    private const val PEERS = "div.torrent_info_div span.teiv_leechers"
    private const val UPLOAD_DATE = "div.torrent_info_div > div:nth-child(9)"
    private const val UPLOADER = "div.torrent_info_div span.uploader_nick"
    private const val LAST_CHECKED = "div.torrent_info_div > div:nth-child(8)"
    private const val MAGNET_URI = "a.md_btn"
    private const val FILE_DOWNLOAD_LINK = "a.td_btn"

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val infoHash = html.selectFirst(INFO_HASH)
                ?.ownText()
                ?.removePrefix("[hash_info]:")
                ?.trim()
                ?.lowercase()
                ?: return@withContext null
            val name = html.selectFirst(NAME)
                ?.text()
                ?.takeWhile { it != '#' }
                ?.trim()
                ?: return@withContext null
            val size = html.selectFirst(SIZE)?.ownText()?.uppercase()
            val seeders = html.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
            val uploadDate = html.selectFirst(UPLOAD_DATE)
                ?.ownText()
                ?.removePrefix("[uploaded]:")
                ?.trim()
            val uploader = html.selectFirst(UPLOADER)?.ownText()?.removePrefix("@")
            val lastChecked = html.selectFirst(LAST_CHECKED)
                ?.ownText()
                ?.removePrefix("[last checked]:")
                ?.trim()
            val magnetUri = html.selectFirst(MAGNET_URI)
                ?.attr("href")
                ?: TorrentUtils.createMagnetUri(infoHash)
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")

            TorrentDetails(
                infoHash = infoHash,
                name = name,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = Category.Porn,
                uploader = uploader,
                lastChecked = lastChecked,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
            )
        }
}