package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.testing.fakeNetworkClient
import com.prajwalch.torrentsearch.testing.fixture

import kotlinx.coroutines.test.runTest

import java.time.Instant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileMoodTest {
    private val searchUrl = "https://filemood.com/result?q=ubuntu+in%3Atitle"
    private val detailsUrl =
        "https://filemood.com/ubuntu-26.04-live-server-amd64.iso-e1fc140a6391357fa1cf08ddb70274f9c05eb88b.html"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = FileMood(fakeNetworkClient(searchUrl to fixture("filemood/search.html")))

        val torrents = provider.search(query = "ubuntu", category = Category.Other)

        // The name is split into several <span>s; text() must join them without spaces.
        assertEquals(
            listOf(
                "ubuntu-26.04-desktop-amd64.iso",
                "ubuntu-26.04-live-server-amd64.iso",
                "ubuntu-24.04.4-desktop-amd64.iso",
                "ubuntu-26.04.1-desktop-amd64.iso",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("6.5 GB", "2.9 GB", "6.7 GB", "6.5 GB"), torrents.map { it.size })
        assertEquals(listOf(2161u, 1595u, 1477u, 1333u), torrents.map { it.seeders })
        assertEquals(listOf(24u, 11u, 30u, 69u), torrents.map { it.peers })
        assertTrue(torrents.all { it.category == Category.Other })
        assertTrue(torrents.all { it.providerName == "FileMood" })
        assertTrue(torrents.all { it.uploadDate == null })
        assertEquals(4, torrents.map { it.id }.toSet().size)

        val torrent = torrents[1]
        // Relative hrefs must be resolved against the page URL.
        assertEquals(detailsUrl, torrent.detailsPageUrl)
        val magnetUri = (torrent.magnetUri as MagnetUri.Available).value
        assertTrue(
            magnetUri,
            magnetUri.startsWith(
                "magnet:?xt=urn:btih:e1fc140a6391357fa1cf08ddb70274f9c05eb88b&tr=",
            ),
        )
        assertEquals(
            "https://filemood.com/ubuntu-24.04.4-desktop-amd64.iso-01c137287d6f0ed05a56742dae794f632c79ff3d.html",
            torrents[2].detailsPageUrl,
        )
    }

    @Test
    fun `search returns no results for a page without result rows`() = runTest {
        val provider = FileMood(
            fakeNetworkClient(searchUrl to "<html><body><table><tr><td>No results</td></tr></table></body></html>"),
        )

        assertEquals(emptyList<Any>(), provider.search(query = "ubuntu", category = Category.Other))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = FileMood(fakeNetworkClient(detailsUrl to fixture("filemood/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Ubuntu 26 04 live server amd64 iso", details.name)
        assertEquals("e1fc140a6391357fa1cf08ddb70274f9c05eb88b", details.infoHash)
        assertEquals("2.9 GB", details.size)
        assertEquals(Category.Other, details.category)
        // "Last Seen" is empty on the live page.
        assertEquals(null, details.lastChecked)
        assertTrue(
            details.magnetUri,
            details.magnetUri.startsWith("magnet:?xt=urn:btih:e1fc140a6391357fa1cf08ddb70274f9c05eb88b&tr="),
        )
    }

    @Test
    fun `getDetails parses the last seen date when present`() = runTest {
        val html = fixture("filemood/details.html")
            .replace("<p class=\"value\"><b></b></p>", "<p class=\"value\"><b>2026-10-09 12:34:56</b></p>")
        val provider = FileMood(fakeNetworkClient(detailsUrl to html))

        val details = provider.getDetails(detailsUrl)

        assertEquals(Instant.parse("2026-10-09T00:00:00Z"), details?.lastChecked)
    }

    @Test
    fun `getDetails returns null when the page has no torrent`() = runTest {
        val provider = FileMood(fakeNetworkClient(detailsUrl to "<html><body></body></html>"))

        assertEquals(null, provider.getDetails(detailsUrl))
    }
}
