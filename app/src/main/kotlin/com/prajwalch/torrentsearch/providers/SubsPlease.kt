package com.prajwalch.torrentsearch.providers

import androidx.core.net.toUri

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.extension.asObject
import com.prajwalch.torrentsearch.extension.getArray
import com.prajwalch.torrentsearch.extension.getString
import com.prajwalch.torrentsearch.util.DateUtils
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

class SubsPlease : SearchProvider {
    override val info = SearchProviderInfo(
        id = "subsplease",
        name = "SubsPlease",
        url = "https://subsplease.org",
        specializedCategory = Category.Anime,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(info.url)
            append("/api")
            append("?f=search")
            append("&tz=$")
            append("&s=$query")
        }
        val responseJson = context.httpClient.getJson(url = requestUrl) ?: return emptyList()

        return withContext(Dispatchers.Default) {
            try {
                parseResponseObject(responseObject = responseJson.asObject())
            } catch (_: IllegalArgumentException) {
                emptyList()
            }
        }
    }

    private fun parseResponseObject(responseObject: JsonObject): List<Torrent> {
        return responseObject
            .values
            .mapNotNull { parseAnimeObject(animeObject = it.asObject()) }
            .flatten()
    }

    private fun parseAnimeObject(animeObject: JsonObject): List<Torrent>? {
        val name = animeObject.getString("show") ?: return null
        val uploadDate = animeObject
            .getString("release_date")
            ?.let(DateUtils::formatRFC1123Date)
            ?: return null
        val descriptionPageUrl = animeObject
            .getString("page")
            ?.let { "${info.url}/$it" }
            ?: return null

        return animeObject.getArray("downloads")
            ?.map { it.asObject() }
            ?.mapNotNull { parseDownloadObject(it) }
            ?.map { downloadLink ->
                Torrent(
                    infoHash = TorrentUtils.getInfoHashFromMagnetUri(downloadLink.magnetUri),
                    name = "$name (${downloadLink.resolution})",
                    size = downloadLink.size,
                    seeders = 1U,
                    peers = 1U,
                    uploadDate = uploadDate,
                    category = info.specializedCategory,
                    descriptionPageUrl = descriptionPageUrl,
                    magnetUri = downloadLink.magnetUri,
                    providerName = info.name,
                )
            }
    }

    private fun parseDownloadObject(downloadObject: JsonObject): DownloadLink? {
        val resolution = downloadObject.getString("res")?.let { "${it}p" } ?: return null
        val magnetUri = downloadObject.getString("magnet") ?: return null
        val size = parseSizeFromMagnetUri(magnetUri = magnetUri) ?: return null

        return DownloadLink(resolution = resolution, size = size, magnetUri = magnetUri)
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

private data class DownloadLink(
    val resolution: String,
    val size: String,
    val magnetUri: MagnetUri,
)