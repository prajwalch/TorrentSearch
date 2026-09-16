package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.domain.model.SearchProviderSafety
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.provider.LatestTorrentsProvider
import com.prajwalch.torrentsearch.provider.MagnetUriProvider
import com.prajwalch.torrentsearch.provider.SearchProvider
import com.prajwalch.torrentsearch.provider.SearchProviderId
import com.prajwalch.torrentsearch.provider.TopTorrentsProvider
import com.prajwalch.torrentsearch.provider.TorrentDetailsProvider
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class MyPornClub(private val networkClient: NetworkClient) :
    SearchProvider,
    LatestTorrentsProvider,
    TopTorrentsProvider,
    MagnetUriProvider,
    TorrentDetailsProvider {
    override val id = "mypornclub"
    override val name = "MyPornClub"
    override val url = "https://myporn.club"
    override val supportedCategories = setOf(Category.Porn)
    override val safety = SearchProviderSafety.Safe
    override val enabledByDefault = false

    private val resultsPageParser = MyPornClubResultsPageParser(id, name)

    override suspend fun search(query: String, category: Category): List<Torrent> {
        val formattedQuery = query.trim().replace("%20", "-")
        // TODO: Suffix can be used for sorting: /seeders, /latest, /hits, /views
        val url = "$url/s/$formattedQuery/seeders"
        val responseHtml = networkClient.getText(url)

        return resultsPageParser.parse(html = responseHtml, pageUrl = url)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = networkClient.getText(detailsPageUrl)
        return MyPornClubDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/ts/latest/alltime"
        val responseHtml = networkClient.getText(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/ts/hits/alltime"
        val responseHtml = networkClient.getText(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getMagnetUri(sourceUrl: String): String {
        val detailsPageHtml = networkClient.getText(sourceUrl)
        return MyPornClubDetailsPageParser.extractMagnetUri(detailsPageHtml)
            ?: error("Failed to retrieve magnet URI from '$sourceUrl'")
    }
}

private class MyPornClubResultsPageParser(
    private val providerId: SearchProviderId,
    private val providerName: String,
) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup
                .parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull(::parseListItem)
        }


    /** Parses a single search result row into a [Torrent] object. */
    private fun parseListItem(listItem: Element): Torrent? {
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)
            ?.attr("abs:href")
            ?: return null

        val torrentRemoteId = detailsPageUrl
            .takeWhile { it != '?' }
            .takeLastWhile { it != '/' }
        val torrentId = TorrentUtils.createTorrentId(
            providerId = providerId,
            sourceId = torrentRemoteId,
        )

        val name = listItem.selectFirst(NAME)?.ownText() ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)?.text()
            ?.let(TorrentDateParser::tryParseRelative)

        return Torrent(
            id = torrentId,
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            providerName = providerName,
            uploadDate = uploadDate,
            category = Category.Porn,
            magnetUri = MagnetUri.RequiresFetch(detailsPageUrl),
            descriptionPageUrl = detailsPageUrl,
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
                ?.let(TorrentDateParser::tryParseRelative)
            val uploader = html.selectFirst(UPLOADER)?.ownText()?.removePrefix("@")
            val lastChecked = html.selectFirst(LAST_CHECKED)
                ?.ownText()
                ?.removePrefix("[last checked]:")
                ?.trim()
                ?.let(TorrentDateParser::tryParseRelative)
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

    suspend fun extractMagnetUri(detailsPageHtml: String): String? =
        withContext(Dispatchers.Default) {
            Jsoup.parse(detailsPageHtml)
                .selectFirst(MAGNET_URI)
                ?.attr("href")
        }
}