package com.prajwalch.torrentsearch.providers

import android.util.Log

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorznabConfig
import com.prajwalch.torrentsearch.torznab.TorznabCategoryMapper
import com.prajwalch.torrentsearch.torznab.TorznabFunctions
import com.prajwalch.torrentsearch.torznab.TorznabResultsXmlParser
import com.prajwalch.torrentsearch.torznab.TorznabUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A search provider which is based on Torznab specification. */
class TorznabSearchProvider(private val config: TorznabConfig) : SearchProvider {
    override val id = config.id
    override val name = config.searchProviderName
    override val url = config.url
    override val supportedCategories = config.supportedCategories
    override val safetyStatus = SearchProviderSafetyStatus.Safe
    override val enabledByDefault = false
    override val type = SearchProviderType.Torznab

    /** Log tag for the current config. */
    private val tag = "$BASE_TAG($name)"

    /** The XML parser for the response. */
    private val resultsXmlParser = TorznabResultsXmlParser(providerName = name)

    override suspend fun search(query: String, context: SearchContext): List<Torrent> {
        Log.d(tag, "search")

        val apiUrl = TorznabUtils.normalizeApiUrl(config.url)
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
        Log.d(tag, "Received response of length ${responseXml.length}")

        return withContext(Dispatchers.Default) {
            resultsXmlParser.parse(xml = responseXml)
        }
    }

    private fun getCategoriesId(category: Category): String? {
        return if (category == Category.All || category !in config.supportedCategories) {
            null
        } else {
            TorznabCategoryMapper.getCatParamValue(category)
        }
    }

    companion object {
        private const val BASE_TAG = "TorznabSearchProvider"
    }
}