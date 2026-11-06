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

/**
 * Fixtures are hand-written with neutral placeholder content that mirrors
 * the markup of sukebei.nyaa.si (same NyaaV2 templates as nyaa.si).
 */
class SukebeiTest {
    private val searchUrl = "https://sukebei.nyaa.si/?f=0&c=0_0&q=sample"
    private val detailsUrl = "https://sukebei.nyaa.si/view/4000002"

    private val trackers = "&tr=http%3A%2F%2Fsukebei.tracker.wf%3A8888%2Fannounce" +
            "&tr=udp%3A%2F%2Fopen.stealth.si%3A80%2Fannounce" +
            "&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = Sukebei(fakeNetworkClient(searchUrl to fixture("sukebei/search.html")))

        val torrents = provider.search(query = "sample", category = Category.Porn)

        assertEquals(
            listOf(
                "[Placeholder] Sample Release 01 [1080p]",
                // Second row has a comments link before the name link.
                "Sample Collection & Extras Vol.2",
                "Placeholder Artbook <Deluxe>",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("1.2 GiB", "734.5 MiB", "88.0 MiB"), torrents.map { it.size })
        assertEquals(listOf(15u, 0u, 1024u), torrents.map { it.seeders })
        assertEquals(listOf(3u, 7u, 0u), torrents.map { it.peers })
        assertEquals(
            listOf(1700000000L, 1700003600L, 1700007200L).map(Instant::ofEpochSecond),
            torrents.map { it.uploadDate },
        )
        assertEquals(
            listOf(
                "https://sukebei.nyaa.si/view/4000001",
                "https://sukebei.nyaa.si/view/4000002",
                "https://sukebei.nyaa.si/view/4000003",
            ),
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(
            listOf(
                "https://sukebei.nyaa.si/download/4000001.torrent",
                "https://sukebei.nyaa.si/download/4000002.torrent",
                "https://sukebei.nyaa.si/download/4000003.torrent",
            ),
            torrents.map { it.fileDownloadLink },
        )
        // HTML entities (&amp;) inside the href must be decoded.
        assertEquals(
            listOf(
                "magnet:?xt=urn:btih:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa01" +
                        "&dn=%5BPlaceholder%5D%20Sample%20Release%2001%20%5B1080p%5D$trackers",
                "magnet:?xt=urn:btih:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa02" +
                        "&dn=Sample%20Collection%20%26%20Extras%20Vol.2$trackers",
                "magnet:?xt=urn:btih:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa03" +
                        "&dn=Placeholder%20Artbook%20%3CDeluxe%3E$trackers",
            ).map(MagnetUri::Available),
            torrents.map { it.magnetUri },
        )
        assertTrue(torrents.all { it.category == Category.Porn })
        assertTrue(torrents.all { it.providerName == "Sukebei" })
        assertEquals(3, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `latest torrents resolve links against the bare site url`() = runTest {
        val provider = Sukebei(fakeNetworkClient("https://sukebei.nyaa.si" to fixture("sukebei/search.html")))

        val torrents = provider.getLastestTorrents(Category.Porn)

        assertEquals(3, torrents.size)
        assertEquals("https://sukebei.nyaa.si/view/4000001", torrents.first().detailsPageUrl)
        assertEquals("https://sukebei.nyaa.si/download/4000001.torrent", torrents.first().fileDownloadLink)
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = Sukebei(fakeNetworkClient(searchUrl to "<html><body><h3>No results found</h3></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "sample", category = Category.Porn))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = Sukebei(fakeNetworkClient(detailsUrl to fixture("sukebei/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Sample Collection & Extras Vol.2", details.name)
        assertEquals(
            "magnet:?xt=urn:btih:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa02" +
                    "&dn=Sample%20Collection%20%26%20Extras%20Vol.2$trackers",
            details.magnetUri,
        )
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa02", details.infoHash)
        assertEquals("734.5 MiB", details.size)
        assertEquals(0u, details.seeders)
        assertEquals(7u, details.peers)
        assertEquals(Instant.ofEpochSecond(1700003600), details.uploadDate)
        assertEquals(Category.Porn, details.category)
        assertEquals("placeholder_uploader", details.uploader)
        assertEquals("https://sukebei.nyaa.si/download/4000002.torrent", details.fileDownloadLink)
        // html() collapses the raw markdown newlines and re-escapes entities.
        assertEquals(
            "## Placeholder notes Encoded with sample settings &amp; checked twice. Runtime &lt; 2h",
            details.description,
        )
    }

    @Test
    fun `getDetails returns null when the page has no torrent`() = runTest {
        val provider = Sukebei(fakeNetworkClient(detailsUrl to "<html><body><div class=\"container\"></div></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }
}
