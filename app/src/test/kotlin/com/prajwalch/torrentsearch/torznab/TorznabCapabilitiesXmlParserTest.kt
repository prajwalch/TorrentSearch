package com.prajwalch.torrentsearch.torznab

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.testing.fixture

import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TorznabCapabilitiesXmlParserTest {
    @Test
    fun `parse collects categories and subcategories outside the custom range`() = runTest {
        val capabilities = TorznabCapabilitiesXmlParser.parse(fixture("torznab/caps.xml"))

        // Category.Other is absent: its only source is a subcat of a custom
        // (>= 100000) parent category, which is skipped as a whole.
        assertEquals(
            setOf(
                Category.Movies,
                Category.Series,
                Category.Anime,
                Category.Music,
                Category.Books,
                Category.Apps,
                Category.Games,
                Category.Porn,
            ),
            capabilities.supportedCategories,
        )
    }

    @Test
    fun `parse fails when the response has no categories`() = runTest {
        val xml = """<?xml version="1.0" encoding="UTF-8"?><caps><categories /></caps>"""

        val result = runCatching { TorznabCapabilitiesXmlParser.parse(xml) }

        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    }
}
