package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.utils.DateUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class TorrentDownloads : SearchProvider {
    override val info = SearchProviderInfo(
        id = "torrentdownloads",
        name = "TorrentDownloads",
        url = "https://torrentdownloads.pro",
        specializedCategory = Category.All,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = true,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(info.url)
            append("/search")

            val categoryId = getCategoryId(category = context.category)
            append("/?s_cat=$categoryId")
            append("&search=$query")
        }

        val responseHtml = context.httpClient.get(url = requestUrl)
        val torrents = withContext(Dispatchers.Default) {
            parseHtml(html = responseHtml, httpClient = context.httpClient)
        }

        return torrents.orEmpty()
    }

    private fun getCategoryId(category: Category) = when (category) {
        Category.All -> 0
        Category.Anime -> 1
        Category.Apps -> 7
        Category.Books -> 2
        Category.Games -> 3
        Category.Movies -> 4
        Category.Music -> 5
        Category.Series -> 8
        Category.Porn, Category.Other -> 9
    }

    private suspend fun parseHtml(html: String, httpClient: HttpClient): List<Torrent>? {
        return Jsoup
            .parse(html)
            .select("div.inner_container")
            .last()
            ?.select("div.grey_bar3")
            ?.drop(2)
            ?.mapNotNull { div -> parseRowDiv(div = div, httpClient = httpClient) }
    }

    private suspend fun parseRowDiv(div: Element, httpClient: HttpClient): Torrent? {
        val torrentNameAnchor = div.selectFirst("p > a") ?: return null
        val torrentName = torrentNameAnchor.ownText()
        val descriptionPageUrl = info.url + torrentNameAnchor.attr("href")

        val categoryIconUrl = div.selectFirst("p > img")?.attr("src") ?: return null
        val category = parseCategory(categoryIconUrl = categoryIconUrl)

        val size = div.selectFirst("span:nth-child(5)")?.ownText() ?: return null
        val seeders = div.selectFirst("span:nth-child(4)")?.ownText() ?: return null
        val peers = div.selectFirst("span:nth-child(3)")?.ownText() ?: return null

        val (uploadDate, magnetUri) = withContext(Dispatchers.IO) {
            getUploadDateAndMagnetUri(
                httpClient = httpClient,
                descriptionPageUrl = descriptionPageUrl,
            )
        } ?: return null

        return Torrent(
            name = torrentName,
            size = size,
            seeders = seeders.toUIntOrNull() ?: 0u,
            peers = peers.toUIntOrNull() ?: 0u,
            providerName = info.name,
            uploadDate = uploadDate,
            category = category,
            descriptionPageUrl = descriptionPageUrl,
            infoHashOrMagnetUri = InfoHashOrMagnetUri.MagnetUri(magnetUri),
        )
    }

    private fun parseCategory(categoryIconUrl: String) = when {
        categoryIconUrl.endsWith("menu_icon0.png") -> Category.All
        categoryIconUrl.endsWith("menu_icon1.png") -> Category.Anime
        categoryIconUrl.endsWith("menu_icon2.png") -> Category.Books
        categoryIconUrl.endsWith("menu_icon3.png") -> Category.Games
        categoryIconUrl.endsWith("menu_icon4.png") -> Category.Movies
        categoryIconUrl.endsWith("menu_icon5.png") -> Category.Music
        categoryIconUrl.endsWith("menu_icon7.png") -> Category.Apps
        categoryIconUrl.endsWith("menu_icon8.png") -> Category.Series
        categoryIconUrl.endsWith("menu_icon9.png") -> Category.Other
        else -> Category.Other
    }

    private suspend fun getUploadDateAndMagnetUri(
        httpClient: HttpClient,
        descriptionPageUrl: String,
    ): Pair<String, String>? {
        val responseHtml = httpClient.get(url = descriptionPageUrl)
        val innerContainer = Jsoup
            .parse(responseHtml)
            .selectFirst("div.inner_container")
            ?: return null

        val greyBars = innerContainer.select("div.grey_bar1")

        val magnetUri = greyBars.getOrNull(3)
            ?.selectFirst("a")
            ?.attr("href")
            ?: return null

        val uploadDate = greyBars.getOrNull(6)
            ?.selectFirst("p")
            ?.ownText()
            ?.split(' ')
            ?.first()
            ?.let { DateUtils.formatYearMonthDay(it) }
            ?: return null

        return Pair(uploadDate, magnetUri)
    }
}