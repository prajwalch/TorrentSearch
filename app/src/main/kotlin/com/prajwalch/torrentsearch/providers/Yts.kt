package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.Category
import com.prajwalch.torrentsearch.data.SearchContext
import com.prajwalch.torrentsearch.data.SearchProvider
import com.prajwalch.torrentsearch.models.FileSize
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.utils.prettyDate

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

class Yts : SearchProvider {
    override fun specializedCategory() = Category.Movies

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        return if (isQueryIMDBId(query)) {
            singleMovieLinks(query, context)
        } else {
            multipleMovieLinks(query, context)
        }
    }

    private fun isQueryIMDBId(query: String): Boolean {
        return query.startsWith("tt") && (query.trimStart('t').toUIntOrNull() != null)
    }

    /**
     * Fetches and returns all the available torrents of a movie associated with
     * the given `imdbid` if the response is in expected layout, otherwise empty list.
     *
     * Response layout. Visit https://yts.mx/api for more details.
     *
     *     status: "ok" | "error"
     *     status_message: <string>
     *     data:
     *        movie:       <movie object>
     *
     * @see [parseMovieObject]
     */
    private suspend fun singleMovieLinks(imdbId: String, context: SearchContext): List<Torrent> {
        val path = "/api/v2/movie_details.json"
        val queryParams = "?imdb_id=$imdbId"
        val requestUrl = "$BASE_URL$path$queryParams"

        val responseObject = context
            .httpClient
            .getJson(requestUrl)
            ?.jsonObject
            ?: return emptyList()
        val movieObject = responseObject["data"]
            ?.jsonObject["movie"]
            ?.jsonObject
            ?: return emptyList()

        return parseMovieObject(movieObject = movieObject)
    }

    /**
     * Fetches and returns all the available torrents of all movies that the
     * given `query` can pull.
     *
     * Response layout. Visit https://yts.mx/api for more details.
     *
     *     status: "ok" | "error"
     *     status_message: <string>
     *     data:
     *        movie_count:  <number>
     *        limit:        <number>
     *        page_number:  <number>
     *        movies:       <array of movie object>
     *
     * @see [parseMovieObject]
     */
    private suspend fun multipleMovieLinks(query: String, context: SearchContext): List<Torrent> {
        val path = "/api/v2/list_movies.json"
        val queryParams = "?query_term=$query"
        val requestUrl = "$BASE_URL$path$queryParams"

        val responseObject = context
            .httpClient
            .getJson(requestUrl)
            ?.jsonObject
            ?: return emptyList()

        val movieObjects = responseObject["data"]
            ?.jsonObject["movies"]
            ?.jsonArray
            ?.map { rawArrayItem -> rawArrayItem.jsonObject }
            ?: return emptyList()

        return movieObjects.flatMap { movieObject ->
            parseMovieObject(movieObject = movieObject)
        }
    }

    /**
     * Parses the movie object and returns it [Torrent]s.
     *
     * Object layout (only shown necessary fields):
     *
     *    title_long: <string>
     *    torrents:   <array of torrent object>
     *    ...
     *
     * @see [parseTorrentObject]
     */
    private fun parseMovieObject(movieObject: JsonObject): List<Torrent> {
        val titleLong = movieObject["title_long"]
            ?.toString()
            ?.trim('"')
            ?: return emptyList()

        val torrents = movieObject["torrents"]
            ?.jsonArray
            ?.map { rawArrayItem -> rawArrayItem.jsonObject }
            ?.mapNotNull { torrentObject -> parseTorrentObject(titleLong, torrentObject) }

        return torrents.orEmpty()
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
    private fun parseTorrentObject(movieTitle: String, torrentObject: JsonObject): Torrent? {
        val quality = torrentObject["quality"]?.toString()?.trim('"') ?: "-"
        val type = torrentObject["type"]?.toString()?.trim('"') ?: "-"
        val codec = torrentObject["video_codec"]?.toString()?.trim('"') ?: "-"

        val name = "$movieTitle [$quality] [$type] [$codec]"
        val hash = torrentObject["hash"]?.toString()?.trim('"') ?: return null

        val sizeBytes = torrentObject["size_bytes"]?.jsonPrimitive?.floatOrNull ?: return null
        val size = FileSize.fromBytes(sizeBytes)

        val seeds = torrentObject["seeds"]?.jsonPrimitive?.int?.toUInt() ?: return null
        val peers = torrentObject["peers"]?.jsonPrimitive?.int?.toUInt() ?: return null

        val uploadDateEpochSeconds = torrentObject["date_uploaded_unix"]
            ?.jsonPrimitive
            ?.long
            ?: return null
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
        private const val BASE_URL = "https://yts.mx"
        private const val NAME = "yts.mx"
    }
}