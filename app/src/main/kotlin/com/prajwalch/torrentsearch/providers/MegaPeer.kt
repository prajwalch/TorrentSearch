package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class MegaPeer : SearchProvider, TorrentDetailsProvider {
    override val id = "megapeer"
    override val name = "MegaPeer"
    override val url = "https://megapeer.vip"
    override val supportedCategories = setOf(
        Category.Apps,
        Category.Books,
        Category.Games,
        Category.Movies,
        Category.Music,
        Category.Other,
        Category.Series,
    )
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val categoryMap = mapOf(
        Category.All to 0,
        Category.Apps to 29,
        Category.Books to 52,
        Category.Games to 28,
        Category.Movies to 80,
        Category.Music to 94,
        Category.Other to 59,
        Category.Series to 6,
    )
    private val resultsPageParser = MegaPeerResultsPageParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val categoryId = categoryMap[context.category] ?: 0
        val requestUrl =
            "$url/browse.php?search=$query&age=&cat=$categoryId&stype=0&sort=0&ascdesc=0"
        val responseHtml = context.httpClient.get(requestUrl)

        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return MegaPeersDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }
}

private class MegaPeerResultsPageParser(private val providerName: String) {
    private companion object {
        private const val LIST_ITEM = "div#index > table > tbody > tr.table_fon"
        private const val TORRENT_NAME = "td:nth-child(2) > a:nth-child(2)"
        private const val SIZE = "td:nth-child(3)"
        private const val SEEDERS = "td:nth-child(4) > font:nth-child(2)"
        private const val PEERS = "td:nth-child(4) > font:nth-child(4)"
        private const val FILE_DOWNLOAD_LINK = "td:nth-child(2) > a:nth-child(1)"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
        private const val UPLOAD_DATE = "td:nth-child(1)"
    }

    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .map { async { parseListItem(it) } }
                .awaitAll()
                .filterNotNull()
        }

    private suspend fun parseListItem(listItem: Element): Torrent? {
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href") ?: return null
        val magnetUri = MegaPeersDetailsPageParser.getMagnetUri(detailsPageUrl) ?: return null

        val torrentName = listItem.selectFirst(TORRENT_NAME)?.ownText() ?: return null
        val size = listItem.selectFirst(SIZE)?.ownText()
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.toUIntOrNull()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)
            ?.ownText()
            ?.replace("Мая", "Май")
            ?.let { runCatching { TorrentDateParser.convertRussianMonthToEnglish(it) } }
            ?.getOrNull()
            ?.let { TorrentDateParser.parse(date = it, format = "d MMM yy") }
        val fileDownloadLink = listItem.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = torrentName,
            size = size,
            seeders = seeders,
            peers = peers,
            uploadDate = uploadDate,
            providerName = providerName,
            fileDownloadLink = fileDownloadLink,
            descriptionPageUrl = detailsPageUrl,
        )
    }
}

private object MegaPeersDetailsPageParser {
    private const val TORRENT_NAME = "h1"
    private const val SIZE = "td:containsOwn(Размер)"
    private const val SEEDERS = "td:containsOwn(Раздают)"
    private const val PEERS = "td:containsOwn(Качают)"
    private const val CATEGORY = "td:containsOwn(Категория)"
    private const val MAGNET_URI = """a[href^="magnet:?xt="]"""
    private const val FILE_DOWNLOAD_LINK = """a[href^="/download/"]"""
    private const val DESCRIPTION = "table#details > tbody > tr:nth-child(1) > td:nth-child(2)"
    private const val POSTER_URL = "table#details > tbody > tr:nth-child(1) > td:nth-child(2) > img"
    //    private const val UPLOAD_DATE = "td:containsOwn(Добавлен)"
    //    private const val LAST_CHECKED = "td:containsOwn(Сид был)"

    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val torrentName = html.selectFirst(TORRENT_NAME)?.text() ?: return@withContext null
            val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
            val size = html.selectFirst(SIZE)
                ?.nextElementSibling()
                ?.ownText()?.takeWhile { it != '(' }?.trim()
            val seeders = html.selectFirst(SEEDERS)
                ?.nextElementSibling()
                ?.ownText()
                ?.toUIntOrNull()
            val peers = html.selectFirst(PEERS)
                ?.nextElementSibling()
                ?.ownText()
                ?.toUIntOrNull()
            val category = html.selectFirst(CATEGORY)
                ?.nextElementSibling()
                ?.selectFirst("a")
                ?.attr("href")
                ?.removePrefix("/cat/")
                ?.let(::getCategoryFromId)
            /*
            Date parsing doesn't work with Russian months.
            val uploadDate = html.selectFirst(UPLOAD_DATE)
                ?.nextElementSibling()
                ?.ownText()
                ?.substringBefore("в")
                ?.trim()
                ?.runCatching { TorrentDateParser.parse(date = this, format = "d MMMM yyyy") }
                ?.getOrNull()
            val lastChecked = html.selectFirst(LAST_CHECKED)
                ?.nextElementSibling()
                ?.ownText()
                ?.substringBefore("в")
                ?.trim()
                ?.runCatching { TorrentDateParser.parse(date = this, format = "d MMMM yyyy") }
                ?.getOrNull()
             */
            val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("abs:href")
            val posterUrl = html.selectFirst(POSTER_URL)?.attr("src")
            val descriptionElement = html.selectFirst(DESCRIPTION)
            val screenshotUrls = descriptionElement?.let(::extractScreenshots)
            val description = descriptionElement
                ?.apply {
                    // Remove poster
                    selectFirst("img")?.remove()
                    // Remove screenshots and unsupported tags
                    select("div.sp-wrap").remove()
                }
                ?.html()

            TorrentDetails(
                infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
                name = torrentName,
                size = size,
                seeders = seeders,
                peers = peers,
//                uploadDate = uploadDate,
                category = category,
//                lastChecked = lastChecked,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
                description = description,
                posterUrl = posterUrl,
                screenshotUrls = screenshotUrls.orEmpty(),
            )
        }

    private fun extractScreenshots(description: Element): List<String>? {
        return description.selectFirst("div.sp-head:containsOwn(Скриншоты)")
            ?.nextElementSibling()
            ?.select("img.lazy-screenshot")
            ?.mapNotNull { it.attr("data-src").takeIf { url -> url.isNotBlank() } }
    }

    private fun getCategoryFromId(id: String): Category = when (id) {
        "80", "79" -> Category.Movies
        "5", "6", "55", "21" -> Category.Series
        "94" -> Category.Music
        "28" -> Category.Games
        "29" -> Category.Apps
        "52" -> Category.Books
        else -> Category.Other
    }

    suspend fun getMagnetUri(detailsPageUrl: String): String? {
        val detailsPageHtml = withContext(Dispatchers.IO) { HttpClient.get(detailsPageUrl) }
        return withContext(Dispatchers.Default) {
            Jsoup.parse(detailsPageHtml).selectFirst(MAGNET_URI)?.attr("href")
        }
    }
}