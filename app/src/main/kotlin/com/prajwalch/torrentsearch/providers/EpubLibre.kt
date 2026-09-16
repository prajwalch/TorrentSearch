package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.domain.model.SearchProviderSafety
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.extension.asObject
import com.prajwalch.torrentsearch.extension.getString
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.provider.LatestTorrentsProvider
import com.prajwalch.torrentsearch.provider.MagnetUriProvider
import com.prajwalch.torrentsearch.provider.SearchProvider
import com.prajwalch.torrentsearch.provider.SearchProviderId
import com.prajwalch.torrentsearch.provider.TopTorrentsProvider
import com.prajwalch.torrentsearch.provider.TorrentDetailsProvider
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class EpubLibre(private val networkClient: NetworkClient) :
    SearchProvider,
    LatestTorrentsProvider,
    TopTorrentsProvider,
    MagnetUriProvider,
    TorrentDetailsProvider {
    override val id = "epublibre"
    override val name = "EpubLibre"
    override val url = "https://epublibre.org"
    override val supportedCategories = setOf(Category.Books)
    override val safety = SearchProviderSafety.Safe
    override val enabledByDefault = false

    private val resultsPageParser = EpubLibreResultsPageParser(id, name)

    override suspend fun search(query: String, category: Category): List<Torrent> {
        val requestUrl = "$url/catalogo/index/0/nuevo/todos/sin/todos/$query/ajax"
        val responseJson = networkClient.postJson(
            url = requestUrl,
            headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
        ) ?: return emptyList()

        return resultsPageParser.parseJson(responseJson)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/catalogo/index/0/nuevo/novedades/sin/todos/--/ajax"
        val responseJson = networkClient.postJson(
            url = requestUrl,
            headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
        ) ?: return emptyList()

        return resultsPageParser.parseJson(responseJson)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/catalogo/index/0/valorado/novedades/sin/todos/--/ajax"
        val responseJson = networkClient.postJson(
            url = requestUrl,
            headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
        ) ?: return emptyList()

        return resultsPageParser.parseJson(responseJson)
    }

    override suspend fun getMagnetUri(url: String): String {
        val detailsPageHtml = networkClient.getText(url)
        return EpubLibreDetailsPageParser.extractMagnetUri(detailsPageHtml)
            ?: error("Failed to retrieve magnet URI from '$url'")
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val detailsPageHtml = networkClient.getText(detailsPageUrl)
        return EpubLibreDetailsPageParser.parse(detailsPageHtml)
    }
}

private class EpubLibreResultsPageParser(
    private val providerId: SearchProviderId,
    private val providerName: String,
) {
    suspend fun parseJson(json: JsonElement): List<Torrent> = withContext(Dispatchers.Default) {
        val html = json.asObject().getString("contenido") ?: return@withContext emptyList()
        Jsoup.parse(html)
            .select(LIST_ITEM)
            .mapNotNull(::parseListItem)
    }

    private fun parseListItem(listItem: Element): Torrent? {
        val bookName = listItem.selectFirst(BOOK_NAME)?.ownText() ?: return null
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("href") ?: return null

        val bookRemoteId = detailsPageUrl.takeLastWhile { it != '/' }.takeIf { it.isNotBlank() }
        val torrentId = TorrentUtils.createTorrentId(
            providerId = providerId,
            sourceId = bookRemoteId ?: detailsPageUrl,
        )
        val torrentName = buildString {
            append(bookName)

            val authorName = listItem.selectFirst(BOOK_AUTHOR)?.ownText()
            authorName?.let {
                append(" - ")
                append(it)
            }
        }

        return Torrent(
            id = torrentId,
            name = torrentName,
            providerName = providerName,
            category = Category.Books,
            magnetUri = MagnetUri.RequiresFetch(detailsPageUrl),
            descriptionPageUrl = detailsPageUrl,
        )
    }

    private companion object {
        private const val LIST_ITEM = "div.span2"
        private const val BOOK_NAME = "a#stk > div.texto-portada > h1"
        private const val BOOK_AUTHOR = "a#stk > div.texto-portada > h2"
        private const val DETAILS_PAGE_URL = "a#stk"
    }
}

private object EpubLibreDetailsPageParser {
    private const val BOOK_NAME = "div#titulo_libro"
    private const val POSTER_URL = "img#portada"
    private const val DESCRIPTION = "div.detalle"
    private const val MAGNET_URI = "a#en_desc"

    suspend fun parse(html: String): TorrentDetails? = withContext(Dispatchers.Default) {
        val dom = Jsoup.parse(html)
        val bookName = dom.selectFirst(BOOK_NAME)?.text() ?: return@withContext null
        val magnetUri = dom.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
        val posterUrl = dom.selectFirst(POSTER_URL)?.attr("src")
        val description = dom.selectFirst(DESCRIPTION)?.html()

        TorrentDetails(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = bookName,
            category = Category.Books,
            magnetUri = magnetUri,
            posterUrl = posterUrl,
            description = description,
        )
    }

    suspend fun extractMagnetUri(detailsPageHtml: String): String? =
        withContext(Dispatchers.Default) {
            Jsoup.parse(detailsPageHtml).selectFirst(MAGNET_URI)?.attr("href")
        }
}