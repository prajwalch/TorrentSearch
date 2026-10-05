package com.prajwalch.torrentsearch.torznab

import com.prajwalch.torrentsearch.domain.model.Category

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

/**
 * Contains the capabilities of the search provider/indexer.
 */
data class TorznabCapabilities(val supportedCategories: Set<Category>)

/**
 * Torznab capabilities XML parser.
 *
 * See [API spec](https://torznab.github.io/spec-1.3-draft/torznab/Specification-v1.3.html#capabilities).
 */
object TorznabCapabilitiesXmlParser {
    private const val CATEGORY = "caps > categories > category"
    private const val SUB_CATEGORY = "subcat"

    suspend fun parse(xml: String): TorznabCapabilities = withContext(Dispatchers.Default) {
        val supportedCategories = Jsoup.parse(xml, Parser.xmlParser())
            .select(CATEGORY)
            .map(::parseCategory)
            .reduce { acc, categories -> acc union categories }

        TorznabCapabilities(supportedCategories = supportedCategories)
    }

    private fun parseCategory(category: Element): Set<Category> {
        val parentCategoryId = category.attr("id").toInt()
        if (parentCategoryId >= TorznabConstants.CUSTOM_CATEGORY_RANGE_START) return emptySet()

        val parentCategory = TorznabCategoryMapper.getCategoryFromId(parentCategoryId)
        if (category.childrenSize() == 0) return setOf(parentCategory)

        val childCategories = category.select(SUB_CATEGORY)
            .mapNotNull {
                it.attr("id")
                    .toInt()
                    .takeIf { id -> id < TorznabConstants.CUSTOM_CATEGORY_RANGE_START }
                    ?.let(TorznabCategoryMapper::getCategoryFromId)
            }
            .toSet()

        return childCategories.plus(parentCategory)
    }
}