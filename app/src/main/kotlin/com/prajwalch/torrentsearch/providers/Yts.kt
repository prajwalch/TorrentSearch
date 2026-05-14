package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.extension.asObject
import com.prajwalch.torrentsearch.extension.getArray
import com.prajwalch.torrentsearch.extension.getLong
import com.prajwalch.torrentsearch.extension.getObject
import com.prajwalch.torrentsearch.extension.getString
import com.prajwalch.torrentsearch.extension.getUInt
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.util.TorrentDateParser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class Yts : SearchProvider, LatestTorrentsProvider, TopTorrentsProvider {
    override val id = "ytsmx"
    override val name = "Yts"
    override val url = "https://yts.bz"
    override val supportedCategories = setOf(Category.Movies)
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = true

    private val resultsJsonParser = YtsResultsJsonParser(providerName = name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(API_BASE_URL)
            append("/list_movies.json")
            append("?query_term=$query")
            append("&limit=50")
        }
        val responseJson = context.httpClient.getJson(url = requestUrl) ?: return emptyList()

        return resultsJsonParser.parse(responseJson)
    }

    override suspend fun getLastestTorrents(category: Category): List<Torrent> {
        val requestUrl = "$API_BASE_URL/list_movies.json"
        val responseJson = HttpClient.getJson(requestUrl) ?: return emptyList()

        return resultsJsonParser.parse(responseJson)
    }

    override suspend fun getTopTorrents(category: Category): List<Torrent> {
        // Using this url doesn't work. For some reason the server ignores the
        // 'sort_by' param.
        // https://movies-api.accel.li/api/v2/list_movies.json&sort_by=seeds
        return getLastestTorrents(category)
    }

    private companion object {
        private const val API_BASE_URL = "https://movies-api.accel.li/api/v2"
    }
}

private class YtsResultsJsonParser(private val providerName: String) {
    suspend fun parse(json: JsonElement): List<Torrent> = withContext(Dispatchers.Default) {
        json.asObject()
            .getObject("data")
            ?.getArray("movies")
            ?.map { item -> item.asObject() }
            ?.flatMap(::parseMovieObject)
            .orEmpty()
    }

    /**
     * Parses the movie object and returns it [Torrent]s.
     *
     * Object layout (only shown necessary fields):
     *
     *    url:        <string>
     *    title_long: <string>
     *    torrents:   <array of torrent object>
     *    ...
     *
     * @see [parseTorrentObject]
     */
    private fun parseMovieObject(movieObject: JsonObject): List<Torrent> {
        val titleLong = movieObject.getString("title_long") ?: return emptyList()
        val detailsPageUrl = movieObject.getString("url")

        return movieObject.getArray("torrents")
            ?.mapNotNull {
                parseTorrentObject(
                    torrentObject = it.asObject(),
                    movieTitle = titleLong,
                    detailsPageUrl = detailsPageUrl,
                )
            }
            .orEmpty()
    }

    /**
     * Parses the torrent object and returns [Torrent] if the object has a
     * expected layout, otherwise returns `null`.
     *
     * Object layout (only shown necessary fields).
     *
     *     hash:               <info hash>
     *     quality:            <e.g. 720p>
     *     type:               <e.g. bluray>
     *     video_codec:        <e.g. x264>
     *     seeds:              <number>
     *     peers:              <number>
     *     size_bytes:         <number>
     *     date_uploaded_unix: <number>
     *
     * The given `movieTitle` and fields `quality`, `type` and `video_codec`
     * are used to construct a torrent name. Something like:
     *
     *     Day of the Fight (1951) [720p] [bluray] [x264]
     */
    private fun parseTorrentObject(
        torrentObject: JsonObject,
        movieTitle: String,
        detailsPageUrl: String?,
    ): Torrent? {
        val infoHash = torrentObject.getString("hash")?.lowercase() ?: return null

        val quality = torrentObject.getString("quality") ?: "-"
        val type = torrentObject.getString("type") ?: "-"
        val codec = torrentObject.getString("video_codec") ?: "-"
        val name = "$movieTitle [$quality] [$type] [$codec]"

        val size = torrentObject.getString("size") ?: return null
        val seeders = torrentObject.getUInt("seeds") ?: return null
        val peers = torrentObject.getUInt("peers") ?: return null
        val uploadDate = torrentObject
            .getLong("date_uploaded_unix")
            ?.let(TorrentDateParser::epochSecondToInstant)

        return Torrent(
            infoHash = infoHash,
            name = name,
            size = size,
            seeders = seeders,
            peers = peers,
            providerName = providerName,
            uploadDate = uploadDate,
            category = Category.Movies,
            descriptionPageUrl = detailsPageUrl ?: "",
        )
    }
}