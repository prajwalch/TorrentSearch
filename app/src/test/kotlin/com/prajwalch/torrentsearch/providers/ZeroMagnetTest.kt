package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.testing.fakeNetworkClient
import com.prajwalch.torrentsearch.testing.fixture

import kotlinx.coroutines.test.runTest

import java.time.Instant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 0Magnet is an adult provider, so its fixtures are hand-written with neutral
// placeholder content that mirrors the markup structure the selectors expect.
class ZeroMagnetTest {
    private val searchUrl = "https://9mag.net/search?q=sample"
    private val detailsUrl = "https://9mag.net/!aB3x"
    private val magnetUri = "magnet:?xt=urn:btih:A1B2C3D4E5F60718293A4B5C6D7E8F9012345678&dn=Sample%20Video%20One"

    @Test
    fun `search parses every result row and skips non-result rows`() = runTest {
        val provider = ZeroMagnet(fakeNetworkClient(searchUrl to fixture("zeromagnet/search.html")))

        val torrents = provider.search(query = "sample", category = Category.Porn)

        assertEquals(
            listOf("Sample Video One", "Sample & Clip Two", "Sample Collection Three"),
            torrents.map { it.name },
        )
        assertEquals(listOf("1.37 GB", "700.5 MB", "512 KB"), torrents.map { it.size })
        assertEquals(
            listOf("2026-10-05T00:00:00Z", "2026-05-28T00:00:00Z", "2025-12-31T00:00:00Z")
                .map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        val detailsPageUrls = listOf(
            "https://9mag.net/!aB3x",
            "https://9mag.net/!Zq7m",
            "https://9mag.net/!Lm04",
        )
        assertEquals(detailsPageUrls, torrents.map { it.detailsPageUrl })
        assertEquals(detailsPageUrls.map(MagnetUri::RequiresFetch), torrents.map { it.magnetUri })
        assertTrue(torrents.all { it.seeders == null && it.peers == null })
        assertTrue(torrents.all { it.category == Category.Porn })
        assertTrue(torrents.all { it.providerName == "0Magnet" })
        assertEquals(3, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = ZeroMagnet(fakeNetworkClient(searchUrl to "<html><body><p>No results</p></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "sample", category = Category.Porn))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = ZeroMagnet(fakeNetworkClient(detailsUrl to fixture("zeromagnet/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Sample Video One", details.name)
        assertEquals("a1b2c3d4e5f60718293a4b5c6d7e8f9012345678", details.infoHash)
        assertEquals(magnetUri, details.magnetUri)
        assertEquals("1.37 GB", details.size)
        assertEquals(Instant.parse("2026-10-05T14:22:05Z"), details.uploadDate)
        assertEquals(Category.Porn, details.category)
    }

    @Test
    fun `getDetails returns null when the page has no torrent`() = runTest {
        val provider = ZeroMagnet(fakeNetworkClient(detailsUrl to "<html><body><h1>404</h1></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }

    @Test
    fun `getMagnetUri extracts the magnet from the details page`() = runTest {
        val provider = ZeroMagnet(fakeNetworkClient(detailsUrl to fixture("zeromagnet/details.html")))

        assertEquals(magnetUri, provider.getMagnetUri(detailsUrl))
    }

    @Test
    fun `getMagnetUri fails when the page has no magnet`() = runTest {
        val provider = ZeroMagnet(fakeNetworkClient(detailsUrl to "<html><body><h1>404</h1></body></html>"))

        val result = runCatching { provider.getMagnetUri(detailsUrl) }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
