package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.ContentType
import com.prajwalch.torrentsearch.data.FileSize
import com.prajwalch.torrentsearch.data.Provider
import com.prajwalch.torrentsearch.data.Rank
import com.prajwalch.torrentsearch.data.SearchContext
import com.prajwalch.torrentsearch.data.Torrent

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class ThePirateBay : Provider {
    private val url = "https://apibay.org/q.php"

    override fun rank() = Rank.HIGH

    override fun name() = "thepiratebay"

    override suspend fun fetch(query: String, context: SearchContext): List<Torrent> {
        val contentTypeIndex = contentTypeIndex(context.contentType)
        val url = "$url?q=$query&cat=$contentTypeIndex"

        val response = context.httpClient.getJson(url)
        val torrents = response.jsonArray.map { it.jsonObject }.map(::parseJsonObject)

        return torrents
    }

    fun contentTypeIndex(contentType: ContentType): UInt = when (contentType) {
        ContentType.All, ContentType.Anime -> 0u
        ContentType.Apps -> 300u
        ContentType.Books -> 601u
        ContentType.Games -> 400u
        ContentType.Movies, ContentType.Series -> 200u
        ContentType.Music -> 101u
        ContentType.Porn -> 500u
    }

    fun parseJsonObject(jsonObject: JsonObject): Torrent {
        val name = jsonObject["name"]!!.toString().trim('"')
        val hash = jsonObject["info_hash"]!!.toString().trim('"')
        val size = FileSize.fromString(jsonObject["size"]!!.toString().trim('"'))
        val seeds = jsonObject["seeders"]!!.toString().trim('"').toUInt()
        val peers = jsonObject["leechers"]!!.toString().trim('"').toUInt()

        return Torrent(name, hash, size, seeds, peers, providerName = name())
    }
}