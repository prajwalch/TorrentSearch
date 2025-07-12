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

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class AnimeTosho(override val id: SearchProviderId) : SearchProvider {
    override fun specializedCategory() = Category.Anime

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        val requestUrl = "$BASE_URL/search?q=$query"
        val responseHtml = context.httpClient.get(url = requestUrl)

        return withContext(Dispatchers.Default) {
            parseHtml(html = responseHtml)
        }
    }

    /** Parses the entire result HTML and returns all the extracted torrents. */
    private fun parseHtml(html: String): List<Torrent> {
        return Jsoup
            .parse(html)
            .select("div.home_list_entry")
            .mapNotNull { parseEntryDiv(it) }
    }

    /** Parses an individual result row into a [Torrent] object. */
    private fun parseEntryDiv(entryDiv: Element): Torrent? {
        val anchor = entryDiv.selectFirst("div.link > a") ?: return null
        val name = anchor.text()
        val descriptionPageUrl = anchor.attr("href")

        val size = entryDiv.selectFirst("div.size")?.ownText() ?: return null
        val (seeds, peers) = parseSeedsAndPeers(entryDiv)

        val uploadDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            parseUploadDate(entryDiv) ?: return null
        } else {
            entryDiv.selectFirst("div.date")?.ownText() ?: return null
        }

        val magnetUri = entryDiv
            .select("div.links > a")
            .firstOrNull { it.ownText() == "Magnet" }
            ?.attr("href")
            ?: return null

        return Torrent(
            name = name,
            size = size,
            seeds = seeds,
            peers = peers,
            providerId = id,
            providerName = NAME,
            uploadDate = uploadDate,
            category = Category.Anime,
            descriptionPageUrl = descriptionPageUrl,
            infoHashOrMagnetUri = InfoHashOrMagnetUri.MagnetUri(magnetUri),
        )
    }

    /** Parses the upload date and converts "Today"/"Yesterday" into real dates. */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseUploadDate(entryDiv: Element): String? {
        val raw = entryDiv
            .selectFirst("div.date")
            ?.attr("title")
            ?.removePrefix(DATE_PREFIX)
            ?.trim()
            ?: return null

        val normalizedRaw = when {
            raw.startsWith("Today") -> {
                val timePart = raw.removePrefix("Today").trim()
                val today = LocalDate.now()
                "${today.format(INPUT_DATE_ONLY)} $timePart"
            }

            raw.startsWith("Yesterday") -> {
                val timePart = raw.removePrefix("Yesterday").trim()
                val yesterday = LocalDate.now().minusDays(1)
                "${yesterday.format(INPUT_DATE_ONLY)} $timePart"
            }

            else -> raw
        }

        return try {
            val input = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH)
            val output = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            output.format(input.parse(normalizedRaw)!!)
        } catch (_: Exception) {
            raw
        }
    }

    /** Extracts seeds and peers from the stats block. */
    private fun parseSeedsAndPeers(entryDiv: Element): Pair<UInt, UInt> {
        val span = entryDiv
            .selectFirst("div.links")
            ?.select("span")
            ?.firstOrNull { span -> span.hasAttr("title") }
            ?: return Pair(0u, 0u)
        val spanText = span.ownText()

        val match = STATS_REGEX.find(spanText)
        val seeds = match?.groupValues?.getOrNull(1)?.toUIntOrNull() ?: 0u
        val peers = match?.groupValues?.getOrNull(2)?.toUIntOrNull() ?: 0u

        return seeds to peers
    }

    private companion object {
        private const val BASE_URL = "https://animetosho.org"
        private const val NAME = "animetosho.org"
        private const val DATE_PREFIX = "Date/time submitted: "
        private val STATS_REGEX = """\[(\d+)↑/(\d+)↓]""".toRegex()

        @RequiresApi(Build.VERSION_CODES.O)
        private val INPUT_DATE_ONLY = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}