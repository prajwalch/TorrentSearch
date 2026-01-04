package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.domain.models.Torrent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class TorrentDownload : SearchProvider {
    override val info = SearchProviderInfo(
        id = "torrentdownloadinfo",
        name = "TorrentDownload",
        url = "https://torrentdownload.info",
        specializedCategory = Category.All,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(info.url)
            append("/search")
            append("?q=$query")
        }

        val responseHtml = context.httpClient.get(url = requestUrl)
        val torrents = withContext(Dispatchers.Default) {
            parseResponseHtml(html = responseHtml)
        }

        return torrents.orEmpty()
    }

    private fun parseResponseHtml(html: String): List<Torrent>? {
        return Jsoup
            .parse(html)
            .select("table.table2")
            .last()
            ?.select("tbody > tr")
            ?.mapNotNull(::parseTableRow)
    }

    private fun parseTableRow(tr: Element): Torrent? {
        val torrentNameDiv = tr.selectFirst("td:nth-child(1) > div.tt-name") ?: return null

        val nameHref = torrentNameDiv.selectFirst("> a") ?: return null
        val torrentName = nameHref.text()

        val category = torrentNameDiv
            .selectFirst("> span")
            ?.ownText()
            // strip everything but letters
            ?.replace(regex = CategoryTextFilterRegex, "")
            ?.let(::getCategory)
            ?: return null

        val descriptionPageRelativeUrl = nameHref.attr("href")
        val descriptionPageUrl = "${info.url}$descriptionPageRelativeUrl"
        val infoHash = descriptionPageRelativeUrl
            .split('/')
            .getOrNull(1)
            ?.let(InfoHashOrMagnetUri::InfoHash)
            ?: return null

        val uploadDate = tr.selectFirst("td:nth-child(2)")?.ownText() ?: return null
        val size = tr.selectFirst("td:nth-child(3)")?.ownText() ?: return null
        val seeders = tr.selectFirst("td:nth-child(4)")?.ownText()?.replace(",", "") ?: return null
        val peers = tr.selectFirst("td:nth-child(5)")?.ownText()?.replace(",", "") ?: return null

        return Torrent(
            name = torrentName,
            size = size,
            seeders = seeders.toUIntOrNull() ?: 0U,
            peers = peers.toUIntOrNull() ?: 0U,
            uploadDate = uploadDate,
            category = category,
            providerName = info.name,
            descriptionPageUrl = descriptionPageUrl,
            infoHashOrMagnetUri = infoHash,
        )
    }

    private fun getCategory(string: String) = when (string) {
        "XXX",
        "XXXVideo",
        "XXXHDVideo",
        "XXXPictures",
        "Adult",
        "AdultPornHDVideo",
        "AdultPornPictures",
        "AdultPornVideo",
            -> Category.Porn

        "Anime",
        "AnimeEnglishtranslated",
        "AnimeAnimeOther",
            -> Category.Anime

        "Applications",
        "ApplicationsAndroid",
        "ApplicationsWindows",
        "Software",
            -> Category.Apps

        "BooksAcademic",
        "BooksComics",
        "BooksEbooks",
        "BooksEducational",
        "BooksMagazines",
        "BooksFiction",
        "BooksNonfiction",
        "BooksTextbooks",
        "Ebooks",
        "OtherEbooks",
        "OtherComics",
        "AudioBooks",
        "AudioAudiobooks",
            -> Category.Books

        "Games", "GamesWindows" -> Category.Games

        "Movies",
        "MoviesAction",
        "MoviesConcerts",
        "MoviesCrime",
        "MoviesDocumentary",
        "MoviesDubbedMovies",
        "MoviesHighresMovies",
        "MoviesMusicvideos",
        "MoviesThriller",
        "VideoMovies",
            -> Category.Movies

        "Music",
        "MusicHardrock",
        "MusicMp",
        "MusicFLAC",
        "MusicLossless",
        "MusicRB",
        "MusicTranceHouseDance",
        "VideoMusic",
        "AudioMusic",
            -> Category.Music

        "TV",
        "TVBBC",
        "TVshows",
        "Television",
            -> Category.Series

        else -> Category.Other
    }

    private companion object {
        private val CategoryTextFilterRegex = Regex("[^A-Za-z]+")
    }
}