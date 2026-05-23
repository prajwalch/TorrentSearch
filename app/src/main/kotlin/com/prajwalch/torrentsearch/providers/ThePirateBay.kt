package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.extension.asArray
import com.prajwalch.torrentsearch.extension.asObject
import com.prajwalch.torrentsearch.extension.getLong
import com.prajwalch.torrentsearch.extension.getString
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class ThePirateBay :
    SearchProvider,
    TorrentDetailsProvider,
    LatestTorrentsProvider,
    TopTorrentsProvider {
    override val id = "thepiratebay"
    override val name = "ThePirateBay"
    override val url = "https://thepiratebay.org"
    override val supportedCategories = setOf(
        Category.Apps,
        Category.Books,
        Category.Games,
        Category.Movies,
        Category.Music,
        Category.Porn,
        Category.Series,
        Category.Other,
    )
    override val safetyStatus = SearchProviderSafetyStatus.Unsafe(
        reason = R.string.tpb_unsafe_reason,
    )
    override val enabledByDefault = false
    override val alternateUrlDomains = listOf("https://knaben.xyz/thepiratebay/")

    private val resultsJsonParser = TBPResultsJsonParser(
        providerName = name,
        providerUrl = url,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(API_URL)
            append("/q.php")
            append("?q=$query")

            val categoryIndex = categoryId(context.category)
            append("&cat=$categoryIndex")
        }

        val responseJson = context.httpClient.getJson(requestUrl) ?: return emptyList()
        return resultsJsonParser.parse(responseJson)
    }

    override suspend fun getDetails(detailsPageUrl: String): TorrentDetails? {
        val id = detailsPageUrl.takeLastWhile { it != '=' }
        val requestUrl = "$API_URL/t.php?id=$id"

        return HttpClient.getJson(requestUrl)?.let { TBPDetailsJsonParser.parse(it) }
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = if (category == Category.All) {
            "$API_URL/precompiled/data_top100_recent.json"
        } else {
            val categoryId = categoryId(category)
            "$API_URL/q.php?q=category%3A$categoryId"
        }
        val responseJson = HttpClient.getJson(requestUrl) ?: return emptyList()

        return resultsJsonParser.parse(responseJson)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        val requestUrl = buildString {
            append(API_URL)
            append("/precompiled")

            if (category == Category.All) {
                append("/data_top100_48h.json")
            } else {
                val categoryId = categoryId(category)
                append("/data_top100_48h_$categoryId.json")
            }
        }
        val responseJson = HttpClient.getJson(requestUrl) ?: return emptyList()

        return resultsJsonParser.parse(responseJson)
    }

    /**
     * Returns the index of a given category.
     *
     * Visit https://thepiratebay.org/browse.php to see all the supported index.
     */
    private fun categoryId(category: Category): Int = when (category) {
        Category.All, Category.Anime -> 0
        Category.Apps -> 300
        Category.Books -> 601
        Category.Games -> 400
        Category.Movies, Category.Series -> 200
        Category.Music -> 101
        Category.Porn -> 500
        Category.Other -> 600
    }

    private companion object {
        private const val API_URL = "https://apibay.org"
    }
}

private class TBPResultsJsonParser(
    private val providerName: String,
    private val providerUrl: String,
) {
    suspend fun parse(json: JsonElement): List<Torrent> = withContext(Dispatchers.Default) {
        json
            .asArray()
            .map { rawArrayItem -> rawArrayItem.asObject() }
            .mapNotNull { torrentObject ->
                parseTorrentObject(torrentObject = torrentObject)
            }
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
        val descriptionPageUrl = "$providerUrl/description.php?id=$id"

        val infoHash = torrentObject.getString("info_hash")?.lowercase()?.trim() ?: return null
        val sizeBytes = torrentObject.getString("size") ?: return null
        val size = FileSizeUtils.formatBytes(bytes = sizeBytes)
        val seeders = torrentObject.getString("seeders")?.toUIntOrNull() ?: return null
        val peers = torrentObject.getString("leechers")?.toUIntOrNull() ?: return null
        val uploadDate = torrentObject
            .getString("added")
            ?.toLongOrNull()
            ?.let(TorrentDateParser::epochSecondToInstant)
            ?: return null

        val categoryId = torrentObject.getString("category") ?: return null
        val category = categoryFromId(categoryId.toInt())

        return Torrent(
            infoHash = infoHash,
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            providerName = providerName,
            uploadDate = uploadDate,
            category = category,
            descriptionPageUrl = descriptionPageUrl,
        )
    }
}

private object TBPDetailsJsonParser {
    suspend fun parse(json: JsonElement): TorrentDetails? =
        withContext(Dispatchers.Default) {
            val json = json.asObject()

            val infoHash = json.getString("info_hash")?.lowercase() ?: return@withContext null
            val name = json.getString("name") ?: return@withContext null
            val size = json.getLong("size")?.toFloat()?.let(FileSizeUtils::formatBytes)
            val seeders = json.getLong("seeders")?.toUInt()
            val peers = json.getLong("leechers")?.toUInt()
            val uploadDate = json.getLong("added")?.let(TorrentDateParser::epochSecondToInstant)
            val category = json.getLong("category")?.toInt()?.let(::categoryFromId)
            val uploader = json.getString("username")
            val description = json.getString("descr")
            val magnetUri = TorrentUtils.createMagnetUri(infoHash)

            TorrentDetails(
                infoHash = infoHash,
                name = name,
                size = size,
                seeders = seeders,
                peers = peers,
                uploadDate = uploadDate,
                category = category,
                uploader = uploader,
                magnetUri = magnetUri,
                description = description,
            )
        }
}

/** Returns the [Category] that matches the index. */
private fun categoryFromId(id: Int): Category = when (id) {
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