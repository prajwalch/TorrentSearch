package com.prajwalch.torrentsearch.providers

import androidx.core.net.toUri

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.extension.asObject
import com.prajwalch.torrentsearch.extension.getArray
import com.prajwalch.torrentsearch.extension.getObject
import com.prajwalch.torrentsearch.extension.getString
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

import org.jsoup.Jsoup
import java.time.Instant

class SubsPlease : SearchProvider, LatestTorrentsProvider, TorrentDetailsProvider {
    override val id = "subsplease"
    override val name = "SubsPlease"
    override val url = "https://subsplease.org"
    override val supportedCategories = setOf(Category.Anime)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsJsonParser =
        SubsPleaseResultsJsonParser(providerName = name, providerUrl = url)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/api")
            append("?f=search")
            append("&tz=$")
            append("&s=$query")
        }
        val responseJson = context.httpClient.getJson(url = requestUrl) ?: return emptyList()

        return resultsJsonParser.parse(responseJson)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = "$url/api/?f=latest&tz=$"
        val responseJson = HttpClient.getJson(requestUrl) ?: return emptyList()

        return resultsJsonParser.parse(responseJson)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val responseHtml = HttpClient.get(detailsPageUrl)
        return SubsPleaseDetailsPageParser.parse(html = responseHtml, pageUrl = detailsPageUrl)
    }
}

private class SubsPleaseResultsJsonParser(
    private val providerName: String,
    private val providerUrl: String,
) {
    suspend fun parse(json: JsonElement): List<Torrent> = withContext(Dispatchers.Default) {
        json.asObject()
            .entries
            .mapNotNull { (showName, animeObject) ->
                parseAnimeObject(
                    showName = showName,
                    animeObject = animeObject.asObject(),
                )
            }
            .flatten()
    }

    private fun parseAnimeObject(showName: String, animeObject: JsonObject): List<Torrent>? {
        val uploadDate = animeObject.getString("release_date")?.let(TorrentDateParser::parseRFC1123)
        val detailsPageUrl = animeObject.getString("page")?.let { "$providerUrl/$it" }
        val episodeNumber = animeObject.getString("episode")!!

        return animeObject.getArray("downloads")?.mapNotNull {
            parseDownloadObject(
                downloadObject = it.asObject(),
                torrentName = showName,
                uploadDate = uploadDate,
                detailsPageUrl = detailsPageUrl,
                episodeNumber = episodeNumber,
            )
        }
    }

    private fun parseDownloadObject(
        downloadObject: JsonObject,
        torrentName: String,
        uploadDate: Instant?,
        detailsPageUrl: String?,
        episodeNumber: String,
    ): Torrent? {
        val magnetUri = downloadObject.getString("magnet") ?: return null
        val size = SubsPleaseUtils.parseSizeFromMagnetUri(magnetUri = magnetUri)

        val resolution = downloadObject.getString("res")!!
        val finalTorrentName = "$torrentName [${resolution}p]"

        val detailsPageUrl = detailsPageUrl?.let { "$it?ep=$episodeNumber&res=$resolution" }

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = finalTorrentName,
            size = size,
            uploadDate = uploadDate,
            category = Category.Anime,
            descriptionPageUrl = detailsPageUrl,
            magnetUri = magnetUri,
            providerName = providerName,
        )
    }
}

private object SubsPleaseDetailsPageParser {
    suspend fun parse(html: String, pageUrl: String): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val html = Jsoup.parse(html, pageUrl)

            val showId = html.selectFirst("table#show-release-table")
                ?.attr("sid")
                ?: return@withContext null
            val showName = html.selectFirst("h1.entry-title")?.ownText() ?: return@withContext null
            val posterUrl = html.selectFirst("img.img-responsive.img-center")?.attr("abs:src")
            val description = html.selectFirst("div.series-syn > p")?.ownText()

            val (episodeNumber, torrentResolution) = pageUrl.takeLastWhile { it != '?' }
                .split("&", limit = 2)
                .let { (episodeParam, resolutionParam) ->
                    Pair(
                        episodeParam.removePrefix("ep="),
                        resolutionParam.removePrefix("res="),
                    )
                }

            val (episodeObject, downloadObject) = getEpisodeAndDownloadObject(
                showId = showId,
                episodeNumber = episodeNumber,
                torrentResolution = torrentResolution
            ) ?: return@withContext null

            val torrentName = "$showName - $episodeNumber [${torrentResolution}p]"
            val magnetUri = downloadObject.getString("magnet") ?: return@withContext null
            val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
            val size = SubsPleaseUtils.parseSizeFromMagnetUri(magnetUri)
            val uploadDate = episodeObject.getString("time")?.let {
                TorrentDateParser.parse(date = it, format = "MM/dd/yy")
            }
            val fileDownloadLink = downloadObject.getString("torrent")

            TorrentDetails(
                infoHash = infoHash,
                name = torrentName,
                size = size,
                uploadDate = uploadDate,
                category = Category.Anime,
                magnetUri = magnetUri,
                fileDownloadLink = fileDownloadLink,
                description = description,
                posterUrl = posterUrl,
            )
        }

    suspend fun getEpisodeAndDownloadObject(
        showId: String,
        episodeNumber: String,
        torrentResolution: String,
    ): Pair<JsonObject, JsonObject>? = withContext(Dispatchers.IO) {
        val requestUrl = "https://subsplease.org/api/?f=show&tz=$&sid=$showId"
        val responseJson = HttpClient.getJson(requestUrl) ?: return@withContext null

        val episodeObject = responseJson.asObject()
            .getObject("episode")
            ?.values
            ?.map { it.asObject() }
            ?.find { it.getString("episode") == episodeNumber }
            ?: return@withContext null

        val downloadObject = episodeObject.getArray("downloads")
            ?.map { it.asObject() }
            ?.find { it.getString("res") == torrentResolution }
            ?: return@withContext null

        Pair(episodeObject, downloadObject)
    }
}

private object SubsPleaseUtils {
    fun parseSizeFromMagnetUri(magnetUri: MagnetUri): String? {
        val magnetUri = magnetUri.toUri()
        // android.net.Uri.getQueryParamater() is not supported for magnet URI.
        return magnetUri.query
            ?.split('&')
            ?.firstOrNull { it.startsWith("xl=") }
            ?.removePrefix("xl=")
            ?.let(FileSizeUtils::formatBytes)
    }
}