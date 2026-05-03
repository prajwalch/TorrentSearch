package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class MyPornClub : SearchProvider, TorrentDetailsProvider {
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

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return MyPornClubDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
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
                .select(LIST_ITEM)
                .map { async { parseListItem(it) } }
                .awaitAll()
                .filterNotNull()
        }


    /** Parses a single search result row into a [Torrent] object. */
    private suspend fun parseListItem(listItem: Element): Torrent? {
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")
            ?: return null
        val detailsPageHtml = HttpClient.get(detailsPageUrl)
        val torrentDetails = MyPornClubDetailsPageParser.parse(
            html = detailsPageHtml,
            pageUrl = detailsPageUrl,
        ) ?: return null

        val name = listItem.selectFirst(NAME)?.ownText() ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)?.text()

        return Torrent(
            infoHash = torrentDetails.infoHash,
            name = name,
            size = size ?: "0 KB",
            seeders = seeders ?: 0U,
            peers = peers ?: 0U,
            providerName = providerName,
            uploadDate = uploadDate ?: "0 min ago",
            category = providerSpecializedCategory,
            descriptionPageUrl = detailsPageUrl,
            magnetUri = torrentDetails.magnetUri,
            fileDownloadLink = torrentDetails.fileDownloadLink,
        )
    }

    private companion object {
        private const val LIST_ITEM = "div.torrents_list > div.torrent_element"
        private const val NAME =
            "div.torrent_element_text_div > a:nth-child(2) > span.torrent_element_text_span"
        private const val SIZE = "div.torrent_element_info > span.teiv:nth-child(4)"
        private const val SEEDERS = "div.torrent_element_info > span.teiv.teiv_seeders"
        private const val PEERS = "div.torrent_element_info > span.teiv.teiv_leechers"
        private const val UPLOAD_DATE = "div.torrent_element_info > span.teiv:nth-child(2)"
        private const val DETAILS_PAGE_URL = "div.torrent_element_text_div > a:nth-child(2)"
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