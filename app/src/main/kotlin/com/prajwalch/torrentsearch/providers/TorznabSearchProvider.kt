package com.prajwalch.torrentsearch.providers


import android.util.Log
import android.util.Xml

import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.domain.models.Torrent
import com.prajwalch.torrentsearch.domain.models.TorznabConfig
import com.prajwalch.torrentsearch.domain.models.TorznabConnectionCheckResult
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.utils.FileSizeUtils

import io.ktor.client.statement.bodyAsText

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

import java.net.ConnectException
import java.net.UnknownHostException

/**
 *  Torznab functions.
 *
 * See [API spec](https://torznab.github.io/spec-1.3-draft/torznab/Specification-v1.3.html#function-overview).
 */
private object TorznabFunctions {
    /** Returns the capabilities of the api. */
    const val CAPS = "caps"

    /** Free text search query. */
    const val SEARCH = "search"
}

/** A search provider which is based on Torznab specification. */
class TorznabSearchProvider(
    id: SearchProviderId,
    private val config: TorznabConfig,
) : SearchProvider {
    override val info = SearchProviderInfo(
        id = id,
        name = config.searchProviderName,
        url = config.url,
        // TODO: We need to change the way we handle the specialized category.
        //       Instead of only storing on, let each search provider store all
        //       the supported categories.
        specializedCategory = config.category,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        enabledByDefault = false,
        type = SearchProviderType.Torznab,
    )

    /**
     * Category map.
     *
     * See:
     * - https://github.com/Jackett/Jackett/wiki/Jackett-Categories
     * - https://newznab.readthedocs.io/en/latest/misc/api.html#predefined-categories
     */
    private val categories = mapOf(
        Category.Anime to listOf("5070"),
        Category.Apps to listOf(
            "4000",
            "4010",
            "4020",
            "4030",
            "4040",
            "4050",
            "4060",
            "4070",
        ),
        Category.Books to listOf(
            "7000",
            "7010",
            "7020",
            "7030",
            "7040",
            "7050",
            "7060",
        ),
        Category.Games to listOf("4050"),
        Category.Movies to listOf(
            "2000",
            "2010",
            "2020",
            "2030",
            "2040",
            "2045",
            "2050",
            "2060",
            "2070",
            "2080",
        ),
        Category.Music to listOf(
            "3000",
            "3010",
            "3040",
            "3050",
            "3060",
        ),
        Category.Porn to listOf(
            "6000",
            "6010",
            "6020",
            "6030",
            "6040",
            "6045",
            "6050",
            "6060",
            "6070",
            "6080",
            "6090",
        ),
        Category.Series to listOf(
            "5000",
            "5010",
            "5020",
            "5030",
            "5040",
            "5045",
            "5050",
            "5060",
            "5080",
        ),
        Category.Other to listOf(
            "8000",
            "8010",
            "8020",
        ),
    )

    /**
     * Capabilities of the current search provider.
     *
     * This is fetched on the first run and cached for later runs.
     */
    private var capabilities: TorznabCapabilities? = null

    /** The XML parser for the capabilities. */
    private val capabilitiesXmlParser = TorznabCapabilitiesXmlParser()

    /** The XML parser for the response. */
    private val responseXmlParser = TorznabResponseXmlParser(providerName = info.name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        Log.d(TAG, "search")

        val apiUrl = normalizeUrl(url = config.url)

        if (capabilities == null) {
            Log.d(TAG, "Capabilities not found")
            capabilities = fetchCapabilities(apiUrl = apiUrl, httpClient = context.httpClient)
        } else {
            Log.d(TAG, "Reusing cached capabilities")
        }

        val requestUrl = buildString {
            append(apiUrl)
            append("?apikey=${config.apiKey}")
            // Force client to include all attributes.
            append("&extended=1")
            append("&t=${TorznabFunctions.SEARCH}")
            append("&q=$query")

            if (context.category != Category.All) {
                val categoriesId = getCategoriesId(category = context.category)
                append("&cat=$categoriesId")
            }
        }

        val responseXml = context.httpClient.get(url = requestUrl)
        Log.d(TAG, "Received response of length ${responseXml.length}")

        return withContext(Dispatchers.Default) {
            responseXmlParser.parse(xml = responseXml)
        }
    }

    private suspend fun fetchCapabilities(
        apiUrl: String,
        httpClient: HttpClient,
    ): TorznabCapabilities? {
        Log.d(TAG, "Fetching ${config.searchProviderName} capabilities")

        val requestUrl = "$apiUrl?t=${TorznabFunctions.CAPS}&apikey=${config.apiKey}"
        val capabilitiesResponseXml = httpClient.get(url = requestUrl)
        Log.d(TAG, "Capabilities fetch succeed")

        return withContext(Dispatchers.Default) {
            try {
                Log.d(TAG, "Attempting to parse capabilities")

                val capabilities = capabilitiesXmlParser.parse(xml = capabilitiesResponseXml)
                Log.d(TAG, "Capabilities parse succeed")

                capabilities
            } catch (e: XmlPullParserException) {
                Log.e(TAG, "Capabilities parse failed", e)
                null
            }
        }
    }

    private fun getCategoriesId(category: Category): String {
        val categoriesId = categories[category]!!
        val capabilities = capabilities ?: return categoriesId.joinToString()

        // Remove unsupported ids by checking against capabilities.
        val filteredCategoriesId = categoriesId.intersect(capabilities.supportedCategoriesId)
        return filteredCategoriesId.joinToString()
    }

    companion object {
        private const val TAG = "TorznabSearchProvider"
        private const val HTTP_STATUS_OK = 200
        private const val HTTP_STATUS_NOT_AUTHORIZED = 401
        private const val XML_DECLARATION = """<?xml version="1.0" encoding="UTF-8"?>"""

        suspend fun checkConnection(
            url: String,
            apiKey: String,
        ): TorznabConnectionCheckResult = withContext(Dispatchers.IO) {
            Log.i(TAG, "Checking connection")

            val apiUrl = normalizeUrl(url)
            val requestUrl = "$apiUrl?t=${TorznabFunctions.CAPS}&apikey=$apiKey"

            val response = try {
                Log.d(TAG, "Attempting to fetch capabilities")
                HttpClient.getResponse(url = requestUrl)
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Failed to resolve host IP address", e)
                return@withContext TorznabConnectionCheckResult.ConnectionFailed
            } catch (e: ConnectException) {
                Log.e(TAG, "Failed to establish a connection to host", e)
                return@withContext TorznabConnectionCheckResult.ConnectionFailed
            }

            Log.d(TAG, "Capabilities fetch succeed")

            val responseStatusCode = response.status.value

            // Some client returns 401 instead of returning 200 with error page
            // when invalid API key is given.
            //
            // For example: Prowlarr returns 401 but Jackett return 200
            // with proper error response with code following the spec.
            if (responseStatusCode == HTTP_STATUS_NOT_AUTHORIZED) {
                return@withContext TorznabConnectionCheckResult.InvalidApiKey
            }

            if (responseStatusCode != HTTP_STATUS_OK) {
                Log.d(TAG, "Received unexpected HTTP status code $responseStatusCode")
                return@withContext TorznabConnectionCheckResult.UnexpectedError
            }

            val responseXml = response.bodyAsText()
            val responseXmlWoDeclaration = responseXml.removePrefix(XML_DECLARATION).trimStart()

            if (responseXmlWoDeclaration.startsWith("<caps>")) {
                return@withContext TorznabConnectionCheckResult.ConnectionEstablished
            }

            if (!responseXmlWoDeclaration.startsWith("<error code=")) {
                val startTag = responseXmlWoDeclaration.takeWhile { it != '>' }
                Log.d(TAG, "Response starts with unexpected tag $startTag")

                return@withContext TorznabConnectionCheckResult.UnexpectedError
            }

            val errorResponseXmlParser = TorznabErrorResponseXmlParser()
            val errorCode = errorResponseXmlParser.parse(xml = responseXml)

            when (errorCode) {
                in 100..199 -> TorznabConnectionCheckResult.InvalidApiKey
                in 200..299 -> TorznabConnectionCheckResult.ApplicationError(errorCode)
                else -> TorznabConnectionCheckResult.UnexpectedResponse(errorCode)
            }
        }

        private fun normalizeUrl(url: String) = if (url.endsWith("api")) url else "${url}/api"
    }
}

