package com.prajwalch.torrentsearch.torznab

import com.prajwalch.torrentsearch.constant.TorrentSearchConstants
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.provider.SearchProviderId
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

/**
 * An XML parser for the results returned by the indexer.
 *
 * See [API spec](https://torznab.github.io/spec-1.3-draft/torznab/Specification-v1.3.html).
 */
class TorznabResultsXmlParser(
    private val providerId: SearchProviderId,
    private val providerName: String,
) {
    private companion object {
        private const val ITEM = "channel > item"
        private const val GUID = "guid"
        private const val TITLE = "title"
        private const val SIZE = "size"
        private const val PUB_DATE = "pubDate"
        private const val COMMENTS = "comments"
        private const val ENCLOSURE = "enclosure"
        private const val LINK = "link"

        // Torznab specific attributes
        // See: https://torznab.github.io/spec-1.3-draft/torznab/Specification-v1.3.html#extended-attributes
        private const val SIZE_ATTR = """torznab|attr[name="size"]"""
        private const val SEEDERS_ATTR = """torznab|attr[name="seeders"]"""
        private const val PEERS_ATTR = """torznab|attr[name="peers"]"""
        private const val INFO_HASH_ATTR = """torznab|attr[name="infohash"]"""
        private const val MAGNET_URI_ATTR = """torznab|attr[name="magneturl"]"""
        private const val CATEGORY_ATTR = """torznab|attr[name="category"]"""

        private const val MAGNET_URI_PREFIX = "magnet:?xt="
    }

    suspend fun parse(xml: String): List<Torrent> = withContext(Dispatchers.Default) {
        Jsoup.parse(xml, "", Parser.xmlParser())
            .select(ITEM)
            .mapNotNull(::parseItem)
    }

    private fun parseItem(item: Element): Torrent {
        val guid = item.selectFirst(GUID)?.ownText()
            ?: error("TorznabResultsXmlParser: <guid> tag not found inside <item>")
        val torrentName = item.selectFirst(TITLE)?.ownText()
            ?: error("TorznabResultsXmlParser: <title> tag not found inside <item>")
        val magnetUri = extractMagnetUri(item)
            ?: error("TorznabResultsXmlParser: magnet URI not found inside <item>")

        val torrentId = TorrentUtils.createTorrentId(providerId, guid)
        val size = extractSize(item)
        val seeders = item.selectFirst(SEEDERS_ATTR)?.attr("value")?.toUIntOrNull()
        val peers = item.selectFirst(PEERS_ATTR)?.attr("value")?.toUIntOrNull()
        val uploadDate = item.selectFirst(PUB_DATE)?.ownText()?.let(TorrentDateParser::parseRFC1123)
        val category = extractAndInferCategory(item)
        val detailsPageUrl = item.selectFirst(COMMENTS)?.ownText()
        val fileDownloadLink = extractFileDownloadLink(item)

        return Torrent(
            id = torrentId,
            name = torrentName,
            size = size,
            seeders = seeders,
            peers = peers,
            uploadDate = uploadDate,
            category = category,
            providerName = providerName,
            magnetUri = MagnetUri.Available(magnetUri),
            fileDownloadLink = fileDownloadLink,
            detailsPageUrl = detailsPageUrl,
        )
    }

    private fun extractMagnetUri(item: Element): String? {
        fun tryGetFromInfoHash(): String? =
            item.selectFirst(INFO_HASH_ATTR)
                ?.attr("value")
                ?.let(TorrentUtils::createMagnetUri)

        fun tryGetFromEnclosureTag(): String? =
            item.selectFirst(ENCLOSURE)
                ?.takeIf { it.attr("type") == TorrentSearchConstants.MIME_TYPE_TORRENT }
                ?.attr("url")

        fun tryGetFromLinkTag(): String? =
            item.selectFirst(LINK)
                ?.ownText()
                ?.takeIf { it.startsWith(MAGNET_URI_PREFIX) }

        return item.selectFirst(MAGNET_URI_ATTR)?.attr("value")
            ?: tryGetFromInfoHash()
            ?: tryGetFromEnclosureTag()
            ?: tryGetFromLinkTag()
    }

    private fun extractSize(item: Element): String? {
        return (item.selectFirst(SIZE)?.ownText() ?: item.selectFirst(SIZE_ATTR)
            ?.attr("value"))
            ?.let(FileSizeUtils::formatBytes)
    }

    private fun extractAndInferCategory(item: Element): Category? {
        return item.select(CATEGORY_ATTR)
            .mapNotNull {
                it.attr("value")
                    .toInt()
                    .takeIf { id -> id > TorznabConstants.CUSTOM_CATEGORY_RANGE_START }
            }
            .maxOrNull()
            ?.let(TorznabCategoryMapper::getCategoryFromId)
    }

    private fun extractFileDownloadLink(item: Element): String? {
        return (item.selectFirst(LINK)?.ownText() ?: item.selectFirst(ENCLOSURE)?.attr("url"))
            ?.takeIf { !it.startsWith(MAGNET_URI_PREFIX) }
    }
}