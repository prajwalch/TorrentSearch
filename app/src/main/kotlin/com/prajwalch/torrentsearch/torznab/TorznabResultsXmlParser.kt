package com.prajwalch.torrentsearch.torznab

import android.util.Xml

import com.prajwalch.torrentsearch.constant.TorrentSearchConstants
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.extension.readParentTag
import com.prajwalch.torrentsearch.extension.skipCurrentTag
import com.prajwalch.torrentsearch.util.FileSizeUtils
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import org.xmlpull.v1.XmlPullParser
import java.time.Instant

/**
 * An XML parser for the results returned by the indexer.
 *
 * See [API spec](https://torznab.github.io/spec-1.3-draft/torznab/Specification-v1.3.html).
 */
class TorznabResultsXmlParser(private val providerName: String) {
    private val parser = Xml.newPullParser()
    private val namespace: String? = null
    private val torrents = mutableListOf<Torrent>()

    fun parse(xml: String): List<Torrent> {
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(xml.byteInputStream(), null)
        parser.nextTag()

        torrents.clear()
        readRss()

        return torrents.toList()
    }

    private fun readRss() {
        parser.require(XmlPullParser.START_TAG, namespace, "rss")
        parser.readParentTag(tagName = "rss") {
            if (parser.name == "channel") {
                readChannel()
            } else {
                parser.skipCurrentTag()
            }
        }
    }

    private fun readChannel() {
        parser.readParentTag(tagName = "channel") {
            if (parser.name == "item") {
                readItem()
            } else {
                parser.skipCurrentTag()
            }
        }
    }

    private fun readItem() {
        var torrentName: String? = null
        var size: String? = null
        var seeders: String? = null
        var peers: String? = null
        var uploadDate: Instant? = null
        var descriptionPageUrl: String? = null
        var magnetUri: String? = null
        var infoHash: String? = null
        var fileDownloadLink: String? = null

        val categoryIds = mutableSetOf<Int>()

        parser.readParentTag(tagName = "item") {
            when (parser.name) {
                "title" -> torrentName = readTitle()
                "comments" -> descriptionPageUrl = readComments()
                "pubDate" -> uploadDate = readPubDate()
                "size" -> size = readSize()
                "enclosure" if (fileDownloadLink == null) -> {
                    fileDownloadLink = readEnclosure()?.takeIf { !it.startsWith("magnet:?") }
                }
                // Torznab specific attributes.
                //
                // TODO: Attributes are optional so they are not guaranteed to always be present.
                //       We need some way to check them ahead of time and skip the <item> if not
                //       present so that we can prevent un-necessary processing. `XmlPullParser`
                //       doesn't contain any API for doing that.
                //
                // See: https://torznab.github.io/spec-1.3-draft/torznab/Specification-v1.3.html#extended-attributes
                "torznab:attr" -> when (parser.getAttributeValue(null, "name")) {
                    "seeders" -> seeders = readTorznabAttributeValue()
                    "peers" -> peers = readTorznabAttributeValue()
                    "magneturl" -> magnetUri = readTorznabAttributeValue()
                    "infohash" -> infoHash = readTorznabAttributeValue()
                    "category" -> {
                        val id = readTorznabAttributeValue().toInt()

                        if (id < TorznabConstants.CUSTOM_CATEGORY_RANGE_START) {
                            categoryIds.add(id)
                        }
                    }

                    // Some clients provide size inside the <torznab:size value../>
                    //
                    // Example: https://feed.animetosho.org/api?t=search&apikey=0&q=one
                    "size" if (size == null) -> {
                        size = readTorznabAttributeValue().let(FileSizeUtils::formatBytes)
                    }

                    else -> parser.skipCurrentTag()
                }

                else -> parser.skipCurrentTag()
            }
        }

        val category = categoryIds.maxOrNull()
            ?.let(TorznabCategoryMapper::getCategoryFromId) ?: Category.Other

        val torrent = Torrent(
            infoHash = infoHash ?: magnetUri?.let(TorrentUtils::getInfoHashFromMagnetUri) ?: return,
            name = torrentName ?: return,
            size = size,
            seeders = seeders?.toUIntOrNull(),
            peers = peers?.toUIntOrNull(),
            providerName = providerName,
            uploadDate = uploadDate,
            category = category,
            descriptionPageUrl = descriptionPageUrl,
            magnetUri = magnetUri,
            fileDownloadLink = fileDownloadLink,
        )
        torrents.add(torrent)
    }

    private fun readTitle(): String {
        return readTextContainedTag(tagName = "title")
    }

    private fun readComments(): String {
        return readTextContainedTag(tagName = "comments")
    }

    private fun readPubDate(): Instant {
        return readTextContainedTag(tagName = "pubDate")
            .let(TorrentDateParser::parseRFC1123)
    }

    private fun readSize(): String {
        val sizeBytes = readTextContainedTag(tagName = "size")
        return FileSizeUtils.formatBytes(bytes = sizeBytes)
    }

    private fun readTextContainedTag(tagName: String): String {
        parser.require(XmlPullParser.START_TAG, namespace, tagName)

        val text = readText()

        parser.require(XmlPullParser.END_TAG, namespace, tagName)
        return text
    }

    private fun readText(): String {
        var text = ""

        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.text
            parser.nextTag()
        }

        return text
    }

    private fun readEnclosure(): String? {
        parser.require(XmlPullParser.START_TAG, namespace, "enclosure")

        val url: String? = parser.getAttributeValue(null, "url")
        val type: String? = parser.getAttributeValue(null, "type")

        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, namespace, "enclosure")

        return if (type == TorrentSearchConstants.MIME_TYPE_TORRENT) url else null
    }

    private fun readTorznabAttributeValue(): String {
        parser.require(XmlPullParser.START_TAG, namespace, "torznab:attr")

        val value = parser.getAttributeValue(null, "value")

        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, namespace, "torznab:attr")

        return value
    }
}