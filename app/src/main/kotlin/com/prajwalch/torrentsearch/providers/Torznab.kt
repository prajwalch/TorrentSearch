package com.prajwalch.torrentsearch.providers

import android.util.Log
import android.util.Xml

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.InfoHashOrMagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.utils.prettyFileSize

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.xmlpull.v1.XmlPullParser

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

/** Configuration for the Torznab search provider. */
data class TorznabSearchProviderConfig(
    val id: String,
    val name: String,
    val url: String,
    val apiKey: String,
    val category: Category = Category.All,
    val safetyStatus: SearchProviderSafetyStatus = SearchProviderSafetyStatus.Safe,
    val enabledByDefault: Boolean = false,
)

/** A Torznab API compatible search provider. */
class TorznabSearchProvider(private val config: TorznabSearchProviderConfig) : SearchProvider {
    override val info = SearchProviderInfo(
        id = config.id,
        name = config.name,
        url = config.url,
        // TODO: We need to change the way we handle the specialized category.
        //       Instead of only storing on, let each search provider store all
        //       the supported categories.
        specializedCategory = config.category,
        safetyStatus = config.safetyStatus,
        enabledByDefault = config.enabledByDefault,
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

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        if (capabilities == null) {
            capabilities = fetchCapabilities(httpClient = context.httpClient)
        }

        var queryParams =
            "?apikey=${config.apiKey}&extended=1&t=${TorznabFunctions.SEARCH}&q=$query"

        if (context.category != Category.All) {
            val categoriesId = getCategoriesId(category = context.category)
            queryParams += "&cat=$categoriesId"
        }

        val requestUrl = "${config.url}/api$queryParams"
        Log.d(TAG, "Requesting $requestUrl")
        val responseXml = context.httpClient.get(url = requestUrl)
        Log.d(TAG, "-> Got response; length = ${responseXml.length}")

        return withContext(Dispatchers.Default) {
            val xmlParser = TorznabResponseXmlParser(
                providerId = info.id,
                providerName = info.name,
                category = if (info.specializedCategory != Category.All) {
                    info.specializedCategory
                } else {
                    // FIXME: If the category is `All` then we need to read the
                    //        category from the each result itself.
                    context.category
                },
            )
            xmlParser.parse(xml = responseXml)
        }
    }

    private suspend fun fetchCapabilities(httpClient: HttpClient): TorznabCapabilities {
        Log.i(TAG, "Fetching ${config.name} capabilities ")

        val requestUrl = "${config.url}/api?t=${TorznabFunctions.CAPS}&apikey=${config.apiKey}"
        val capabilitiesResponseXml = httpClient.get(url = requestUrl)

        Log.i(TAG, "-> Capabilities fetch succeed")

        return withContext(Dispatchers.Default) {
            val capabilitiesXmlParser = TorznabCapabilitiesXmlParser()
            Log.i(TAG, "Parsing capabilities")
            val capabilities = capabilitiesXmlParser.parse(xml = capabilitiesResponseXml)
            Log.i(TAG, "Capabilities parse succeed")

            capabilities
        }
    }

    private fun getCategoriesId(category: Category): String {
        val categoriesId = categories[category]!!
        val capabilities = capabilities ?: return categoriesId.joinToString()

        // Remove unsupported ids by checking against capabilities.
        val filteredCategoriesId = categoriesId.intersect(capabilities.supportedCategoriesId)
        return filteredCategoriesId.joinToString()
    }

    private companion object {
        private const val TAG = "TorznabSearchProvider"
    }
}

/**
 * A XML parser for the response returned by the indexer.
 *
 * See [API spec](https://torznab.github.io/spec-1.3-draft/torznab/Specification-v1.3.html).
 */
