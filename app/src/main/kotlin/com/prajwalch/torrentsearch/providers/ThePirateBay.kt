package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.Category
import com.prajwalch.torrentsearch.data.SearchContext
import com.prajwalch.torrentsearch.data.SearchProvider
import com.prajwalch.torrentsearch.models.FileSize
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.utils.prettyDate

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class ThePirateBay : SearchProvider {
    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val categoryIndex = this@ThePirateBay.categoryIndex(context.category)
        val url = "$URL?q=$query&cat=$categoryIndex"

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
            providerName = NAME,
            uploadDate = uploadDate
        )
    }

    private companion object {
        private const val URL = "https://apibay.org/q.php"
        private const val NAME = "thepiratebay.org"
    }
}