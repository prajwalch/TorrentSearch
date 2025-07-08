package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.Category
import com.prajwalch.torrentsearch.data.SearchContext
import com.prajwalch.torrentsearch.data.SearchProvider
import com.prajwalch.torrentsearch.extensions.asObject
import com.prajwalch.torrentsearch.extensions.getArray
import com.prajwalch.torrentsearch.extensions.getLong
import com.prajwalch.torrentsearch.extensions.getObject
import com.prajwalch.torrentsearch.extensions.getString
import com.prajwalch.torrentsearch.extensions.getUInt
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.utils.prettyDate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import kotlinx.serialization.json.JsonObject

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

        val responseJson = context.httpClient.getJson(url = requestUrl) ?: return emptyList()
        val torrents = withContext(Dispatchers.Default) {
            responseJson
                .asObject()
                .getObject("data")
                ?.getObject("movie")
                ?.let { movieObject ->
                    parseMovieObject(movieObject = movieObject)
                }
        }

        return torrents.orEmpty()
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

        val responseJson = context.httpClient.getJson(url = requestUrl) ?: return emptyList()
        val torrents = withContext(Dispatchers.Default) {
            responseJson
                .asObject()
                .getObject("data")
                ?.getArray("movies")
                ?.map { rawArrayItem -> rawArrayItem.asObject() }
                ?.flatMap { movieObject ->
                    parseMovieObject(movieObject = movieObject)
                }
        }

        return torrents.orEmpty()
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
        val descriptionPageUrl = movieObject.getString("url") ?: return emptyList()
        val titleLong = movieObject.getString("title_long") ?: return emptyList()

        val torrents = movieObject.getArray("torrents")
            ?.map { rawArrayItem -> rawArrayItem.asObject() }
            ?.mapNotNull { torrentObject ->
                parseTorrentObject(
                    descriptionPageUrl = descriptionPageUrl,
                    movieTitle = titleLong,
                    torrentObject = torrentObject,
                )
            }

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
    private fun parseTorrentObject(
        descriptionPageUrl: String,
        movieTitle: String,
        torrentObject: JsonObject,
    ): Torrent? {
        val quality = torrentObject.getString("quality") ?: "-"
        val type = torrentObject.getString("type") ?: "-"
        val codec = torrentObject.getString("video_codec") ?: "-"

        val name = "$movieTitle [$quality] [$type] [$codec]"
        val infoHash = torrentObject.getString("hash") ?: return null

        val size = torrentObject.getString("size") ?: return null
        val seeds = torrentObject.getUInt("seeds") ?: return null
        val peers = torrentObject.getUInt("peers") ?: return null

        val uploadDateEpochSeconds = torrentObject.getLong("date_uploaded_unix") ?: return null
        val uploadDate = prettyDate(uploadDateEpochSeconds)

        return Torrent(
            name = name,
            size = size,
            seeds = seeds,
            peers = peers,
            providerName = NAME,
            uploadDate = uploadDate,
            descriptionPageUrl = descriptionPageUrl,
            infoHashOrMagnetUri = InfoHashOrMagnetUri.InfoHash(infoHash),
        )
    }

    private companion object {
        private const val BASE_URL = "https://yts.mx"
        private const val NAME = "yts.mx"
    }
}