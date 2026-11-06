package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.testing.fakeNetworkClient
import com.prajwalch.torrentsearch.testing.fixture
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.test.runTest

import java.time.Instant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Bt4gTest {
    private val searchUrl = "https://bt4gprx.com/search?q=nickel boys&category=movie&orderby=seeders&p=1"
    private val detailsUrl = "https://bt4gprx.com/magnet/j2goCsO2vfKb613EKOXeRzlfffVhB1tbC"
    private val infoHash = "4fe13cbd0dfa3b5f1f9a5e3c1c2f0a8d6b7e9c21"

    @Test
    fun `search parses every result item`() = runTest {
        val provider = Bt4g(fakeNetworkClient(searchUrl to fixture("bt4g/search.html")))

        val torrents = provider.search(query = "nickel boys", category = Category.Movies)

        assertEquals(
            listOf(
                "Nickel Boys (2024) [1080p] [WEBRip] [5.1] [YTS.MX]",
                // text() collapses the runs of spaces of the anchor text.
                "www.UIndex.org - Nickel.Boys.2024.MULTi.1080p.WEB.DD5.1.H265-SiC",
                "Nickel.Boys.2024.1080p.WEB.H264-SmilingAbidingSeriemaFromEldorado",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf(133u, 101u, 40u), torrents.map { it.seeders })
        assertEquals(listOf(18u, 0u, 0u), torrents.map { it.peers })
        // "Creation Time:&nbsp;2025-02-11": the non-breaking space must be
        // normalised for the "Creation Time: " prefix to be removed.
        assertEquals(
            listOf("2025-02-11T00:00:00Z", "2026-06-26T00:00:00Z", "2025-02-11T00:00:00Z").map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        // The size selector (`span:nth-child(5) > b`) points at the "Seeders"
        // span of the current layout, which has no <b> child.
        assertEquals(listOf(null, null, null), torrents.map { it.size })
        assertTrue(torrents.all { it.category == Category.Movies })
        assertTrue(torrents.all { it.providerName == "BT4G" })

        val slugs = listOf(
            "j2goCsO2vfKb613EKOXeRzlfffVhB1tbC",
            "V0ubHyyerz5NiL2gJf1EpeXCjWoaQJ4OH",
            "TElg544K4j1dR0FuZXLN0ibIKTIDqjm0",
        )
        val expectedDetailsPageUrls = slugs.map { "https://bt4gprx.com/magnet/$it" }
        assertEquals(expectedDetailsPageUrls, torrents.map { it.detailsPageUrl })
        assertEquals(expectedDetailsPageUrls.map(MagnetUri::RequiresFetch), torrents.map { it.magnetUri })
        assertEquals(
            slugs.map { TorrentUtils.createTorrentId(providerId = "bt4g", sourceId = it) },
            torrents.map { it.id },
        )
    }

    @Test
    fun `search returns no results for a page without result items`() = runTest {
        val provider = Bt4g(fakeNetworkClient(searchUrl to "<html><body><p>No results</p></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "nickel boys", category = Category.Movies))
    }

    @Test
    fun `getLastestTorrents parses the listing with the results parser`() = runTest {
        val provider = Bt4g(fakeNetworkClient("https://bt4gprx.com/new" to fixture("bt4g/search.html")))

        val torrents = provider.getLastestTorrents(Category.All)

        assertEquals(listOf(133u, 101u, 40u), torrents.map { it.seeders })
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = Bt4g(fakeNetworkClient(detailsUrl to fixture("bt4g/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Nickel Boys (2024) [1080p] [WEBRip] [5.1] [YTS.MX]", details.name)
        assertEquals(infoHash, details.infoHash)
        assertEquals(TorrentUtils.createMagnetUri(infoHash), details.magnetUri)
        assertEquals("2.58 GB", details.size)
        assertEquals(133u, details.seeders)
        assertEquals(18u, details.peers)
        assertEquals(Instant.parse("2025-02-11T00:00:00Z"), details.uploadDate)
        assertEquals(Category.Movies, details.category)
        // "a long while ago" is not a parsable relative time.
        assertNull(details.lastChecked)
    }

    @Test
    fun `getDetails returns null when the page has no magnet link`() = runTest {
        val provider = Bt4g(fakeNetworkClient(detailsUrl to "<html><body></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }

    @Test
    fun `getMagnetUri builds the magnet from the info hash of the details page`() = runTest {
        val provider = Bt4g(fakeNetworkClient(detailsUrl to fixture("bt4g/details.html")))

        assertEquals(TorrentUtils.createMagnetUri(infoHash), provider.getMagnetUri(detailsUrl))
    }
}
