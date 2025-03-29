package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.ContentType
import com.prajwalch.torrentsearch.data.FileSize
import com.prajwalch.torrentsearch.data.Provider
import com.prajwalch.torrentsearch.data.Rank
import com.prajwalch.torrentsearch.data.SearchContext
import com.prajwalch.torrentsearch.data.Torrent

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class Yts : Provider {
    private val baseURL = "https://yts.mx"

    override fun rank() = Rank.Companion.highest(0u)

    override fun specializedContentType(): ContentType? = ContentType.Movies

    override fun name() = "yts"

    override suspend fun fetch(query: String, context: SearchContext): List<Torrent> {
        return if (isQueryIMDBId(query)) {
            singleMovieLinks(query, context)
        } else {
            multipleMovieLinks(query, context)
        }
    }

    private fun isQueryIMDBId(query: String): Boolean {
        return query.startsWith("tt") && (query.trimStart('t').toUIntOrNull() != null)
    }

    private suspend fun singleMovieLinks(imdbId: String, context: SearchContext): List<Torrent> {
        val path = "/api/v2/movie_details.json"
        val query = "?imdb_id=$imdbId"
        val url = "$baseURL$path$query"

        val response = context.httpClient.getJson(url)
        val torrents =
            parseMovieObject(response.jsonObject["data"]!!.jsonObject["movie"]!!.jsonObject)

        return torrents
    }

    private suspend fun multipleMovieLinks(query: String, context: SearchContext): List<Torrent> {
        val path = "/api/v2/list_movies.json"
        val query = "?query_term=$query"
        val url = "$baseURL$path$query"

        val response = context.httpClient.getJson(url)
        val torrents = response.jsonObject["data"]!!.jsonObject["movies"]?.jsonArray?.flatMap {
            parseMovieObject(it.jsonObject)
        }

        return torrents.orEmpty()
    }

    private fun parseMovieObject(jsonObject: JsonObject): List<Torrent> {
        val title = jsonObject["title_long"]!!.toString().trim('"')
        val torrents =
            jsonObject["torrents"]!!.jsonArray.map { parseTorrentObject(title, it.jsonObject) }

        return torrents
    }

    private fun parseTorrentObject(title: String, jsonObject: JsonObject): Torrent {
        val hash = jsonObject["hash"]!!.toString().trim('"')
        val size = FileSize.fromBytes(jsonObject["size_bytes"]!!.jsonPrimitive.float)
        val seeds = jsonObject["seeds"]!!.jsonPrimitive.int.toUInt()
        val peers = jsonObject["peers"]!!.jsonPrimitive.int.toUInt()

        val quality = jsonObject["quality"]!!.toString().trim('"')
        val type = jsonObject["type"]!!.toString().trim('"')
        val codec = jsonObject["video_codec"]!!.toString().trim('"')

        val name = "$title [$quality] [$type] [$codec]"
        return Torrent(name, hash, size, seeds, peers, providerName = name())
    }
}