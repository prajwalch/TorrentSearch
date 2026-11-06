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

class NyaaTest {
    private val searchUrl = "https://nyaa.si/?f=0&c=6_1&q=linux"
    private val detailsUrl = "https://nyaa.si/view/151471"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = Nyaa(fakeNetworkClient(searchUrl to fixture("nyaa/search.html")))

        val torrents = provider.search(query = "linux", category = Category.Apps)

        assertEquals(
            listOf(
                "[Linux] [UNIX] SoftwareDesign-soushuu-hen(2000-2009)",
                "Linux Mangaka Moe v4",
                "Linux Mangaka Moe v3",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("2.9 GiB", "3.4 GiB", "2.7 GiB"), torrents.map { it.size })
        assertEquals(listOf(0u, 0u, 0u), torrents.map { it.seeders })
        assertEquals(listOf(1u, 1u, 0u), torrents.map { it.peers })
        assertEquals(
            listOf(1292966880L, 1281885900L, 1275975360L).map(Instant::ofEpochSecond),
            torrents.map { it.uploadDate },
        )
        assertTrue(torrents.all { it.category == Category.Apps })
        assertTrue(torrents.all { it.providerName == "Nyaa" })
        assertEquals(3, torrents.map { it.id }.toSet().size)

        val torrent = torrents[1]
        // Relative hrefs must be resolved against the page URL.
        assertEquals("https://nyaa.si/view/151471", torrent.detailsPageUrl)
        assertEquals("https://nyaa.si/download/151471.torrent", torrent.fileDownloadLink)
        // HTML entities (&amp;) inside the href must be decoded.
        val magnetUri = (torrent.magnetUri as MagnetUri.Available).value
        assertTrue(
            magnetUri,
            magnetUri.startsWith(
                "magnet:?xt=urn:btih:ad83a84c69c38870934bc6e967fa18bf8e76a0ab" +
                        "&dn=Linux%20Mangaka%20Moe%20v4&tr=http%3A%2F%2Fnyaa.tracker.wf%3A7777%2Fannounce",
            ),
        )
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = Nyaa(fakeNetworkClient(searchUrl to "<html><body><p>Nothing here</p></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "linux", category = Category.Apps))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = Nyaa(fakeNetworkClient(detailsUrl to fixture("nyaa/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Linux Mangaka Moe v4", details.name)
        assertEquals("ad83a84c69c38870934bc6e967fa18bf8e76a0ab", details.infoHash)
        assertEquals("3.4 GiB", details.size)
        assertEquals(0u, details.seeders)
        assertEquals(1u, details.peers)
        assertEquals(Instant.ofEpochSecond(1281885900), details.uploadDate)
        assertEquals(Category.Apps, details.category)
        assertEquals("NyaaTorrents", details.uploader)
        assertEquals("https://nyaa.si/download/151471.torrent", details.fileDownloadLink)
        assertEquals(
            "http://animesoft.wordpress.com/2010/08/14/linux-mangaka-moe-v4-screenshots/",
            details.description,
        )
        assertTrue(details.magnetUri.startsWith("magnet:?xt=urn:btih:ad83a84c69c38870934bc6e967fa18bf8e76a0ab&dn="))
    }

    @Test
    fun `getDetails returns null when the page has no torrent`() = runTest {
        val provider = Nyaa(fakeNetworkClient(detailsUrl to "<html><body></body></html>"))

        assertEquals(null, provider.getDetails(detailsUrl))
    }
}
