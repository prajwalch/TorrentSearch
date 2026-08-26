package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.TextNode

class Eztv(private val networkClient: NetworkClient) : SearchProvider, TorrentDetailsProvider {
    override val id = "eztvx"
    override val name = "Eztv"
    override val url = "https://eztvx.to"
    override val cloudflareSolverUrl = "$url/home"
    override val supportedCategories = setOf(Category.Series)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val isCloudflareProtected = true
    override val enabledByDefault = true

    private val resultsPageParser = EztvResultsPageParser(name)

    override suspend fun search(query: String, category: Category): List<Torrent> {
        val requestUrl = "$url/search/$query"
        // Without setting that cookie, it returns results without magnet links.
        val responseHtml = networkClient.getText(
            url = requestUrl,
            headers = mapOf("Cookie" to "layout=def_wlinks"),
        )

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = networkClient.getText(detailsPageUrl)
        return EztvDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }
}

private class EztvResultsPageParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Ksoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .drop(2)
                .mapNotNull(::parseListItem)
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val torrentName = listItem.selectFirst(TORRENT_NAME)?.ownText() ?: return null
        val magnetUri = listItem.selectFirst(MAGNET_URI)?.attr("href") ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val fileDownloadLink = listItem.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href")

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size,
            seeders = seeders,
            peers = 0u,
            category = Category.Series,
            providerName = providerName,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
            descriptionPageUrl = detailsPageUrl,
        )
    }

    private companion object {
        private const val LIST_ITEM = "table:last-of-type > tbody > tr"
        private const val TORRENT_NAME = "td:nth-child(2) > a.epinfo"
        private const val SIZE = "td:nth-child(4)"
        private const val SEEDERS = "td:nth-child(6)"
        private const val MAGNET_URI = "td:nth-child(3) > a.magnet"
        private const val FILE_DOWNLOAD_LINK = "td:nth-child(3) > a:nth-child(2)"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}

private object EztvDetailsPageParser {
    private const val TORRENT_NAME =
        "#header_holder > table > tbody > tr:nth-child(1) > td > h1 > span"
    private const val TORRENT_INFO_CONTAINER =
        "#header_holder > table > tbody > tr:nth-child(2) > td > table.episode_columns_holder > tbody > tr:nth-child(1) > td:nth-child(3) > table > tbody > tr:nth-child(2) > td > table > tbody > tr:nth-child(1) > td"
    private const val SEEDERS =
        "#header_holder > table > tbody > tr:nth-child(2) > td > table.episode_columns_holder > tbody > tr:nth-child(1) > td.episode_middle_column > table:nth-child(2) > tbody > tr:nth-child(2) > td > div > table > tbody > tr > td:nth-child(2) > span.stat_red"
    private const val PEERS =
        "#header_holder > table > tbody > tr:nth-child(2) > td > table.episode_columns_holder > tbody > tr:nth-child(1) > td.episode_middle_column > table:nth-child(2) > tbody > tr:nth-child(2) > td > div > table > tbody > tr > td:nth-child(2) > span.stat_green"
    private const val MAGNET_URI = """a[title="Magnet Link"]"""
    private const val FILE_DOWNLOAD_LINK = """a[title="Download Torrent"]"""
    private const val DESCRIPTION = "div.desc_big"
    private const val POSTER_URL =
        "#header_holder > table > tbody > tr:nth-child(2) > td > table.episode_columns_holder > tbody > tr:nth-child(1) > td.episode_left_column > table > tbody > tr:nth-child(2) > td > a:nth-child(1) > img"

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Ksoup.parse(html, pageUrl)

            val torrentName = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val seeders = html.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)?.ownText()?.toUIntOrNull()

            val torrentInfoContainer = html.selectFirst(TORRENT_INFO_CONTAINER)
            val size = torrentInfoContainer
                ?.find { it.ownText() == "Filesize:" }
                ?.nextSibling()
                .let { it as? TextNode }
                ?.text()
                ?.trim()
            val uploadDate = torrentInfoContainer
                ?.find { it.ownText() == "Released:" }
                ?.nextSibling()
                .let { it as? TextNode }
                ?.text()
                ?.trim()
                ?.replace(Regex("(\\d+)(st|nd|rd|th)"), "$1")
                ?.let { TorrentDateParser.parse(date = it, format = "d MMM yyyy") }
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
            val description = html.selectFirst(DESCRIPTION)?.html()
            val posterUrl = html.selectFirst(POSTER_URL)?.attr("abs:src")

            TorrentDetails(
                infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
                name = torrentName,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = Category.Series,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
                description = description,
                posterUrl = posterUrl,
            )
        }
}