private class TorznabResponseXmlParser(
    private val providerId: SearchProviderId,
    private val providerName: String,
    private val category: Category,
) {
    private val parser = Xml.newPullParser()
    private val namespace: String? = null
    private val torrents = mutableListOf<Torrent>()

    fun parse(xml: String): List<Torrent> {
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(xml.byteInputStream(), null)
        parser.nextTag()

        readRss()
        return torrents.toList()
    }

    private fun readRss() {
        parser.require(XmlPullParser.START_TAG, namespace, "rss")
        Log.i(TAG, "Reading <rss>")

        parser.readParentTag(tagName = "rss") {
            if (parser.name == "channel") {
                readChannel()
            } else {
                Log.i(TAG, "-> Skipping <${parser.name}>")
                parser.skipCurrentTag()
            }
        }
    }

    private fun readChannel() {
        Log.i(TAG, "Reading <channel>")

        parser.readParentTag(tagName = "channel") {
            if (parser.name == "item") {
                readItem()
            } else {
                Log.i(TAG, "-> Skipping <${parser.name}>")
                parser.skipCurrentTag()
            }
        }
    }

    private fun readItem() {
        Log.i(TAG, "Reading <item>")

        var torrentName: String? = null
        var size: String? = null
        var seeders: String? = null
        var peers: String? = null
        var uploadDate: String? = null
        var descriptionPageUrl: String? = null
        var magnetUri: String? = null
        var infoHash: String? = null

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
                    else -> {
                        Log.d(TAG, "-> Skipping <${parser.name}>")
                        parser.skipCurrentTag()
                    }
                }

                else -> {
                    Log.d(TAG, "-> Skipping <${parser.name}>")
                    parser.skipCurrentTag()
                }
            }
        }

        val infoHashOrMagnetUri = when {
            magnetUri != null -> InfoHashOrMagnetUri.MagnetUri(uri = magnetUri)
            infoHash != null -> InfoHashOrMagnetUri.InfoHash(hash = infoHash)
            else -> {
                Log.w(TAG, "Both info hash and magnet URI not found. Skipping..")
                return
            }
        }
        val torrent = Torrent(
            name = torrentName ?: return,
            size = size ?: return,
            seeders = seeders?.toUIntOrNull() ?: return,
            peers = peers?.toUIntOrNull() ?: return,
            providerId = providerId,
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
        return prettyFileSize(bytes = sizeBytes)
    }

    private fun readTextContainedTag(tagName: String): String {
        parser.require(XmlPullParser.START_TAG, namespace, tagName)
        Log.i(TAG, "Reading <$tagName>")

        val text = readText()
        Log.d(TAG, "-> Extracted text = $text")

        parser.require(XmlPullParser.END_TAG, namespace, tagName)
        return text
    }

    private fun readText(): String {
        Log.i(TAG, "Reading text from <${parser.name}>")
        var text = ""

        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.text
            parser.nextTag()
        } else {
            Log.e(TAG, "-> Not a text")
        }

        return text
    }

    private fun readTorznabAttribute(name: String): String {
        parser.require(XmlPullParser.START_TAG, namespace, "torznab:attr")
        Log.i(TAG, "Reading <torrent:attr $name>")

        val value = parser.getAttributeValue(null, "value")
        Log.d(TAG, "-> Attribute value = $value")

        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, namespace, "torznab:attr")

        return value
    }

    private companion object {
        private const val TAG = "TorznabResponseXmlParser"
    }
}

/** Contains the capabilities of the search provider/indexer. */
private data class TorznabCapabilities(
    val supportedCategoriesId: List<String>,
)

/**
 * Torznab capabilities XML parser.
 *
 * See [API spec](https://torznab.github.io/spec-1.3-draft/torznab/Specification-v1.3.html#capabilities).
 */
private class TorznabCapabilitiesXmlParser {
    private val parser = Xml.newPullParser()
    private val namespace: String? = null
    private val supportedCategoriesId = mutableListOf<String>()

    fun parse(xml: String): TorznabCapabilities {
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(xml.byteInputStream(), null)
        parser.nextTag()

        readCaps()

        return TorznabCapabilities(supportedCategoriesId = supportedCategoriesId.toList())
    }

    private fun readCaps() {
        Log.i(TAG, "Reading <caps>")

        parser.readParentTag(tagName = "caps") {
            if (parser.name == "categories") {
                readCategories()
            } else {
                Log.d(TAG, "-> Skipping <${parser.name}>")
                parser.skipCurrentTag()
            }
        }
    }

    private fun readCategories() {
        Log.i(TAG, "Reading <categories>")

        parser.readParentTag(tagName = "categories") {
            if (parser.name == "category") {
                readCategory()
            } else {
                Log.d(TAG, "-> Skipping <${parser.name}>")
                parser.skipCurrentTag()
            }
        }
    }

    private fun readCategory() {
        parser.require(XmlPullParser.START_TAG, namespace, "category")
        Log.i(TAG, "Reading <category>")

        val parentCategoryId = parser.getAttributeValue(null, "id")
        Log.d(TAG, "-> Parent category id = $parentCategoryId")
        supportedCategoriesId.add(parentCategoryId)

        // Some <category> can contain child tags <subcat>.
        parser.readParentTag(tagName = "category") {
            if (parser.name == "subcat") {
                readSubCat()
            } else {
                Log.d(TAG, "-> Skipping <${parser.name}>")
                parser.skipCurrentTag()
            }
        }
    }

    private fun readSubCat() {
        parser.require(XmlPullParser.START_TAG, namespace, "subcat")
        Log.i(TAG, "Reading <subcat>")

        val subCategoryId = parser.getAttributeValue(null, "id")
        Log.d(TAG, "-> Sub category id = $subCategoryId")
        supportedCategoriesId.add(subCategoryId)

        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, namespace, "subcat")
    }

    private companion object {
        private const val TAG = "TorznabCapabilitiesXmlParser"
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