package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.Category
import com.prajwalch.torrentsearch.data.SearchContext
import com.prajwalch.torrentsearch.data.SearchProvider
import com.prajwalch.torrentsearch.extensions.asArray
import com.prajwalch.torrentsearch.extensions.asObject
import com.prajwalch.torrentsearch.extensions.getString
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.utils.prettyDate
import com.prajwalch.torrentsearch.utils.prettyFileSize

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

class ThePirateBay : SearchProvider {
    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val categoryIndex = categoryIndex(context.category)
        val queryParams = "?q=$query&cat=$categoryIndex"
        val requestUrl = "$URL$queryParams"

        val responseJson = context.httpClient.getJson(requestUrl) ?: return emptyList()
        val torrents = withContext(Dispatchers.Default) {
            responseJson
                .asArray()
                .map { rawArrayItem -> rawArrayItem.asObject() }
                .mapNotNull { torrentObject ->
                    parseTorrentObject(torrentObject = torrentObject)
                }
        }

        return torrents
    }

    /**
     * Returns the index of a given category.
     *
     * Visit https://thepiratebay.org/browse.php to see all the supported index.
     */
    private fun categoryIndex(category: Category): UInt = when (category) {
        Category.All, Category.Anime -> 0u
        Category.Apps -> 300u
        Category.Books -> 601u
        Category.Games -> 400u
        Category.Movies, Category.Series -> 200u
        Category.Music -> 101u
        Category.Porn -> 500u
    }

    /**
     * Parses the torrent object and returns [Torrent] if the object has a
     * expected layout, otherwise returns `null`.
     *
     * Object layout (only shown necessary fields).
     *
     *     name:               <string>
     *     info_hash:          <string>
     *     leechers:           <number>
     *     seeders:            <number>
     *     size:               <bytes in string>
     *     added:              <seconds in string>
     */
    private fun parseTorrentObject(torrentObject: JsonObject): Torrent? {
        val name = torrentObject.getString("name") ?: return null

        // Yeah, this is how it returns empty results.
        if (name == "No results returned") {
            return null
        }

        val hash = torrentObject.getString("info_hash") ?: return null
        val sizeBytes = torrentObject.getString("size") ?: return null
        val size = prettyFileSize(bytes = sizeBytes)
        val seeds = torrentObject.getString("seeders")?.toUIntOrNull() ?: return null
        val peers = torrentObject.getString("leechers")?.toUIntOrNull() ?: return null

        val uploadDateEpochSeconds = torrentObject.getString("added")?.toLongOrNull() ?: return null
        val uploadDate = prettyDate(uploadDateEpochSeconds)

        return Torrent(
            name = name,
            hash = hash,
            size = size,
            seeds = seeds,
            peers = peers,
            providerName = NAME,
            uploadDate = uploadDate
        )
    }

    private companion object {
        private const val URL = "https://apibay.org/q.php"
        private const val NAME = "thepiratebay.org"
    }
}