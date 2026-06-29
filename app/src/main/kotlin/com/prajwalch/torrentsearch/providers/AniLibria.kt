package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.extension.asArray
import com.prajwalch.torrentsearch.extension.asObject
import com.prajwalch.torrentsearch.extension.getLong
import com.prajwalch.torrentsearch.extension.getObject
import com.prajwalch.torrentsearch.extension.getString
import com.prajwalch.torrentsearch.extension.getUInt
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentDateParser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

/**
 * Provider implementation backed by the public [AniLibria API](https://anilibria.top/api/v1).
 *
 * The search endpoint returns matching releases. The torrents for each release
 * are fetched from a separate endpoint, then mapped to magnet-based [Torrent]s.
 */
class AniLibria : SearchProvider {
    override val id = "anilibria"
    override val name = "AniLibria"
    override val url = "https://www.anilibria.top"
    override val supportedCategories = setOf(Category.Anime)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val searchUrl = buildString {
            append(API_URL)
            append("/app/search/releases")
            append("?query=$query")
        }
        val responseJson = context.httpClient.getJson(url = searchUrl) ?: return emptyList()

        val releaseIds = withContext(Dispatchers.Default) {
            responseJson
                .asArray()
                .map { it.asObject() }
                .mapNotNull { it.getLong("id") }
                .take(MAX_RELEASES)
        }

        return coroutineScope {
            releaseIds
                .map { releaseId -> async { fetchReleaseTorrents(context, releaseId) } }
                .awaitAll()
                .flatten()
        }
    }

    /** Fetches and parses the torrents that belong to a single release. */
    private suspend fun fetchReleaseTorrents(
        context: SearchContext,
        releaseId: Long,
    ): List<Torrent> {
        val torrentsUrl = "$API_URL/anime/torrents/release/$releaseId"
        val responseJson = context.httpClient.getJson(url = torrentsUrl) ?: return emptyList()

        return withContext(Dispatchers.Default) {
            responseJson
                .asArray()
                .map { it.asObject() }
                .mapNotNull { parseTorrentObject(it) }
        }
    }

    /**
     * Parses a single torrent entry into a [Torrent], or returns `null` if a
     * required field is missing.
     *
     * Example structure (only the used fields are shown):
     *
     *     hash:       <string>
     *     label:      <string>
     *     size:       <number>
     *     magnet:     <string>
     *     seeders:    <number>
     *     leechers:   <number>
     *     created_at: <ISO 8601 string>
     *     release:    { name: { english, main }, alias: <string> }
     */
    private fun parseTorrentObject(obj: JsonObject): Torrent? {
        val infoHash = obj.getString("hash") ?: return null

        val release = obj.getObject("release")
        val releaseName = release
            ?.getObject("name")
            ?.let { it.getString("english") ?: it.getString("main") }
        val name = obj.getString("label") ?: releaseName ?: return null

        val size = obj.getLong("size")?.toFloat()?.let(FileSizeUtils::formatBytes)
        val magnetUri = obj.getString("magnet")
        val seeders = obj.getUInt("seeders")
        val peers = obj.getUInt("leechers")
        val uploadDate = obj.getString("created_at")?.let(TorrentDateParser::parseIso)
        val descriptionPageUrl = release
            ?.getString("alias")
            ?.let { "$url/anime/releases/release/$it" }

        return Torrent(
            infoHash = infoHash,
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            providerName = this.name,
            uploadDate = uploadDate,
            category = Category.Anime,
            descriptionPageUrl = descriptionPageUrl,
            magnetUri = magnetUri,
        )
    }

    private companion object {
        private const val API_URL = "https://anilibria.top/api/v1"

        /** Maximum number of matched releases to fetch torrents for. */
        private const val MAX_RELEASES = 15
    }
}
