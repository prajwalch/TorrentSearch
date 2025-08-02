package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.extensions.asArray
import com.prajwalch.torrentsearch.extensions.asObject
import com.prajwalch.torrentsearch.extensions.getString
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.utils.prettyDate
import com.prajwalch.torrentsearch.utils.prettyFileSize

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

class ThePirateBay : SearchProvider {
    override val info = SearchProviderInfo(
        id = "thepiratebay",
        name = "ThePirateBay",
        url = "https://thepiratebay.org",
        specializedCategory = Category.All,
        safetyStatus = SearchProviderSafetyStatus.Unsafe(
            reason = "Terrible regulation, and the calculated injection of insidious malware."
        ),
        enabled = false,
    )

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
        Category.Other -> 600u
    }

    /**
     * Parses the torrent object and returns [Torrent] if the object has a
     * expected layout, otherwise returns `null`.
     *
     * Object layout (only shown necessary fields).
     *
     *     id:                 <string>
     *     name:               <string>
     *     info_hash:          <string>
     *     leechers:           <number>
     *     seeders:            <number>
     *     size:               <bytes in string>
     *     added:              <seconds in string>
     *     category:           <seconds in string>
     */
    private fun parseTorrentObject(torrentObject: JsonObject): Torrent? {
        val name = torrentObject.getString("name") ?: return null

        // Yeah, this is how it returns empty results.
        if (name == "No results returned") {
            return null
        }

        val id = torrentObject.getString("id") ?: return null
        val descriptionPageUrl = "${info.url}/description.php?id=$id"

        val infoHash = torrentObject.getString("info_hash") ?: return null
        val sizeBytes = torrentObject.getString("size") ?: return null
        val size = prettyFileSize(bytes = sizeBytes)
        val seeders = torrentObject.getString("seeders")?.toUIntOrNull() ?: return null
        val peers = torrentObject.getString("leechers")?.toUIntOrNull() ?: return null

        val uploadDateEpochSeconds = torrentObject.getString("added")?.toLongOrNull() ?: return null
        val uploadDate = prettyDate(uploadDateEpochSeconds)

        val categoryIndex = torrentObject.getString("category") ?: return null
        val category = getCategoryFromIndex(categoryIndex = categoryIndex.toInt())

        return Torrent(
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            providerId = info.id,
            providerName = info.name,
            uploadDate = uploadDate,
            category = category,
            descriptionPageUrl = descriptionPageUrl,
            infoHashOrMagnetUri = InfoHashOrMagnetUri.InfoHash(infoHash),
        )
    }

    /** Returns the [Category] that matches the index. */
    private fun getCategoryFromIndex(categoryIndex: Int): Category = when (categoryIndex) {
        // Apps
        in 300..306, 399 -> Category.Apps
        // Books
        601 -> Category.Books
        // Games
        in 400..408, 499 -> Category.Games
        // Movies
        201, 202, 204, 207, 209, 210, 211 -> Category.Movies
        // Music
        in 100..104, 199 -> Category.Music
        // Porn
        in 500..507, 599 -> Category.Porn
        // Series
        205, 208, 212 -> Category.Series
        // All, Anime
        //
        // TPB doesn't have dedicated anime category instead it mostly falls
        // under the series category.
        else -> Category.Other
    }

    private companion object {
        private const val URL = "https://apibay.org/q.php"
    }
}