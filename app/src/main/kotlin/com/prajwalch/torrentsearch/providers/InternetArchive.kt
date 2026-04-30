package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.GetTorrentDetailsResponse
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.extension.asObject
import com.prajwalch.torrentsearch.extension.getArray
import com.prajwalch.torrentsearch.extension.getLong
import com.prajwalch.torrentsearch.extension.getObject
import com.prajwalch.torrentsearch.extension.getString
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.DateUtils
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

class InternetArchive : SearchProvider {
    override val id = "internetarchive"
    override val name = "InternetArchive"
    override val url = "https://archive.org"
    override val specializedCategory = Category.All
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsJsonParser = IAResultsJsonParser(
        providerName = name,
        providerUrl = url,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/advancedsearch.php")
            append("?q=title:$query")
            appendCategory(category = context.category)
            append("&fl[]=title,item_size,publicdate,mediatype,identifier,btih")
            append("&rows=100")
            append("&page=1")
            append("&output=json")
        }

        val responseJson = context.httpClient.getJson(url = requestUrl) ?: return emptyList()
        return resultsJsonParser.parse(responseJson.asObject()).orEmpty()
    }

    override suspend fun getDetails(detailsPageUrl: String): GetTorrentDetailsResponse {
        val jsonMetadataPageUrl = detailsPageUrl.takeLastWhile { it != '/' }
            .let { "https://archive.org/metadata/$it" }

        return HttpClient.getJson(jsonMetadataPageUrl)
            ?.asObject()
            ?.let { IAMetadataJsonParser.parse(it) }
            ?.let(GetTorrentDetailsResponse::Success)
            ?: GetTorrentDetailsResponse.DetailsNotFound
    }

    private fun StringBuilder.appendCategory(category: Category) {
        if (category == Category.All) {
            return
        }

        this.append("%20AND%20")
        this.append("mediatype:%28")
        when (category) {
            Category.All -> throw IllegalStateException("Category.All is already covered")
            Category.Anime,
            Category.Games,
            Category.Music,
            Category.Porn,
            Category.Series,
            Category.Other,
                -> this.append("other")

            Category.Apps -> this.append("software")
            Category.Books -> this.append("texts")
            Category.Movies -> this.append("movies")
        }
        this.append("%29")
    }
}

private class IAResultsJsonParser(
    private val providerName: String,
    private val providerUrl: String,
) {
    suspend fun parse(json: JsonObject): List<Torrent>? = withContext(Dispatchers.Default) {
        json.getObject("response")
            ?.getArray("docs")
            ?.map { it.asObject() }
            ?.mapNotNull { parseDocObject(it) }
    }

    private fun parseDocObject(obj: JsonObject): Torrent? {
        val name = obj.getString("title") ?: return null
        val size = obj.getLong("item_size")
            ?.let { FileSizeUtils.formatBytes(it.toFloat()) }
            ?: return null
        val uploadDate = obj.getString("publicdate")?.let(DateUtils::formatIsoDate) ?: return null
        val category = obj.getString("mediatype")?.let(::categoryFromMediaType) ?: return null
        val descriptionPageUrl = obj.getString("identifier")
            ?.let { "$providerUrl/details/$it" }
            ?: return null
        val infoHash = obj.getString("btih")?.lowercase()?.trim() ?: return null

        return Torrent(
            infoHash = infoHash,
            name = name,
            size = size,
            seeders = 1U,
            peers = 1U,
            uploadDate = uploadDate,
            category = category,
            providerName = providerName,
            descriptionPageUrl = descriptionPageUrl,
        )
    }
}

private object IAMetadataJsonParser {
    suspend fun parse(json: JsonObject): TorrentDetails? = withContext(Dispatchers.Default) {
        val lastChecked = json.getLong("item_last_updated")?.let(DateUtils::formatEpochSecond)
        val size = json.getLong("item_size")?.toFloat()?.let(FileSizeUtils::formatBytes)

        val metadataObj = json.getObject("metadata") ?: return@withContext null
        val name = metadataObj.getString("title") ?: return@withContext null
        val uploadDate = metadataObj.getString("publicdate")
        val uploader = metadataObj.getString("uploader")
        val description = metadataObj.getString("description")
        val category = metadataObj.getString("metadata")?.let(::categoryFromMediaType)

        val torrentObj = json.getArray("files")
            ?.map { it.asObject() }
            ?.find { it.containsKey("btih") }
            ?: return@withContext null
        val id = metadataObj.getString("identifier") ?: return@withContext null
        val infoHash = torrentObj.getString("btih") ?: return@withContext null
        val torrentFileName = torrentObj.getString("name") ?: return@withContext null
        val fileDownloadLink = "https://archive.org/download/$id/$torrentFileName"

        TorrentDetails(
            infoHash = infoHash,
            name = name,
            size = size,
            uploadDate = uploadDate,
            category = category,
            uploader = uploader,
            lastChecked = lastChecked,
            magnetUri = TorrentUtils.createMagnetUri(infoHash),
            fileDownloadLink = fileDownloadLink,
            description = description,
        )
    }
}

private fun categoryFromMediaType(mediaType: String) = when (mediaType) {
    "software" -> Category.Apps
    "texts" -> Category.Books
    "movies" -> Category.Movies
    else -> Category.Other
}