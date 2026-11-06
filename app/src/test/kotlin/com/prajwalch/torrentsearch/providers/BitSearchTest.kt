package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.testing.fakeNetworkClient
import com.prajwalch.torrentsearch.testing.fixture

import kotlinx.coroutines.test.runTest

import java.time.Duration
import java.time.Instant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BitSearchTest {
    private val detailsUrl = "https://bitsearch.to/torrent/5c36630591125666e6ee9b90"
    private val emptyResultsPage = "<html><body><main><p>No results found</p></main></body></html>"

    private fun searchUrl(page: Int) =
        "https://bitsearch.to/search?q=ubuntu&page=$page&sortBy=seeders&category=5"

    @Test
    fun `search parses and concatenates every result page`() = runTest {
        val provider = BitSearch(
            fakeNetworkClient(
                searchUrl(1) to fixture("bitsearch/search_page1.html"),
                searchUrl(2) to fixture("bitsearch/search_page2.html"),
                searchUrl(3) to emptyResultsPage,
                searchUrl(4) to emptyResultsPage,
                searchUrl(5) to emptyResultsPage,
            ),
        )

        val torrents = provider.search(query = "ubuntu", category = Category.Apps)

        assertEquals(
            listOf(
                "ubuntu-ultimate-1.4-dvd",
                "ubuntu-16.04.1-desktop-i386.iso",
                "ubuntu-14.10-desktop-amd64.iso",
                "ubuntu-10.04-desktop-i386.iso",
                "ubuntu-12.04-desktop-i386.iso",
            ),
            torrents.map { it.name },
        )
        assertEquals(
            listOf("1.94 GB", "1.43 GB", "1.08 GB", "699.44 MB", "701.29 MB"),
            torrents.map { it.size },
        )
        assertEquals(listOf(8u, 7u, 6u, 1u, 0u), torrents.map { it.seeders })
        assertEquals(listOf(1u, 0u, 0u, 0u, 0u), torrents.map { it.peers })
        assertEquals(
            listOf("2019-01-09", "2018-10-26", "2018-11-05", "2018-12-04", "2018-10-26")
                .map { Instant.parse("${it}T00:00:00Z") },
            torrents.map { it.uploadDate },
        )
        // "Softwares/Android".
        assertTrue(torrents.all { it.category == Category.Apps })
        assertTrue(torrents.all { it.providerName == "BitSearch" })
        assertEquals(5, torrents.map { it.id }.toSet().size)
        assertEquals(
            listOf(
                "FEBD9A2CB755EC82E6E7A015A8DC497FDE9DD507",
                "13D22EC551069369502A3100A99B991DD56389D4",
                "B415C913643E5FF49FE37D304BBB5E6E11AD5101",
                "3E16157F0879EB43E9E51F45D485FEFF90A77283",
                "BBB6DB69965AF769F664B6636E7914F8735141B3",
            ),
            torrents.map {
                (it.magnetUri as MagnetUri.Available).value
                    .removePrefix("magnet:?xt=urn:btih:")
                    .substringBefore('&')
            },
        )

        val torrent = torrents[0]
        // Relative hrefs must be resolved against the page URL.
        assertEquals("https://bitsearch.to/torrent/5c36630591125666e6ee9b90", torrent.detailsPageUrl)
        assertEquals(
            "https://bitsearch.to/download/torrent/FEBD9A2CB755EC82E6E7A015A8DC497FDE9DD507" +
                    "?title=ubuntu-ultimate-1.4-dvd",
            torrent.fileDownloadLink,
        )
        // Both &#x3D; and &amp; inside the href must be decoded.
        assertEquals(
            MagnetUri.Available(
                "magnet:?xt=urn:btih:FEBD9A2CB755EC82E6E7A015A8DC497FDE9DD507" +
                        "&dn=%5BBitsearch.to%5D%20ubuntu-ultimate-1.4-dvd" +
                        "&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce" +
                        "&tr=udp%3A%2F%2Fipv4.tracker.harry.lu%3A80%2Fannounce" +
                        "&tr=udp%3A%2F%2Ftracker.moeking.me%3A6969%2Fannounce" +
                        "&tr=udp%3A%2F%2Ftracker.bitsearch.to%3A1337%2Fannounce" +
                        "&tr=udp%3A%2F%2Ftracker.torrent.eu.org%3A451%2Fannounce",
            ),
            torrent.magnetUri,
        )
    }

    @Test
    fun `search returns no results when no page has results`() = runTest {
        val provider = BitSearch(
            fakeNetworkClient(*(1..5).map { searchUrl(it) to emptyResultsPage }.toTypedArray()),
        )

        assertEquals(emptyList<Any>(), provider.search(query = "ubuntu", category = Category.Apps))
    }

    @Test
    fun `latest torrents parse relative upload dates`() = runTest {
        val provider = BitSearch(
            fakeNetworkClient("https://bitsearch.to/latest?category=5" to fixture("bitsearch/latest.html")),
        )

        val before = Instant.now()
        val torrents = provider.getLastestTorrents(category = Category.Apps)
        val after = Instant.now()

        assertEquals(
            listOf(
                "Netflix free Cracker 2024 v17.3.57 - DailyApp",
                "Steinberg VST Live Pro 3.0.10 (x64)[FTUApps]",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("40.23 MB", "320.73 MB"), torrents.map { it.size })
        assertEquals(listOf(8688u, 1u), torrents.map { it.seeders })
        assertEquals(listOf(5785u, 0u), torrents.map { it.peers })
        // "Softwares".
        assertTrue(torrents.all { it.category == Category.Apps })
        // "7mo ago" does not match M/d/yyyy and is parsed as a relative date (7 * 30 days).
        val age = Duration.ofDays(210)
        torrents.forEach {
            assertNotNull(it.uploadDate)
            assertTrue(it.uploadDate!! in before.minus(age)..after.minus(age))
        }
        // The unencoded spaces of the relative href are kept when resolving it.
        assertEquals(
            "https://bitsearch.to/download/torrent/CD18941BD3D576CB84F5A27097EE5C428EA6EF08" +
                    "?title=Netflix free Cracker 2024 v17.3.57 - DailyApp",
            torrents[0].fileDownloadLink,
        )
        assertEquals("https://bitsearch.to/torrent/69900c8c090b11f759b6fd8a", torrents[0].detailsPageUrl)
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = BitSearch(fakeNetworkClient(detailsUrl to fixture("bitsearch/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("ubuntu-ultimate-1.4-dvd", details.name)
        assertEquals("febd9a2cb755ec82e6e7a015a8dc497fde9dd507", details.infoHash)
        assertEquals(
            "magnet:?xt=urn:btih:FEBD9A2CB755EC82E6E7A015A8DC497FDE9DD507" +
                    "&dn=%5Bbitsearch.to%5D%20ubuntu-ultimate-1.4-dvd" +
                    "&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce" +
                    "&tr=udp%3A%2F%2Fipv4.tracker.harry.lu%3A80%2Fannounce" +
                    "&tr=udp%3A%2F%2Ftracker.moeking.me%3A6969%2Fannounce" +
                    "&tr=udp%3A%2F%2Ftracker.bitsearch.to%3A1337%2Fannounce" +
                    "&tr=udp%3A%2F%2Ftracker.torrent.eu.org%3A451%2Fannounce",
            details.magnetUri,
        )
        assertEquals("1.94 GB", details.size)
        assertEquals(8u, details.seeders)
        assertEquals(1u, details.peers)
        assertEquals(Instant.parse("2019-01-09T00:00:00Z"), details.uploadDate)
        assertEquals(Instant.parse("2022-01-24T00:00:00Z"), details.lastChecked)
        assertEquals(Category.Apps, details.category)
        assertEquals(
            "https://bitsearch.to/download/torrent/FEBD9A2CB755EC82E6E7A015A8DC497FDE9DD507" +
                    "?title=ubuntu-ultimate-1.4-dvd",
            details.fileDownloadLink,
        )
    }

    @Test
    fun `getDetails returns null for an unexpected page layout`() = runTest {
        val provider = BitSearch(
            fakeNetworkClient(detailsUrl to "<html><body><h1>ubuntu-ultimate-1.4-dvd</h1></body></html>"),
        )

        assertEquals(null, provider.getDetails(detailsUrl))
    }
}
