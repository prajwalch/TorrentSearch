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

class TorrentDownload : SearchProvider {
    override val info = SearchProviderInfo(
        id = "torrentdownloadinfo",
        name = "TorrentDownload",
        url = "https://torrentdownload.info",
        specializedCategory = Category.All,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
    )

    private val resultsPageParser = TorrentDownloadResultsParser(info.name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(info.url)
            append("/search")
            append("?q=$query")
        }

        val responseHtml = context.httpClient.get(url = requestUrl)
        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl).orEmpty()
    }

    override suspend fun getDetails(detailsPageUrl: String): GetTorrentDetailsResponse {
        val responseHtml = HttpClient.get(detailsPageUrl)
        
        return TorrentDownloadDetailsPageParser.parse(responseHtml)
            ?.let(GetTorrentDetailsResponse::Success)
            ?: GetTorrentDetailsResponse.DetailsNotFound
    }
}

private class TorrentDownloadResultsParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent>? =
        withContext(Dispatchers.Default) {
            Jsoup
                .parse(html, pageUrl)
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

        val descriptionPageUrl = nameHref.attr("abs:href")
        val infoHash = nameHref
            .attr("href")
            .removePrefix("/")
            .takeWhile { it != '/' }
            .trim()
            .lowercase()

        val uploadDate = tr.selectFirst("td:nth-child(2)")?.ownText() ?: return null
        val size = tr.selectFirst("td:nth-child(3)")?.ownText() ?: return null
        val seeders = tr.selectFirst("td:nth-child(4)")?.ownText()?.replace(",", "") ?: return null
        val peers = tr.selectFirst("td:nth-child(5)")?.ownText()?.replace(",", "") ?: return null

        return Torrent(
            infoHash = infoHash,
            name = torrentName,
            size = size,
            seeders = seeders.toUIntOrNull() ?: 0U,
            peers = peers.toUIntOrNull() ?: 0U,
            uploadDate = uploadDate,
            category = category,
            providerName = providerName,
            descriptionPageUrl = descriptionPageUrl,
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

private object TorrentDownloadDetailsPageParser {
    private const val NAME =
        "body > div > table.table3.torrentinfo > tbody > tr:nth-child(1) > td:nth-child(2)"
    private const val SIZE =
        "body > div > table.table3.torrentinfo > tbody > tr:nth-child(6) > td:nth-child(2)"
    private const val SEEDERS =
        "body > div > table.table3.torrentinfo > tbody > tr:nth-child(5) > td:nth-child(2) > span:nth-child(1)"
    private const val PEERS =
        "body > div > table.table3.torrentinfo > tbody > tr:nth-child(5) > td:nth-child(2) > span:nth-child(2)"
    private const val UPLOAD_DATE =
        "body > div > table.table3.torrentinfo > tbody > tr:nth-child(8) > td:nth-child(2)"
    private const val CATEGORY =
        "body > div > table.table3.torrentinfo > tbody > tr:nth-child(4) > td:nth-child(2)"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""
    private const val FILE_DOWNLOAD_LINK =
        "body > div > table:nth-child(7) > tbody > tr:nth-child(3) > td > span > a"

    suspend fun parse(html: String): TorrentDetails? = withContext(Dispatchers.Default) {
        val html = Jsoup.parse(html)

        val name = html.selectFirst(NAME)?.ownText() ?: return@withContext null
        val magnetUri = html.selectFirst(MAGNET_URI)?.attr("href") ?: return@withContext null
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
        val size = html.selectFirst(SIZE)?.ownText()
        val seeders = html.selectFirst(SEEDERS)
            ?.ownText()
            ?.removePrefix("Seeds: ")
            ?.trim()
            ?.toUIntOrNull()
        val peers = html.selectFirst(PEERS)
            ?.ownText()
            ?.removePrefix("Leechers: ")
            ?.trim()
            ?.toUIntOrNull()
        val uploadDate = html.selectFirst(UPLOAD_DATE)?.ownText()
        val category = html.selectFirst(CATEGORY)?.ownText()
        val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("href")

        TorrentDetails(
            infoHash = infoHash,
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            uploadDate = uploadDate,
            category = category,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
    }
}