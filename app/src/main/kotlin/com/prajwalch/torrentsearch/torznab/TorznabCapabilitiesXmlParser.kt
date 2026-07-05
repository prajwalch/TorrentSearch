package com.prajwalch.torrentsearch.torznab

import android.util.Xml

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.extension.readParentTag
import com.prajwalch.torrentsearch.extension.skipCurrentTag
import org.xmlpull.v1.XmlPullParser

import java.util.Collections

/** Contains the capabilities of the search provider/indexer. */
data class TorznabCapabilities(val supportedCategories: Set<Category>)

/**
 * Torznab capabilities XML parser.
 *
 * See [API spec](https://torznab.github.io/spec-1.3-draft/torznab/Specification-v1.3.html#capabilities).
 */
class TorznabCapabilitiesXmlParser {
    private val parser = Xml.newPullParser()
    private val namespace: String? = null
    private val supportedCategories = mutableSetOf<Category>()

    fun parse(xml: String): TorznabCapabilities {
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(xml.byteInputStream(), null)
        parser.nextTag()

        supportedCategories.clear()
        readCaps()

        return TorznabCapabilities(
            supportedCategories = Collections.unmodifiableSet(supportedCategories),
        )
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

        val parentCategoryId = parser.getAttributeValue(null, "id")?.toInt()
        if (parentCategoryId != null && parentCategoryId < 100000) {
            val category = TorznabCategoryMapper.getCategoryFromId(parentCategoryId)
            supportedCategories.add(category)
        }

        parser.skipCurrentTag()
        return
    }

//    private fun readSubCat() {
//        parser.require(XmlPullParser.START_TAG, namespace, "subcat")
//
//        val subCategoryId = parser.getAttributeValue(null, "id")
//        if (subCategoryId.toInt() < CUSTOM_CATEGORY_RANGE_START) {
//            supportedCategoriesId.add(subCategoryId)
//        }
//
//        parser.nextTag()
//        parser.require(XmlPullParser.END_TAG, namespace, "subcat")
//    }
}