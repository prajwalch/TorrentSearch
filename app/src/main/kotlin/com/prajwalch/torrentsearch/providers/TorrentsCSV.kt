package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUriState
import com.prajwalch.torrentsearch.domain.model.SearchProviderSafety
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.extension.asObject
import com.prajwalch.torrentsearch.extension.getArray
import com.prajwalch.torrentsearch.extension.getLong
import com.prajwalch.torrentsearch.extension.getString
import com.prajwalch.torrentsearch.extension.getUInt
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.provider.SearchProvider
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

class TorrentsCSV(private val networkClient: NetworkClient) : SearchProvider {
    override val id = "torrentscsv"
    override val name = "TorrentsCSV"
    override val url = "https://torrents-csv.com"
    override val supportedCategories = setOf(Category.Other)
    override val safety = SearchProviderSafety.Safe
    override val enabledByDefault = true

    override suspend fun search(query: String, category: Category): List<Torrent> {
        val requestUrl = buildString {
            append(url)
            append("/service")
            append("/search")
            append("?q=$query")
        }

        val responseJson = networkClient.getJson(url = requestUrl) ?: return emptyList()

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
        val torrentRemoteId = torrentObject.getLong("id") ?: return null
        val torrentId = TorrentUtils.createTorrentId(
            providerId = id,
            sourceId = torrentRemoteId.toString(),
        )
        val magnetUri = TorrentUtils.createMagnetUri(infoHash)
        val size = torrentObject.getLong("size_bytes")
            ?.toFloat()
            ?.let(FileSizeUtils::formatBytes)
        val seeders = torrentObject.getUInt("seeders")
        val peers = torrentObject.getUInt("leechers")
        val uploadDate = torrentObject.getLong("created_unix")
            ?.let(TorrentDateParser::epochSecondToInstant)

        return Torrent(
            id = torrentId,
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            providerName = this.name,
            uploadDate = uploadDate,
            category = Category.Other,
            magnetUriState = MagnetUriState.Available(magnetUri),
        )
    }
}