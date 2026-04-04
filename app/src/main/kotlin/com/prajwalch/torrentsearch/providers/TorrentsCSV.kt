package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.extension.asObject
import com.prajwalch.torrentsearch.extension.getArray
import com.prajwalch.torrentsearch.extension.getLong
import com.prajwalch.torrentsearch.extension.getString
import com.prajwalch.torrentsearch.extension.getUInt
import com.prajwalch.torrentsearch.util.DateUtils
import com.prajwalch.torrentsearch.util.FileSizeUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

class TorrentsCSV : SearchProvider {
    override val info = SearchProviderInfo(
        id = "torrentscsv",
        name = "TorrentsCSV",
        url = "https://torrents-csv.com",
        specializedCategory = Category.All,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = true,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(info.url)
            append("/service")
            append("/search")
            append("?q=$query")
        }

        val responseJson = context.httpClient.getJson(url = requestUrl) ?: return emptyList()

        val torrents = withContext(Dispatchers.Default) {
            responseJson
                .asObject()
                .getArray("torrents")
                ?.map { arrayRawItem -> arrayRawItem.asObject() }
                ?.mapNotNull { torrentObject ->
                    parseTorrentObject(torrentObject = torrentObject)
                }
        }

        return torrents.orEmpty()
    }

    /**
     * Parses the torrent object and returns the [Torrent], if the object has
     * a expected layout. Visit [torrents-csv.com](https://torrents-csv.com/)
     * for more info.
     *
     * Object layout (only shown necessary fields):
     *
     *     name:         <string>
     *     infohash:     <string>
     *     size_bytes:   <number>
     *     seeders:      <number>
     *     leechers:     <number>
     *     created_unix: <number>
     * */
    private fun parseTorrentObject(torrentObject: JsonObject): Torrent? {
        val name = torrentObject.getString("name") ?: return null
        val infoHash = torrentObject.getString("infohash") ?: return null

        val sizeBytes = torrentObject.getLong("size_bytes") ?: return null
        val size = FileSizeUtils.formatBytes(bytes = sizeBytes.toFloat())

        val seeders = torrentObject.getUInt("seeders") ?: return null
        val peers = torrentObject.getUInt("leechers") ?: return null
        val uploadDate = torrentObject
            .getLong("created_unix")
            ?.let { DateUtils.formatEpochSecond(it) }
            ?: return null

        return Torrent(
            infoHash = infoHash,
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            providerName = info.name,
            uploadDate = uploadDate,
            descriptionPageUrl = "",
        )
    }
}