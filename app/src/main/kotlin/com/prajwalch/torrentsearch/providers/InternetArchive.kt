package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.extension.asObject
import com.prajwalch.torrentsearch.extension.getArray
import com.prajwalch.torrentsearch.extension.getLong
import com.prajwalch.torrentsearch.extension.getObject
import com.prajwalch.torrentsearch.extension.getString
import com.prajwalch.torrentsearch.util.DateUtils
import com.prajwalch.torrentsearch.util.FileSizeUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

class InternetArchive : SearchProvider {
    override val info = SearchProviderInfo(
        id = "internetarchive",
        name = "InternetArchive",
        url = "https://archive.org",
        specializedCategory = Category.All,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = buildString {
            append(info.url)
            append("/advancedsearch.php")
            append("?q=title:$query")
            appendCategory(category = context.category)
            append("&fl[]=title,item_size,publicdate,mediatype,identifier,btih")
            append("&rows=100")
            append("&page=1")
            append("&output=json")
        }

        val responseJson = context.httpClient.getJson(url = requestUrl) ?: return emptyList()
        val torrents = withContext(Dispatchers.Default) {
            parseResponseJson(json = responseJson.asObject())
        }

        return torrents.orEmpty()
    }

    private fun StringBuilder.appendCategory(category: Category) {
        if (category == Category.All) {
            return
        }

        this.append("%20AND%20")
        this.append("mediatype:%28")
        when (category) {
            Category.All -> throw IllegalStateException("Category.All is already covered")
            Category.Anime,
            Category.Games,
            Category.Music,
            Category.Porn,
            Category.Series,
            Category.Other,
                -> this.append("other")

            Category.Apps -> this.append("software")
            Category.Books -> this.append("texts")
            Category.Movies -> this.append("movies")
        }
        this.append("%29")
    }

    private fun parseResponseJson(json: JsonObject): List<Torrent>? {
        return json
            .getObject("response")
            ?.getArray("docs")
            ?.map { it.asObject() }
            ?.mapNotNull { parseDocObject(it) }
    }

    private fun parseDocObject(obj: JsonObject): Torrent? {
        val name = obj.getString("title") ?: return null
        val size = obj
            .getLong("item_size")
            ?.let { FileSizeUtils.formatBytes(it.toFloat()) }
            ?: return null
        val uploadDate = obj.getString("publicdate")?.let(DateUtils::formatIsoDate) ?: return null
        val category = obj.getString("mediatype")?.let(::inferMediaType) ?: return null
        val descriptionPageUrl = obj
            .getString("identifier")
            ?.let { "${info.url}/details/$it" }
            ?: return null
        val infoHash = obj.getString("btih") ?: return null

        return Torrent(
            infoHash = infoHash,
            name = name,
            size = size,
            seeders = 1U,
            peers = 1U,
            uploadDate = uploadDate,
            category = category,
            providerName = info.name,
            descriptionPageUrl = descriptionPageUrl,
        )
    }

    private fun inferMediaType(mediaType: String) = when (mediaType) {
        "software" -> Category.Apps
        "texts" -> Category.Books
        "movies" -> Category.Movies
        else -> Category.Other
    }
}