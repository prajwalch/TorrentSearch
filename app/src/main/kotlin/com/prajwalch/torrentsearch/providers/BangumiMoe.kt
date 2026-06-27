package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.extension.asObject
import com.prajwalch.torrentsearch.extension.getArray
import com.prajwalch.torrentsearch.extension.getString
import com.prajwalch.torrentsearch.extension.getUInt
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class BangumiMoe : SearchProvider, TorrentDetailsProvider {
    override val id = "bangumimoe"
    override val name = "BangumiMoe"
    override val url = "https://bangumi.moe"
    override val supportedCategories = setOf(Category.Anime)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    private val resultsJsonParser =
        BangumiMoeResultsJsonParser(
            providerName = name,
            providerUrl = url,
        )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = "$url/api/v2/torrent/search"
        val responseJson = context.httpClient.postJson(
            url = requestUrl,
            buildJsonObject { put("query", JsonPrimitive(query)) },
        ) ?: return emptyList()

        return resultsJsonParser.parse(responseJson)
    }

    private suspend fun parseDetailsJson(json: JsonElement): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val obj = json.asObject()
            val torrentName = obj.getString("title") ?: return@withContext null
            val magnetUri = obj.getString("magnet") ?: return@withContext null
            val infoHash = obj.getString("infohash")
                ?: TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
            val size = obj.getString("size")
            val seeders = obj.getUInt("seeders")
            val peers = obj.getUInt("leechers")
            val uploadDate = obj.getString("publish_time")?.let(TorrentDateParser::parseIso)

            TorrentDetails(
                infoHash = infoHash,
                magnetUri = magnetUri,
                name = torrentName,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = Category.Anime,
            )
        }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val torrentId = detailsPageUrl.takeLastWhile { it != '/' }
        val requestUrl = "https://bangumi.moe/api/torrent/fetch"
        val responseJson = HttpClient.postJson(
            url = requestUrl,
            payload = buildJsonObject { put("_id", JsonPrimitive(torrentId)) }
        ) ?: return null

        return parseDetailsJson(responseJson)
    }

}

private class BangumiMoeResultsJsonParser(
    private val providerName: String,
    private val providerUrl: String,
) {
    suspend fun parse(json: JsonElement): List<Torrent> = withContext(Dispatchers.Default) {
        json.asObject()
            .getArray("torrents")
            ?.mapNotNull { it.asObject() }
            ?.mapNotNull { parseTorrentObject(it) }
            .orEmpty()
    }

    private fun parseTorrentObject(obj: JsonObject): Torrent? {
        val torrentName = obj.getString("title") ?: return null
        val magnetUri = obj.getString("magnet") ?: return null
        val infoHash = obj.getString("infohash") ?: TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
        val size = obj.getString("size")
        val seeders = obj.getUInt("seeders")
        val peers = obj.getUInt("leechers")
        val uploadDate = obj.getString("publish_time")?.let(TorrentDateParser::parseIso)
        val detailsPageUrl = obj.getString("_id")?.let { "$providerUrl/torrent/$it" }

        return Torrent(
            infoHash = infoHash,
            magnetUri = magnetUri,
            name = torrentName,
            size = size ?: "0 KB",
            seeders = seeders ?: 0U,
            peers = peers ?: 0U,
            uploadDate = uploadDate,
            providerName = providerName,
            category = Category.Anime,
            descriptionPageUrl = detailsPageUrl ?: "",
        )
    }
}