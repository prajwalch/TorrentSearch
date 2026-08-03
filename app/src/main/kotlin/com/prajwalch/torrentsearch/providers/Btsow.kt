package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.extension.asObject
import com.prajwalch.torrentsearch.extension.getArray
import com.prajwalch.torrentsearch.extension.getLong
import com.prajwalch.torrentsearch.extension.getObject
import com.prajwalch.torrentsearch.extension.getString
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray

class Btsow(private val networkClient: NetworkClient) : SearchProvider, TorrentDetailsProvider {
    override val id = "btsow"
    override val name = "Btsow"
    override val url = "https://btsow.live"
    override val supportedCategories = setOf(Category.Other)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsJsonParser = BtsowResultsJsonParser(name, url)

    override suspend fun search(query: String, category: Category): List<Torrent> {
        // [{"search":"one"},30,3]
        val requestPayload = buildJsonArray {
            addJsonObject {
                put("search", JsonPrimitive(query))
            }
            // Num of results per page.
            add(JsonPrimitive(30))
            // Page number
            add(JsonPrimitive(1))
        }
        val requestUrl = "$API_BASE_URL/search"
        val responseJson = networkClient.postJson(url = requestUrl, payload = requestPayload)
            ?: return emptyList()

        return resultsJsonParser.parse(responseJson)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val infoHash = detailsPageUrl.takeLastWhile { it != '/' }
        val requestPayload = buildJsonArray { add(JsonPrimitive(infoHash)) }
        val requestUrl = "$API_BASE_URL/magnet"
        val responseJson = networkClient.postJson(url = requestUrl, payload = requestPayload)
            ?: return null

        return BtsowDetailsJsonParser.parse(responseJson)
    }

    private companion object {
        private const val API_BASE_URL = "https://btsow.live/bts/data/api"
    }
}

private class BtsowResultsJsonParser(
    private val providerName: String,
    private val providerUrl: String,
) {
    suspend fun parse(responseJson: JsonElement): List<Torrent> = withContext(Dispatchers.Default) {
        responseJson.asObject()
            .getArray("data")
            ?.map { it.asObject() }
            ?.mapNotNull {
                val infoHash = it.getString("hash")?.lowercase() ?: return@mapNotNull null
                val torrentName = it.getString("name")
                    ?.replace("<em>", "")
                    ?.replace("</em>", "")
                    ?: return@mapNotNull null
                val size = it.getLong("size")?.toFloat()?.let(FileSizeUtils::formatBytes)

                Torrent(
                    infoHash = infoHash,
                    name = torrentName,
                    size = size,
                    providerName = providerName,
                    descriptionPageUrl = "$providerUrl/magnet/detail/$infoHash",
                )
            }
            .orEmpty()
    }
}

private object BtsowDetailsJsonParser {
    suspend fun parse(responseJson: JsonElement): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val dataObject = responseJson.asObject().getObject("data") ?: return@withContext null
            val infoHash = dataObject.getString("hash")?.lowercase() ?: return@withContext null
            val torrentName = dataObject.getString("name") ?: return@withContext null
            val size = dataObject.getLong("size")?.toFloat()?.let(FileSizeUtils::formatBytes)
            val uploadDate = dataObject.getLong("date")
                ?.let(TorrentDateParser::epochSecondToInstant)
            val lastChecked = dataObject.getLong("lastUpdateTime")
                ?.let(TorrentDateParser::epochSecondToInstant)

            TorrentDetails(
                infoHash = infoHash,
                name = torrentName,
                size = size,
                uploadDate = uploadDate,
                lastChecked = lastChecked,
                magnetUri = TorrentUtils.createMagnetUri(infoHash),
            )
        }
}