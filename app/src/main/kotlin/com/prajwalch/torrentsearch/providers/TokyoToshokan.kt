package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.utils.FileSizeUnits

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class TokyoToshokan : SearchProvider {
    override val info = SearchProviderInfo(
        id = "tokyotoshokan",
        name = "TokyoToshokan",
        url = "https://tokyotosho.info",
        specializedCategory = Category.Anime,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = true,
    )

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        // https://www.tokyotosho.info/search.php?terms=One&type=1&searchName=true
        val queryParams = "?terms=$query&type=1&searchName=true"
        val requestUrl = "${info.url}/search.php$queryParams"

        val responseHtml = context.httpClient.get(url = requestUrl)
        val torrents = withContext(Dispatchers.Default) {
            parseHtml(html = responseHtml)
        }

        return torrents.orEmpty()
    }

    private fun parseHtml(html: String): List<Torrent>? {
        return Jsoup
            .parse(html)
            .selectFirst("table.listing > tbody")
            ?.children()
            ?.drop(1)
            ?.zipWithNext()
            ?.mapNotNull { (tr1, tr2) -> parseTableRow(tr1, tr2) }
    }

    private fun parseTableRow(tr1: Element, tr2: Element): Torrent? {
        // First tr's td contains magnet URI, torrent name and description page URL.
        //
        // Magnet URI and torrent name.
        val tr1SecondTd = tr1.selectFirst("td:nth-child(2)") ?: return null
        val magnetUri = tr1SecondTd
            .selectFirst("a:nth-child(1)")
            ?.attr("href")
            ?: return null
        val torrentName = tr1SecondTd
            .selectFirst("a:nth-child(2)")
            ?.text()
            ?.replace(oldValue = " ", newValue = "")
            ?: return null

        val descriptionPageRelativeUrl = tr1
            .selectFirst("td:nth-child(3)")
            ?.select("a")
            ?.last()
            ?.attr("href")
            ?: return null
        val descriptionPageUrl = "${info.url}/$descriptionPageRelativeUrl"

        // Second tr contains size, upload date, seeders and peers.
        //
        // Size and upload date.
        val tr2FirstTd = tr2.selectFirst("td:nth-child(1)") ?: return null
        val (sizeWithPrefix, uploadDateWithPrefix) = tr2FirstTd
            .ownText()
            .split('|')
            .drop(1)
            .map { it.trim() }

        val size = normalizeSize(sizeWithPrefix.removePrefix("Size: "))
        val uploadDate = uploadDateWithPrefix
            .removePrefix("Date: ")
            .split(' ')
            .first()

        // Seeders and peers.
        val tr2SecondTd = tr2.selectFirst("td:nth-child(2)") ?: return null
        val seeders = tr2SecondTd.selectFirst("span:nth-child(1)")?.ownText() ?: return null
        val peers = tr2SecondTd.selectFirst("span:nth-child(2)")?.ownText() ?: return null

        return Torrent(
            name = torrentName,
            size = size,
            seeders = seeders.toUIntOrNull() ?: 0u,
            peers = peers.toUIntOrNull() ?: 0u,
            providerId = info.id,
            providerName = info.name,
            uploadDate = uploadDate,
            category = info.specializedCategory,
            descriptionPageUrl = descriptionPageUrl,
            infoHashOrMagnetUri = InfoHashOrMagnetUri.MagnetUri(magnetUri),
        )
    }

    private fun normalizeSize(size: String): String {
        // 1. Find the unit.
        val unit = fileSizeUnits.find { size.endsWith(it) }!!
        // 2. Remove the unit from the size and reconstruct with space.
        val sizeNoUnit = size.removeSuffix(unit)
        return "$sizeNoUnit $unit"
    }

    private companion object {
        val fileSizeUnits = listOf(
            FileSizeUnits.PB,
            FileSizeUnits.TB,
            FileSizeUnits.GB,
            FileSizeUnits.MB,
            FileSizeUnits.KB,
        )
    }
}