package com.prajwalch.torrentsearch.providers

import android.os.Build
import androidx.annotation.RequiresApi
import com.prajwalch.torrentsearch.data.SearchContext
import com.prajwalch.torrentsearch.data.SearchProvider
import com.prajwalch.torrentsearch.data.SearchProviderId
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Provider implementation for [Knaben](https://knaben.org).
 *
 * Extracts magnet URI results from HTML torrent listings.
 * This provider does not use a description/details page.
 */
class Knaben(override val id: SearchProviderId) : SearchProvider {

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val formattedQuery = query.trim().replace("\\s+".toRegex(), "%20")
        val categoryCode = getCategoryCode(context.category)
        val url = "$BASE_URL/search/$formattedQuery/$categoryCode/1/seeders"

        val html = context.httpClient.get(url)

        return withContext(Dispatchers.Default) {
            parseSearchResults(html)
        }
    }

    /** Maps internal [Category] enums to Knaben's category codes. */
    private fun getCategoryCode(category: Category): String = when (category) {
        Category.All -> "0"
        Category.Music -> "1000000"
        Category.Series -> "2000000"
        Category.Movies -> "3000000"
        Category.Apps -> "4000000"
        Category.Porn -> "5000000"
        Category.Anime -> "6000000"
        Category.Games -> "7000000"
        Category.Other -> "10000000"
        Category.Books -> "9000000"
    }

    /** Parses all valid <tr> rows into a list of torrents. */
    private fun parseSearchResults(html: String): List<Torrent> {
        return Jsoup.parse(html)
            .select("tbody > tr")
            .mapNotNull(::parseRow)
    }

    /** Parses a single row from the result table into a [Torrent] object. */
    private fun parseRow(row: Element): Torrent? {
        val name = row.selectFirst("td:nth-child(2) > a")?.text()?.trim().orEmpty()
        val magnetUri = row.selectFirst("a[href^=magnet:?xt=]")?.attr("href") ?: return null
        val size = row.selectFirst("td:nth-child(3)")?.text()?.trim().orEmpty()

        val uploadDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            parseUploadDate(row)
        } else {
            row.selectFirst("td:nth-child(4)")?.attr("title").orEmpty()
        }

        val seeds = row.selectFirst("td:nth-child(5)")?.text()?.trim()?.toUIntOrNull() ?: 0u
        val peers = row.selectFirst("td:nth-child(6)")?.text()?.trim()?.toUIntOrNull() ?: 0u
        val category = extractCategory(row)

        return Torrent(
            name = name,
            size = size,
            seeds = seeds,
            peers = peers,
            providerId = id,
            providerName = PROVIDER_NAME,
            uploadDate = uploadDate,
            category = category,
            descriptionPageUrl = "", // No description page
            infoHashOrMagnetUri = InfoHashOrMagnetUri.MagnetUri(magnetUri)
        )
    }

    /**
     * Extracts a [Category] enum by matching category label text.
     * Falls back to [Category.All] for unknown or missing labels.
     */
    private fun extractCategory(row: Element): Category {
        return when (row.selectFirst("td:first-child > a")?.text()?.trim().orEmpty()) {
            "Anime" -> Category.Anime
            "Movies" -> Category.Movies
            "TV" -> Category.Series
            "Audio" -> Category.Music
            "Console" -> Category.Games
            "PC" -> Category.Apps
            "Books" -> Category.Books
            "XXX" -> Category.Porn
            "Other" -> Category.Other
            else -> Category.All
        }
    }

    /**
     * Parses the upload date from the [title] attribute into `dd MMM yyyy`.
     * If parsing fails, falls back to the visible text.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseUploadDate(row: Element): String {
        val raw = row.selectFirst("td:nth-child(4)")?.attr("title") ?: return ""
        val inputFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val outputFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
        return LocalDateTime.parse(raw, inputFmt).format(outputFmt)
    }

    private companion object {
        private const val BASE_URL = "https://knaben.org"
        private const val PROVIDER_NAME = "knaben.org"
    }
}