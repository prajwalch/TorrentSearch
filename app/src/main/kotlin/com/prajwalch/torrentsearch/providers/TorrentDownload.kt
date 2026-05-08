package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class TorrentDownload : SearchProvider, TorrentDetailsProvider {
    override val id = "torrentdownloadinfo"
    override val name = "TorrentDownload"
    override val url = "https://torrentdownload.info"
    override val supportedCategories = setOf(
        Category.Anime,
        Category.Apps,
        Category.Books,
        Category.Games,
        Category.Movies,
        Category.Music,
        Category.Porn,
        Category.Series,
        Category.Other,
    )
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsPageParser = TorrentDownloadResultsParser(name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/search")
            append("?q=$query")
        }

        val responseHtml = context.httpClient.get(url = requestUrl)
        return resultsPageParser.parse(html = responseHtml, pageUrl = requestUrl)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return TorrentDownloadDetailsPageParser.parse(responseHtml)
    }
}

private class TorrentDownloadResultsParser(private val providerName: String) {
    suspend fun parse(html: String, pageUrl: String): List<Torrent> =
        withContext(Dispatchers.Default) {
            Jsoup.parse(html, pageUrl)
                .select(LIST_ITEM)
                .mapNotNull(::parseListItem)
        }

    private fun parseListItem(listItem: Element): Torrent? {
        val torrentName = listItem.selectFirst(TORRENT_NAME)?.ownText() ?: return null
        val detailsPageUrl = listItem.selectFirst(DETAILS_PAGE_URL)?.attr("abs:href") ?: return null
        val infoHash = detailsPageUrl
            .dropLastWhile { it != '/' }
            .takeLastWhile { it != '/' }
            .trim()
            .lowercase()
        val size = listItem.selectFirst(SIZE)?.ownText()
        val seeders = listItem.selectFirst(SEEDERS)?.ownText()?.replace(",", "")?.toUIntOrNull()
        val peers = listItem.selectFirst(PEERS)?.ownText()?.replace(",", "")?.toUIntOrNull()
        val uploadDate = listItem.selectFirst(UPLOAD_DATE)
            ?.ownText()
            ?.let(TorrentDateParser::tryParseRelative)
        val category = listItem.selectFirst(CATEGORY)
            ?.ownText()
            ?.let(::categoryFromRawString)

        return Torrent(
            infoHash = infoHash,
            name = torrentName,
            size = size ?: "0 KB",
            seeders = seeders ?: 0U,
            peers = peers ?: 0U,
            uploadDate = uploadDate,
            category = category,
            providerName = providerName,
            descriptionPageUrl = detailsPageUrl,
        )
    }

    private companion object {
        private const val LIST_ITEM = "div.wrapper > table.table2:nth-of-type(2) > tbody > tr"
        private const val TORRENT_NAME = "td:nth-child(1) > div.tt-name > a"
        private const val SIZE = "td:nth-child(3)"
        private const val SEEDERS = "td:nth-child(4)"
        private const val PEERS = "td:nth-child(5)"
        private const val UPLOAD_DATE = "td:nth-child(2)"
        private const val CATEGORY = "td:nth-child(1) > div.tt-name > span"
        private const val DETAILS_PAGE_URL = TORRENT_NAME
    }
}

private object TorrentDownloadDetailsPageParser {
    private const val TORRENT_INFO_TABLE_BODY = "table.torrentinfo > tbody"
    private const val TORRENT_NAME = "$TORRENT_INFO_TABLE_BODY > tr:nth-child(1) > td:nth-child(2)"
    private const val SIZE = "$TORRENT_INFO_TABLE_BODY > tr:nth-child(6) > td:nth-child(2)"
    private const val SEEDERS =
        "$TORRENT_INFO_TABLE_BODY > tr:nth-child(5) > td:nth-child(2) > span:nth-child(1)"
    private const val PEERS =
        "$TORRENT_INFO_TABLE_BODY > tr:nth-child(5) > td:nth-child(2) > span:nth-child(2)"
    private const val UPLOAD_DATE = "$TORRENT_INFO_TABLE_BODY > tr:nth-child(8) > td:nth-child(2)"
    private const val CATEGORY = "$TORRENT_INFO_TABLE_BODY > tr:nth-child(4) > td:nth-child(2)"
    private const val MAGNET_URI = """a[href^="magnet:?"]"""
    private const val FILE_DOWNLOAD_LINK = """a[href^="https://itorrent.net/"]"""

    suspend fun parse(html: String): TorrentDetails? = withContext(Dispatchers.Default) {
        val html = Jsoup.parse(html)

        val torrentName = html.selectFirst(TORRENT_NAME)?.ownText() ?: return@withContext null
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
        val uploadDate = html.selectFirst(UPLOAD_DATE)
            ?.ownText()
            ?.let { TorrentDateParser.parse(date = it, format = "d MMMM yyyy") }
        val category = html.selectFirst(CATEGORY)?.ownText()?.let(::categoryFromRawString)
        val fileDownloadLink = html.selectFirst(FILE_DOWNLOAD_LINK)?.attr("href")

        TorrentDetails(
            infoHash = infoHash,
            name = torrentName,
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

private fun categoryFromRawString(raw: String): Category {
    val raw = raw.replace(regex = Regex("[^A-Za-z]+"), "")

    return when (raw) {
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
}