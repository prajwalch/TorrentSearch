package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.Category
import com.prajwalch.torrentsearch.data.FileSize
import com.prajwalch.torrentsearch.data.Provider
import com.prajwalch.torrentsearch.data.SearchContext
import com.prajwalch.torrentsearch.data.Torrent
import com.prajwalch.torrentsearch.utils.prettyDate

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class ThePirateBay : Provider {
    private val url = "https://apibay.org/q.php"

    override fun name() = "thepiratebay.org"

    override suspend fun fetch(query: String, context: SearchContext): List<Torrent> {
        val categoryIndex = this@ThePirateBay.categoryIndex(context.category)
        val url = "$url?q=$query&cat=$categoryIndex"

        val response = context.httpClient.getJson(url)
        val torrents = response?.let { it.jsonArray.map { it.jsonObject }.map(::parseJsonObject) }

        return torrents.orEmpty()
    }

    fun categoryIndex(category: Category): UInt = when (category) {
        Category.All, Category.Anime -> 0u
        Category.Apps -> 300u
        Category.Books -> 601u
        Category.Games -> 400u
        Category.Movies, Category.Series -> 200u
        Category.Music -> 101u
        Category.Porn -> 500u
    }

    fun parseJsonObject(jsonObject: JsonObject): Torrent {
        val name = jsonObject["name"]!!.toString().trim('"')
        val hash = jsonObject["info_hash"]!!.toString().trim('"')
        val size = FileSize.fromString(jsonObject["size"]!!.toString().trim('"'))
        val seeds = jsonObject["seeders"]!!.toString().trim('"').toUInt()
        val peers = jsonObject["leechers"]!!.toString().trim('"').toUInt()

        val uploadDateEpochSeconds = jsonObject["added"]!!.toString().trim('"').toLong()
        val uploadDate = prettyDate(uploadDateEpochSeconds)

        return Torrent(
            name,
            hash,
            size,
            seeds,
            peers,
            providerName = name(),
            uploadDate = uploadDate
        )
    }
}