/**
 * A XML parser for the response returned by the indexer.
 *
 * See [API spec](https://torznab.github.io/spec-1.3-draft/torznab/Specification-v1.3.html).
 */
private class TorznabResponseXmlParser(
    private val providerName: String,
) {
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
        var uploadDate: String? = null
        var descriptionPageUrl: String? = null
        var magnetUri: String? = null
        var infoHash: String? = null

        val categoryIds = mutableSetOf<Int>()

        parser.readParentTag(tagName = "item") {
            when (parser.name) {
                "title" -> torrentName = readTitle()
                "comments" -> descriptionPageUrl = readComments()
                "pubDate" -> uploadDate = readPubDate()
                "size" -> size = readSize()
                // Torznab specific attributes.
                //
                // TODO: Attributes are optional so they are not guaranteed to always be present.
                //       We need some way to check them ahead of time and skip the <item> if not
                //       present so that we can prevent un-necessary processing. `XmlPullParser`
                //       doesn't contain any API for doing that.
                //
                // See: https://torznab.github.io/spec-1.3-draft/torznab/Specification-v1.3.html#extended-attributes
                "torznab:attr" -> when (parser.getAttributeValue(null, "name")) {
                    "seeders" -> seeders = readTorznabAttribute(name = "seeders")
                    "peers" -> peers = readTorznabAttribute(name = "peers")
                    "magneturl" -> magnetUri = readTorznabAttribute(name = "magneturl")
                    "infohash" -> infoHash = readTorznabAttribute(name = "infohash")
                    "category" -> {
                        val id = readTorznabAttribute("category").toInt()

                        if (id < CUSTOM_CATEGORY_RANGE_START) {
                            categoryIds.add(id)
                        }
                    }

                    // Some clients provide size inside the <torznab:size value../>
                    //
                    // Example: https://feed.animetosho.org/api?t=search&apikey=0&q=one
                    "size" if (size == null) -> {
                        size = readTorznabAttribute(name = "size").let(FileSizeUtils::formatBytes)
                    }

                    else -> {
                        parser.skipCurrentTag()
                    }
                }

                else -> {
                    parser.skipCurrentTag()
                }
            }
        }

        val infoHashOrMagnetUri = when {
            magnetUri != null -> InfoHashOrMagnetUri.MagnetUri(uri = magnetUri)
            infoHash != null -> InfoHashOrMagnetUri.InfoHash(hash = infoHash)
            else -> return
        }
        val category = categoryIds.maxOrNull()?.let(::getCategoryFromId) ?: Category.Other

        val torrent = Torrent(
            name = torrentName ?: return,
            size = size ?: return,
            seeders = seeders?.toUIntOrNull() ?: return,
            peers = peers?.toUIntOrNull() ?: return,
            providerName = providerName,
            uploadDate = uploadDate ?: return,
            category = category,
            descriptionPageUrl = descriptionPageUrl ?: return,
            infoHashOrMagnetUri = infoHashOrMagnetUri,
        )
        torrents.add(torrent)
    }

    private fun readTitle(): String {
        return readTextContainedTag(tagName = "title")
    }

    private fun readComments(): String {
        return readTextContainedTag(tagName = "comments")
    }

    private fun readPubDate(): String {
        return readTextContainedTag(tagName = "pubDate")
            .split(' ')
            .subList(fromIndex = 1, toIndex = 4)
            .joinToString(separator = " ")
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

    private fun readTorznabAttribute(name: String): String {
        parser.require(XmlPullParser.START_TAG, namespace, "torznab:attr")

        val value = parser.getAttributeValue(null, "value")

        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, namespace, "torznab:attr")

        return value
    }

    private fun getCategoryFromId(id: Int): Category = when (id) {
        in 1000..1999 -> Category.Games
        in 2000..2999 -> Category.Movies
        in 3000..3999 -> if (id == 3030) Category.Books else Category.Music
        in 4000..4999 -> if (id == 4050) Category.Games else Category.Apps
        in 5000..5999 -> if (id == 5070) Category.Anime else Category.Series
        in 6000..6999 -> Category.Porn
        in 7000..7999 -> Category.Books
        in 8000..8999 -> Category.Other
        else -> Category.Other
    }

    private companion object {
        private const val CUSTOM_CATEGORY_RANGE_START = 100000
    }
}

