package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUriState
import com.prajwalch.torrentsearch.domain.model.SearchProviderSafety
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.provider.LatestTorrentsProvider
import com.prajwalch.torrentsearch.provider.MagnetUriProvider
import com.prajwalch.torrentsearch.provider.SearchProvider
import com.prajwalch.torrentsearch.provider.SearchProviderId
import com.prajwalch.torrentsearch.provider.TorrentDetailsProvider
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class FitGirlRepacks(private val networkClient: NetworkClient) :
    SearchProvider,
    LatestTorrentsProvider,
    MagnetUriProvider,
    TorrentDetailsProvider {
    override val id = "fitgirlrepacks"
    override val name = "FitGirl Repacks"
    override val url = "https://fitgirl-repacks.site"
    override val supportedCategories = setOf(Category.Games)
    override val safety = SearchProviderSafety.Safe
    override val enabledByDefault = false

    private val resultsPageParser = FitGirlRepacksResultsPageParser(id, name)

    override suspend fun search(query: String, category: Category): List<Torrent> {
        val requestUrl = "$url/?s=$query"
        val responseHtml = networkClient.getText(requestUrl)

        return resultsPageParser.parse(responseHtml, requestUrl)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val responseHtml = networkClient.getText(url)
        return resultsPageParser.parse(responseHtml, url)
    }

    override suspend fun getMagnetUri(sourceUrl: String): String {
        val detailsPageHtml = networkClient.getText(sourceUrl)
        return FitGirlRepacksDetailsPageParser.extractMagnetUri(detailsPageHtml)
            ?: error("Failed to retrieve magnet URI from '$sourceUrl'")
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = networkClient.getText(detailsPageUrl)
        return FitGirlRepacksDetailsPageParser.parse(responseHtml, detailsPageUrl)
    }
}

private class FitGirlRepacksResultsPageParser(
    private val providerId: SearchProviderId,
    private val providerName: String,
) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull(::parseListItem)
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)
            ?.attr("abs:href") ?: return null
        val gameName = listItem.selectFirst(GAME_NAME)?.ownText() ?: return null

        val remoteId = listItem.id().takeIf { it.isNotBlank() }
        val torrentId = TorrentUtils.createTorrentId(
            providerId = providerId,
            sourceId = remoteId ?: detailsPageUrl,
        )

        val uploadDate = listItem.selectFirst(UPLOAD_DATE)
            ?.attr("datetime")
            ?.let(TorrentDateParser::parseIso)
        val magnetUri = listItem.selectFirst(MAGNET_URI)
            ?.attr("href")
            ?.let { MagnetUriState.Available(it) }
            ?: MagnetUriState.FetchRequired(detailsPageUrl)

        return Torrent(
            id = torrentId,
            name = gameName,
            uploadDate = uploadDate,
            category = Category.Games,
            magnetUriState = magnetUri,
            providerName = providerName,
            descriptionPageUrl = detailsPageUrl,
        )
    }

    companion object {
        private const val LIST_ITEM = "article.category-lossless-repack"
        private const val GAME_NAME = "header > h1.entry-title > a"
        private const val UPLOAD_DATE = "header > div.entry-meta > span.entry-date > a > time"
        private const val MAGNET_URI = """div.entry-content a[href^="magnet:?xt="]"""
        private const val DETAILS_PAGE_URL = GAME_NAME
    }
}

private object FitGirlRepacksDetailsPageParser {
    private const val GAME_NAME = "h1.entry-title"
    private const val UPLOAD_DATE = "time.entry-date"
    private const val UPLOADER = "span.author > a"
    private const val DESCRIPTION = "div.entry-content"
    private const val POSTER = "div.entry-content > p:nth-child(2) > a:nth-child(1) > img"
    private const val SCREENSHOTS_TITLE =
        "div.entry-content > h3:containsOwn(Screenshots (Click to enlarge))"
    private const val MAGNET_URI = """a[href^="magnet:?xt="]"""

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val dom = Jsoup.parse(html, pageUrl)

            val magnetUri = dom.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val gameName = dom.selectFirst(GAME_NAME)?.ownText() ?: return@withContext null
            val uploadDate = dom.selectFirst(UPLOAD_DATE)
                ?.attr("datetime")
                ?.let(TorrentDateParser::parseIso)
            val uploader = dom.selectFirst(UPLOADER)?.ownText()
            val description = dom.selectFirst(DESCRIPTION)?.html()
            val posterUrl = dom.selectFirst(POSTER)?.attr("abs:src")
            val screenshotUrls = dom.selectFirst(SCREENSHOTS_TITLE)
                ?.nextElementSibling()
                ?.select("img")
                ?.map { it.attr("src") }
                .orEmpty()

            TorrentDetails(
                infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
                name = gameName,
                uploadDate = uploadDate,
                uploader = uploader,
                category = Category.Games,
                description = description,
                magnetUri = magnetUri,
                posterUrl = posterUrl,
                screenshotUrls = screenshotUrls,
            )
        }

    suspend fun extractMagnetUri(detailsPageHtml: String): String? =
        withContext(Dispatchers.Default) {
            Jsoup.parse(detailsPageHtml).selectFirst(MAGNET_URI)?.attr("href")
        }
}