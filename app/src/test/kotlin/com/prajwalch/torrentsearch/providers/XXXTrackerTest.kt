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

// XXXTracker is an adult provider, so its fixtures are hand-written with neutral
// placeholder content that mirrors the markup structure the selectors expect.
class XXXTrackerTest {
    private val searchUrl = "https://xxxtor.com/b.php?search=sample"
    private val detailsUrl = "https://xxxtor.com/torrent/101/sample-video-one"

    @Test
    fun `search parses every result row and skips header and ad rows`() = runTest {
        val provider = XXXTracker(fakeNetworkClient(searchUrl to fixture("xxxtracker/search.html")))

        val torrents = provider.search(query = "sample", category = Category.Porn)

        assertEquals(
            listOf("Sample Video One", "Sample & Clip Two", "Sample Collection Three"),
            torrents.map { it.name },
        )
        assertEquals(listOf("1.37 GB", "700.5 MB", "4.20 GB"), torrents.map { it.size })
        assertEquals(listOf(12u, 4u, 0u), torrents.map { it.seeders })
        assertEquals(listOf(3u, 0u, 7u), torrents.map { it.peers })
        assertEquals(
            listOf("2026-10-05T00:00:00Z", "2026-05-28T00:00:00Z", "2025-12-31T00:00:00Z")
                .map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        assertEquals(
            listOf(
                "https://xxxtor.com/torrent/101/sample-video-one",
                "https://xxxtor.com/torrent/102/sample-clip-two",
                "https://xxxtor.com/torrent/103/sample-collection-three",
            ),
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(
            listOf(
                "https://xxxtor.com/download/101",
                "https://xxxtor.com/download/102",
                "https://xxxtor.com/download/103",
            ),
            torrents.map { it.fileDownloadLink },
        )
        assertEquals(
            MagnetUri.Available(
                "magnet:?xt=urn:btih:0f1e2d3c4b5a69788796a5b4c3d2e1f00f1e2d3c" +
                        "&dn=Sample+%26+Clip+Two&tr=udp%3A%2F%2Ftracker.example.org%3A1337%2Fannounce",
            ),
            torrents[1].magnetUri,
        )
        assertTrue(torrents.all { it.category == Category.Porn })
        assertTrue(torrents.all { it.providerName == "XXXTracker" })
        assertEquals(3, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `top torrents reuse the results page parser`() = runTest {
        val provider = XXXTracker(fakeNetworkClient("https://xxxtor.com/top" to fixture("xxxtracker/search.html")))

        val torrents = provider.getTopTorrents(category = Category.Porn)

        assertEquals(
            listOf("Sample Video One", "Sample & Clip Two", "Sample Collection Three"),
            torrents.map { it.name },
        )
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = XXXTracker(fakeNetworkClient(searchUrl to "<html><body><p>Ничего не найдено</p></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "sample", category = Category.Porn))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = XXXTracker(fakeNetworkClient(detailsUrl to fixture("xxxtracker/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Sample Video One", details.name)
        assertEquals("a1b2c3d4e5f60718293a4b5c6d7e8f9012345678", details.infoHash)
        assertEquals(
            "magnet:?xt=urn:btih:A1B2C3D4E5F60718293A4B5C6D7E8F9012345678" +
                    "&dn=Sample+Video+One&tr=udp%3A%2F%2Ftracker.example.org%3A1337%2Fannounce",
            details.magnetUri,
        )
        assertEquals("1.37 GB", details.size)
        assertEquals(12u, details.seeders)
        assertEquals(3u, details.peers)
        assertEquals(Instant.parse("2026-10-05T00:00:00Z"), details.uploadDate)
        assertEquals(Category.Porn, details.category)
        assertEquals("https://xxxtor.com/download/101", details.fileDownloadLink)
        assertEquals("https://img.example.org/posters/101.jpg", details.posterUrl)
        // Poster and the two line breaks after it are stripped from the description;
        // html() pretty-prints the remaining <br> onto its own line.
        assertEquals(
            "Neutral placeholder description with <b>bold</b> text.\n<br>\nSecond line &amp; more.",
            details.description,
        )
    }

    @Test
    fun `getDetails returns null when the page has no torrent`() = runTest {
        val provider = XXXTracker(fakeNetworkClient(detailsUrl to "<html><body><div id=\"content\"></div></body></html>"))

        assertEquals(null, provider.getDetails(detailsUrl))
    }
}