/** Contains the capabilities of the search provider/indexer. */
private data class TorznabCapabilities(
    val supportedCategoriesId: Set<String>,
)

/**
 * Torznab capabilities XML parser.
 *
 * See [API spec](https://torznab.github.io/spec-1.3-draft/torznab/Specification-v1.3.html#capabilities).
 */
private class TorznabCapabilitiesXmlParser {
    private val parser = Xml.newPullParser()
    private val namespace: String? = null
    private val supportedCategoriesId = mutableSetOf<String>()

    fun parse(xml: String): TorznabCapabilities {
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(xml.byteInputStream(), null)
        parser.nextTag()

        supportedCategoriesId.clear()
        readCaps()

        return TorznabCapabilities(supportedCategoriesId = supportedCategoriesId)
    }

    private fun readCaps() {
        parser.readParentTag(tagName = "caps") {
            if (parser.name == "categories") {
                readCategories()
            } else {
                parser.skipCurrentTag()
            }
        }
    }

    private fun readCategories() {
        parser.readParentTag(tagName = "categories") {
            if (parser.name == "category") {
                readCategory()
            } else {
                parser.skipCurrentTag()
            }
        }
    }

    private fun readCategory() {
        parser.require(XmlPullParser.START_TAG, namespace, "category")

        val parentCategoryId = parser.getAttributeValue(null, "id")
        supportedCategoriesId.add(parentCategoryId)

        // Some <category> can contain child tags <subcat>.
        parser.readParentTag(tagName = "category") {
            if (parser.name == "subcat") {
                readSubCat()
            } else {
                parser.skipCurrentTag()
            }
        }
    }

