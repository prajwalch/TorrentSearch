package com.prajwalch.torrentsearch.torznab

import com.prajwalch.torrentsearch.testing.fixture

import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TorznabErrorResponseXmlParserTest {
    @Test
    fun `parse returns the error code`() = runTest {
        assertEquals(100, TorznabErrorResponseXmlParser.parse(fixture("torznab/error.xml")))
    }

    @Test
    fun `parse fails when the response is not an error`() = runTest {
        val xml = """<?xml version="1.0" encoding="UTF-8"?><rss version="2.0"><channel /></rss>"""

        val result = runCatching { TorznabErrorResponseXmlParser.parse(xml) }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
