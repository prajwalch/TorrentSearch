package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.extension.asObject
import com.prajwalch.torrentsearch.extension.getArray
import com.prajwalch.torrentsearch.extension.getLong
import com.prajwalch.torrentsearch.extension.getString
import com.prajwalch.torrentsearch.extension.getUInt
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray

/**
 * Provider implementation using the official [Knaben API](https://knaben.org/api/v1).
 * Returns magnet-based torrents.
 */
class Knaben : SearchProvider, LatestTorrentsProvider, TopTorrentsProvider {
    override val id = "knaben"
    override val name = "Knaben"
    override val url = "https://knaben.org"
    override val supportedCategories = setOf(
        Category.Anime,
        Category.Apps,
        Category.Books,
        Category.Games,
        Category.Movies,
        Category.Music,
        Category.Other,
        Category.Porn,
        Category.Series,
    )
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = true

    private val resultsJsonParser = KnabenResultsJsonParser(providerName = name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestBody = buildRequestJson(
            query = query,
            category = context.category,
            orderBy = "seeders",
        )
        val responseJson = context.httpClient.postJson(
            url = "$API_URL/v1",
            payload = requestBody,
        ) ?: return emptyList()

        return resultsJsonParser.parse(responseJson)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestBody = buildRequestJson(query = null, category = category, orderBy = "date")
        val responseJson = HttpClient.postJson(url = "$API_URL/v1", payload = requestBody)
            ?: return emptyList()

        return resultsJsonParser.parse(responseJson)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        val requestBody = buildRequestJson(query = null, category = category, orderBy = "seeders")
        val responseJson = HttpClient.postJson(url = "$API_URL/v1", payload = requestBody)
            ?: return emptyList()

        return resultsJsonParser.parse(responseJson)
    }

    /** Builds the API request payload. */
    private fun buildRequestJson(query: String?, category: Category, orderBy: String): JsonObject {
        return buildJsonObject {
            query?.let { put("query", JsonPrimitive(it)) }

            put("size", JsonPrimitive(300))
            put("order_by", JsonPrimitive(orderBy))
            put("order_direction", JsonPrimitive("desc"))
            put("hide_unsafe", JsonPrimitive(true))
            put("hide_xxx", JsonPrimitive(false)) // TODO: NSFW can be implemented here

            val categoryIds = getKnabenCategoryIds(category = category)
            if (categoryIds.isNotEmpty()) {
                putJsonArray("categories") {
                    categoryIds.forEach { add(JsonPrimitive(it)) }
                }
            }
        }
    }

    /**
     * Maps the internal [Category] enum to a list of Knaben category IDs.
     */
    private fun getKnabenCategoryIds(category: Category): List<Int> = when (category) {
        Category.All -> listOf()
        Category.Music -> listOf(1000000)
        Category.Series -> listOf(2000000)
        Category.Movies -> listOf(3000000)
        Category.Apps -> listOf(4000000)
        Category.Porn -> listOf(5000000)
        Category.Anime -> listOf(6000000)
        Category.Games -> listOf(7000000)
        Category.Books -> listOf(9000000)
        Category.Other -> listOf(10000000)
    }

    private companion object {
        private const val API_URL = "https://api.knaben.org"
    }
}

private class KnabenResultsJsonParser(private val providerName: String) {
    suspend fun parse(json: JsonElement): List<Torrent> = withContext(Dispatchers.Default) {
        json.asObject()
            .getArray("hits")
            ?.map { it.asObject() }
            ?.mapNotNull(::parseTorrentObject)
            .orEmpty()
    }

    /**
     * Parses a single result entry into a [Torrent].
     *
     * Example structure:
     * - "title": "Some Torrent"
     * - "hash": "ABCDEF..."
     * - "magnetUrl": "magnet:?..."
     * - "bytes": 123456
     * - "seeders": 10
     * - "peers": 20
     * - "date": "2025-06-11T06:13:57+00:00"
     */
    private fun parseTorrentObject(obj: JsonObject): Torrent? {
        val name = obj.getString("title") ?: return null
        val magnetUri = obj.getString("magnetUrl") ?: return null
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
        val size = obj.getLong("bytes")?.toFloat()?.let(FileSizeUtils::formatBytes)
        val seeders = obj.getUInt("seeders")
        val peers = obj.getUInt("peers")
        val uploadDate = obj.getString("date")?.let(TorrentDateParser::parseIso)
        val descriptionPageUrl = obj.getString("details").orEmpty()
        val category = extractCategory(obj)

        return Torrent(
            infoHash = infoHash,
            name = name,
            size = size ?: "0 KB",
            seeders = seeders ?: 0U,
            peers = peers ?: 0U,
            providerName = providerName,
            uploadDate = uploadDate,
            descriptionPageUrl = descriptionPageUrl,
            magnetUri = magnetUri,
            category = category,
        )
    }

    /**
     * Attempts to infer the [Category] from Knaben's numeric `categoryId` list.
     *
     * Knaben returns a list of numeric category IDs for each torrent result.
     * These IDs follow a specific pattern:
     * - Music       → 1000000
     * - Series      → 2000000
     * - Movies      → 3000000
     * - Apps        → 4000000
     * - Porn        → 5000000
     * - Anime       → 6000000
     * - Games       → 7000000
     * - Books       → 9000000
     * - Other       → 10000000
     *
     * Note: Simply using the first digit of the ID (e.g., '1') to determine the category
     * is **not reliable**, because both Music (`1000000`) and Other (`10000000`) start with '1'.
     * Therefore, this function uses numeric range matching to accurately determine the category.
     *
     * If multiple IDs are present, the one with the smallest value is used as the most specific.
     *
     * @param obj The JSON object representing the torrent result.
     * @return The inferred [Category], or [Category.All] if unable to classify.
     */
    private fun extractCategory(obj: JsonObject): Category {
        val categoryIds = obj.getArray("categoryId") ?: return Category.All

        // Use the smallest (most specific) categoryId for classification
        val firstId = categoryIds
            .mapNotNull { it.toString().toLongOrNull() }
            .minOrNull() ?: return Category.All

        return when (firstId) {
            in 1000000L..1999999L -> Category.Music
            in 2000000L..2999999L -> Category.Series
            in 3000000L..3999999L -> Category.Movies
            in 4000000L..4999999L -> Category.Apps
            in 5000000L..5999999L -> Category.Porn
            in 6000000L..6999999L -> Category.Anime
            in 7000000L..7999999L -> Category.Games
            in 9000000L..9999999L -> Category.Books
            in 10000000L..10999999L -> Category.Other
            else -> Category.All
        }
    }
}