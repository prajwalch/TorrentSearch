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
        val categoryIndex = categoryIndex(context.category)
        val requestUrl = "$URL?q=$query&cat=$categoryIndex"

        val response = context.httpClient.getJson(requestUrl)
        val torrents = response
            ?.jsonArray
            ?.map { arrayItem -> arrayItem.jsonObject }
            ?.mapNotNull(::parseJsonObject)

        return torrents.orEmpty()
    }

    private fun categoryIndex(category: Category): UInt = when (category) {
        Category.All, Category.Anime -> 0u
        Category.Apps -> 300u
        Category.Books -> 601u
        Category.Games -> 400u
        Category.Movies, Category.Series -> 200u
        Category.Music -> 101u
        Category.Porn -> 500u
    }

    private fun parseJsonObject(jsonObject: JsonObject): Torrent? {
        val name = jsonObject["name"]?.toString()?.trim('"') ?: return null

        // Yeah, this is how it returns empty results.
        if (name == "No results returned") {
            return null
        }

        val hash = jsonObject["info_hash"]?.toString()?.trim('"') ?: return null
        val sizeBytes = jsonObject["size"]?.toString()?.trim('"') ?: return null
        val seeds = jsonObject["seeders"]?.toString()?.trim('"')?.toUIntOrNull() ?: return null
        val peers = jsonObject["leechers"]?.toString()?.trim('"')?.toUIntOrNull() ?: return null

        val uploadDateEpochSeconds =
            jsonObject["added"]?.toString()?.trim('"')?.toLongOrNull() ?: return null
        val uploadDate = prettyDate(uploadDateEpochSeconds)

        return Torrent(
            name = name,
            hash = hash,
            size = FileSize.fromString(sizeBytes),
            seeds = seeds,
            peers = peers,
            providerName = NAME,
            uploadDate = uploadDate
        )
    }

    private companion object {
        private const val URL = "https://apibay.org/q.php"
        private const val NAME = "thepiratebay.org"
    }
}