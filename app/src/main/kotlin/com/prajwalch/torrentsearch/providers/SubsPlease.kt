package com.prajwalch.torrentsearch.providers

import androidx.core.net.toUri

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.extension.asObject
import com.prajwalch.torrentsearch.extension.getArray
import com.prajwalch.torrentsearch.extension.getString
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

import java.time.Instant

class SubsPlease : SearchProvider, LatestTorrentsProvider {
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

        return animeObject.getArray("downloads")?.mapNotNull {
            parseDownloadObject(
                downloadObject = it.asObject(),
                torrentName = showName,
                uploadDate = uploadDate,
                detailsPageUrl = detailsPageUrl,
            )
        }
    }

    private fun parseDownloadObject(
        downloadObject: JsonObject,
        torrentName: String,
        uploadDate: Instant?,
        detailsPageUrl: String?,
    ): Torrent? {
        val magnetUri = downloadObject.getString("magnet") ?: return null
        val size = parseSizeFromMagnetUri(magnetUri = magnetUri)

        val resolution = downloadObject.getString("res")?.let { "${it}p" }
        val finalTorrentName = if (resolution == null) torrentName else "$torrentName [$resolution]"

        return Torrent(
            infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri),
            name = finalTorrentName,
            size = size ?: "0 KB",
            seeders = 0U,
            peers = 0U,
            uploadDate = uploadDate,
            category = Category.Anime,
            descriptionPageUrl = detailsPageUrl ?: "",
            magnetUri = magnetUri,
            providerName = providerName,
        )
    }

    private fun parseSizeFromMagnetUri(magnetUri: MagnetUri): String? {
        val magnetUri = magnetUri.toUri()
        // android.net.Uri.getQueryParamater() is not supported for magnet URI.
        return magnetUri
            .query
            ?.split('&')
            ?.firstOrNull { it.startsWith("xl=") }
            ?.removePrefix("xl=")
            ?.let(FileSizeUtils::formatBytes)
    }
}