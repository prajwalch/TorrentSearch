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

class LinuxTrackerTest {
    private val searchUrl = "https://linuxtracker.org/index.php?page=torrents&search=ubuntu&category=0&active=0"
    private val detailsUrl = "https://linuxtracker.org/torrents/390796f33cee8a407432f199ec06a5849d6ecce3/"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = LinuxTracker(fakeNetworkClient(searchUrl to fixture("linuxtracker/search.html")))

        val torrents = provider.search(query = "ubuntu", category = Category.Apps)

        assertEquals(
            listOf(
                "ubuntustudio-26.04.1-desktop-amd64.iso",
                "xubuntu-26.04.1-desktop-amd64.iso",
                "xubuntu-26.10-beta-minimal-amd64.iso",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("6.6 GB", "4.8 GB", "3.2 GB"), torrents.map { it.size })
        assertEquals(listOf(7u, 8u, 6u), torrents.map { it.seeders })
        assertEquals(listOf(0u, 0u, 0u), torrents.map { it.peers })
        assertEquals(
            listOf("2026-10-04T00:00:00Z", "2026-10-04T00:00:00Z", "2026-10-02T00:00:00Z").map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        assertTrue(torrents.all { it.category == Category.Apps })
        assertTrue(torrents.all { it.providerName == "LinuxTracker" })
        assertEquals(3, torrents.map { it.id }.toSet().size)

        // Relative hrefs must be resolved against the page URL.
        val detailsPageUrls = listOf(
            "https://linuxtracker.org/torrents/d738c3042effc632f5dc224c1c319d753ab8fee1/",
            "https://linuxtracker.org/torrents/f27a4e4455123e71d411b3941a7d68dfb098354f/",
            "https://linuxtracker.org/torrents/586f8b40b1f046e8781bc39789bfa791e4dae4e3/",
        )
        assertEquals(detailsPageUrls, torrents.map { it.detailsPageUrl })
        assertEquals(detailsPageUrls.map(MagnetUri::RequiresFetch), torrents.map { it.magnetUri })
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = LinuxTracker(fakeNetworkClient(searchUrl to "<html><body><p>Nothing here</p></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "ubuntu", category = Category.Apps))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val html = fixture("linuxtracker/details.html")
        val provider = LinuxTracker(fakeNetworkClient(detailsUrl to html))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("ubuntu-budgie-26.10-snapshot1-desktop-amd64.iso", details.name)
        assertEquals("390796f33cee8a407432f199ec06a5849d6ecce3", details.infoHash)
        // &amp; inside the href must be decoded.
        assertEquals(
            "magnet:?xt=urn:btih:390796F33CEE8A407432F199EC06A5849D6ECCE3" +
                    "&dn=ubuntu-budgie-26.10-snapshot1-desktop-amd64.iso",
            details.magnetUri,
        )
        assertEquals("3.8 GB", details.size)
        assertEquals(3u, details.seeders)
        assertEquals(0u, details.peers)
        assertEquals(Instant.parse("2026-06-08T16:55:00Z"), details.uploadDate)
        assertEquals(Category.Apps, details.category)
        assertEquals(
            "https://linuxtracker.org/torrents/390796f33cee8a407432f199ec06a5849d6ecce3/download/",
            details.fileDownloadLink,
        )
        assertEquals(
            listOf(
                "https://linuxtracker.org/torrents/media/torrent-images/390796f33cee8a407432f199ec06a5849d6ecce3/" +
                        "s1390796f33cee8a407432f199ec06a5849d6ecce3.png",
            ),
            details.screenshotUrls,
        )
        // The description's inner markup, as it appears in the page.
        val expectedDescription = html
            .substringAfter("<div class=\"torrent-description formatted-content\">")
            .substringBefore("</div>")
        assertTrue(expectedDescription.startsWith("<strong>THIS IS NOT THE FINAL RELEASE"))
        assertEquals(normalizeHtml(expectedDescription), details.description?.let(::normalizeHtml))
    }

    @Test
    fun `getDetails returns null when the page has no torrent`() = runTest {
        val provider = LinuxTracker(fakeNetworkClient(detailsUrl to "<html><body></body></html>"))

        assertEquals(null, provider.getDetails(detailsUrl))
    }

    @Test
    fun `getMagnetUri extracts the magnet from the details page`() = runTest {
        val provider = LinuxTracker(fakeNetworkClient(detailsUrl to fixture("linuxtracker/details.html")))

        assertEquals(
            "magnet:?xt=urn:btih:390796F33CEE8A407432F199EC06A5849D6ECCE3" +
                    "&dn=ubuntu-budgie-26.10-snapshot1-desktop-amd64.iso",
            provider.getMagnetUri(detailsUrl),
        )
    }

    /** Removes whitespace around tags and collapses whitespace runs, ignoring pretty-printing. */
    private fun normalizeHtml(html: String): String =
        html.replace(Regex("""\s*(<[^>]+>)\s*"""), "$1").replace(Regex("""\s+"""), " ")
}
