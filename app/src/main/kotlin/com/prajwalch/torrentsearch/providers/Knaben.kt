package com.prajwalch.torrentsearch.providers

import android.os.Build
import androidx.annotation.RequiresApi

import com.prajwalch.torrentsearch.extensions.asObject
import com.prajwalch.torrentsearch.extensions.getArray
import com.prajwalch.torrentsearch.extensions.getLong
import com.prajwalch.torrentsearch.extensions.getString
import com.prajwalch.torrentsearch.extensions.getUInt
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.utils.prettyFileSize

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Provider implementation using the official [Knaben API](https://knaben.org/api/v1).
 * Returns magnet-based torrents.
 */
class Knaben : SearchProvider {
    override val info = SearchProviderInfo(
        id = "knaben",
        name = "Knaben",
        url = "https://knaben.org",
        safetyStatus = SearchProviderSafetyStatus.Safe,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestBody = buildRequestJson(query, context.category)

        val responseJson = context.httpClient.postJson(
            url = "$BASE_URL/v1",
            payload = requestBody,
        ) ?: return emptyList()

        val torrents = withContext(Dispatchers.Default) {
            responseJson
                .asObject()
                .getArray("hits")
                ?.map { it.asObject() }
                ?.mapNotNull(::parseTorrentObject)
        }

        return torrents.orEmpty()
    }

    /** Builds the API request payload. */
    private fun buildRequestJson(query: String, category: Category): JsonObject {
        return buildJsonObject {
            put("query", JsonPrimitive(query))
            put("size", JsonPrimitive(50))
            put("order_by", JsonPrimitive("peers"))
            put("order_direction", JsonPrimitive("desc"))
            put("hide_unsafe", JsonPrimitive(true))
            put("hide_xxx", JsonPrimitive(false)) // TODO: NSFW can be implemented here
            putJsonArray("categories") {
                getKnabenCategoryIds(category).forEach { add(JsonPrimitive(it)) }
            }
        }
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
        val sizeBytes = obj.getLong("bytes") ?: return null
        val size = prettyFileSize(sizeBytes.toFloat())

        val seeders = obj.getUInt("seeders") ?: 0u
        val peers = obj.getUInt("peers") ?: 0u
        val uploadDateIso = obj.getString("date") ?: ""
        val uploadDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            formatIsoDateToPrettyDate(uploadDateIso)
        else
            uploadDateIso

        val descriptionPageUrl = obj.getString("details").orEmpty()
        val category = extractCategory(obj)

        return Torrent(
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            providerId = info.id,
            providerName = info.name,
            uploadDate = uploadDate,
            descriptionPageUrl = descriptionPageUrl,
            category = category,
            infoHashOrMagnetUri = InfoHashOrMagnetUri.MagnetUri(magnetUri),
        )
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

    /**
     * Parses ISO 8601 date (e.g., `2025-06-11T06:13:57+00:00`) into a more readable format.
     *
     * @return Date formatted as `"dd MMM yyyy"`, e.g., `"11 Jun 2025"`.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun formatIsoDateToPrettyDate(isoDate: String): String {
        val parsedDate = OffsetDateTime.parse(isoDate)
        val outputFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
        return parsedDate.format(outputFormatter)
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

    private companion object {
        private const val BASE_URL = "https://api.knaben.org"
    }
}