    private fun readSubCat() {
        parser.require(XmlPullParser.START_TAG, namespace, "subcat")

        val subCategoryId = parser.getAttributeValue(null, "id")
        supportedCategoriesId.add(subCategoryId)

        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, namespace, "subcat")
    }
}

/** A XML parser for the error response. */
private class TorznabErrorResponseXmlParser {
    private val parser = Xml.newPullParser()
    private val namespace: String? = null

    fun parse(xml: String): Int {
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(xml.byteInputStream(), null)
        parser.nextTag()

        return readError()
    }

    private fun readError(): Int {
        parser.require(XmlPullParser.START_TAG, namespace, "error")

        val code = parser.getAttributeValue(null, "code").toInt()

        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, namespace, "error")

        return code
    }
}

/** Reads the current tag and calls the callback on each child tag. */
private fun XmlPullParser.readParentTag(tagName: String, onStartTag: () -> Unit) {
    this.require(XmlPullParser.START_TAG, namespace, tagName)

    while (this.next() != XmlPullParser.END_TAG) {
        if (this.eventType != XmlPullParser.START_TAG) {
            continue
        }
        onStartTag()
    }
}

/** Skips the current tag including all of its child. */
private fun XmlPullParser.skipCurrentTag() {
    if (this.eventType != XmlPullParser.START_TAG) {
        return
    }

    var depth = 1

    while (depth != 0) {
        when (this.next()) {
            XmlPullParser.END_TAG -> depth -= 1
            XmlPullParser.START_TAG -> depth += 1
        }
    }
}