package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.extensions.asObject
import com.prajwalch.torrentsearch.extensions.getArray
import com.prajwalch.torrentsearch.extensions.getLong
import com.prajwalch.torrentsearch.extensions.getString
import com.prajwalch.torrentsearch.extensions.getUInt
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.utils.prettyDate
import com.prajwalch.torrentsearch.utils.prettyFileSize

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

class TorrentsCsv : SearchProvider {
    override val info = SearchProviderInfo(
        id = "torrentscsv",
        name = "TorrentsCSV",
        url = "https://torrents-csv.com",
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabled = true,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val queryParams = "?q=$query"
        val requestUrl = "${info.url}/service/search$queryParams"

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
        val size = prettyFileSize(bytes = sizeBytes.toFloat())

        val seeders = torrentObject.getUInt("seeders") ?: return null
        val peers = torrentObject.getUInt("leechers") ?: return null

        val uploadDateEpochSeconds = torrentObject.getLong("created_unix") ?: return null
        val uploadDate = prettyDate(epochSeconds = uploadDateEpochSeconds)

        return Torrent(
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            providerId = info.id,
            providerName = info.name,
            uploadDate = uploadDate,
            descriptionPageUrl = "",
            infoHashOrMagnetUri = InfoHashOrMagnetUri.InfoHash(infoHash),
        )
    }
}