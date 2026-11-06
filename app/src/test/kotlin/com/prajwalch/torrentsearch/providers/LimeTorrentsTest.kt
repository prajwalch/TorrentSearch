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

class LimeTorrentsTest {
    private val searchUrl = "https://limetorrents.fun/search/applications/ubuntu/date/1/"
    private val latestUrl = "https://limetorrents.fun/browse-torrents/Applications/"
    private val detailsUrl = "https://limetorrents.fun/ubuntu-21-10-desktop-amd64-torrent-16821848.html"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = LimeTorrents(fakeNetworkClient(searchUrl to fixture("limetorrents/search.html")))

        val before = Instant.now()
        val torrents = provider.search(query = "ubuntu", category = Category.Apps)
        val after = Instant.now()

        // ownText() collapses whitespace runs; double-escaped entities stay escaped once.
        assertEquals(
            listOf(
                "Clinton D., Negus C. Ubuntu Linux Bible 11ed 2025",
                "Celtics City S01E08 Chapter VIII Ubuntu 720p AMZN WEB-DL DDP5 1 H 264-RAWR EZTV",
                "ubuntu game pack-20 04-amd64",
                "Ubuntu Network Server --&gt; [ DevCourseWeb ]",
                "Bin U Mastering Ubuntu A Beginner&#039;s Guide 2023 [andryold1]",
            ),
            torrents.map { it.name },
        )
        assertEquals(
            listOf("8.16 MB", "1.77 GB", "3.72 GB", "268.78 MB", "23.14 MB"),
            torrents.map { it.size },
        )
        assertEquals(listOf(35u, 3u, 1u, 7u, 0u), torrents.map { it.seeders })
        assertEquals(listOf(0u, 2u, 1u, 1u, 0u), torrents.map { it.peers })
        // Raw categories ("Other", "TV shows", "Applications", "E-books") don't match the
        // provider's mapping keys, so all of them fall into Other.
        assertTrue(torrents.all { it.category == Category.Other })
        assertTrue(torrents.all { it.providerName == "LimeTorrents" })
        assertEquals(5, torrents.map { it.id }.toSet().size)

        // "9 months ago" and "1 Year+".
        assertRelativeDate(Duration.ofDays(270), before, after, torrents[0].uploadDate)
        torrents.drop(1).forEach { assertRelativeDate(Duration.ofDays(365), before, after, it.uploadDate) }

        val torrent = torrents[0]
        // The download link is taken verbatim (not resolved).
        assertEquals(
            "http://itorrents.net/torrent/232CD67EB3FFBD7C37BF9EC3EE887417E5AE1EE6.torrent" +
                    "?title=Clinton-D --Negus-C -Ubuntu-Linux-Bible-11ed-2025",
            torrent.fileDownloadLink,
        )
        val magnetUri = (torrent.magnetUri as MagnetUri.Available).value
        assertTrue(magnetUri, magnetUri.startsWith("magnet:?xt=urn:btih:232cd67eb3ffbd7c37bf9ec3ee887417e5ae1ee6&tr="))

        // Relative hrefs (containing spaces) are resolved against the page URL.
        assertEquals(
            listOf(
                "https://limetorrents.fun/Clinton-D --Negus-C -Ubuntu-Linux-Bible-11ed-2025-torrent-19390961.html",
                "https://limetorrents.fun/Celtics-City-S01E08-Chapter-VIII-Ubuntu-720p-AMZN-WEB-DL-DDP5-1-H-264-RAWR-EZTV-torrent-18930961.html",
                "https://limetorrents.fun/ubuntu-game-pack-20-04-amd64-torrent-18650518.html",
                "https://limetorrents.fun/Ubuntu-Network-Server-- gt -[-DevCourseWeb-]-torrent-18309920.html",
                "https://limetorrents.fun/Bin-U-Mastering-Ubuntu-A-Beginner  039 s-Guide-2023-[andryold1]-torrent-17669711.html",
            ),
            torrents.map { it.detailsPageUrl },
        )
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = LimeTorrents(fakeNetworkClient(searchUrl to "<html><body><p>Nothing here</p></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "ubuntu", category = Category.Apps))
    }

    @Test
    fun `getLastestTorrents falls back to the requested category`() = runTest {
        val provider = LimeTorrents(fakeNetworkClient(latestUrl to fixture("limetorrents/latest.html")))

        val before = Instant.now()
        val torrents = provider.getLastestTorrents(category = Category.Apps)
        val after = Instant.now()

        assertEquals(
            listOf(
                "Affinity by Canva 3 3 1 4940 (x64) Multilingual Crack",
                "CyberLink PowerDirector Ultimate 2027 v25 0 1007 0 Pre-Cracked {CracksHash}",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("190.53 MB", "741.73 MB"), torrents.map { it.size })
        assertEquals(listOf(5u, 16u), torrents.map { it.seeders })
        assertEquals(listOf(1u, 16u), torrents.map { it.peers })
        // The browse page has no "- in <category>" suffix.
        assertEquals(listOf(Category.Apps, Category.Apps), torrents.map { it.category })
        assertRelativeDate(Duration.ofHours(7), before, after, torrents[0].uploadDate)
        assertRelativeDate(Duration.ofHours(14), before, after, torrents[1].uploadDate)
        assertEquals(
            "https://limetorrents.fun/Affinity-by-Canva-3-3-1-4940-(x64)-Multilingual--Crack-torrent-20004617.html",
            torrents[0].detailsPageUrl,
        )
        val magnetUri = (torrents[0].magnetUri as MagnetUri.Available).value
        assertTrue(magnetUri, magnetUri.startsWith("magnet:?xt=urn:btih:ec0e8dd34430262af1376f89976e65ae0f92550a&tr="))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = LimeTorrents(fakeNetworkClient(detailsUrl to fixture("limetorrents/details.html")))

        val before = Instant.now()
        val details = provider.getDetails(detailsUrl)
        val after = Instant.now()

        assertNotNull(details)
        details!!
        assertEquals("ubuntu-21 10-desktop-amd64", details.name)
        assertEquals("f1fcdc1462d36530f526c1d9402eec9100b7ba18", details.infoHash)
        assertEquals("2.9 GB", details.size)
        assertEquals(50u, details.seeders)
        assertEquals(2u, details.peers)
        // "1 Year+ in Applications - Other OS"
        assertRelativeDate(Duration.ofDays(365), before, after, details.uploadDate)
        assertEquals(Category.Other, details.category)
        assertTrue(
            details.magnetUri,
            details.magnetUri.startsWith(
                "magnet:?xt=urn:btih:F1FCDC1462D36530F526C1D9402EEC9100B7BA18&dn=ubuntu-21+10-desktop-amd64" +
                        "&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce" +
                        "&tr=udp%3A%2F%2Ftracker.internetwarriors.net%3A1337%2Fannounce",
            ),
        )
        // The stray <br/> inside the info <table> is foster-parented before it, which
        // shifts the download area to the 7th child.
        assertEquals(
            "https://itorrents.net/torrent/F1FCDC1462D36530F526C1D9402EEC9100B7BA18.torrent" +
                    "?title=[LimeTorrents.fun]ubuntu-21.10-desktop-amd64",
            details.fileDownloadLink,
        )
    }

    @Test
    fun `getDetails returns null when the page has no torrent`() = runTest {
        val provider = LimeTorrents(fakeNetworkClient(detailsUrl to "<html><body><div id=\"content\"></div></body></html>"))

        assertEquals(null, provider.getDetails(detailsUrl))
    }

    private fun assertRelativeDate(age: Duration, before: Instant, after: Instant, actual: Instant?) {
        assertNotNull(actual)
        assertTrue("$actual", actual!! >= before.minus(age) && actual <= after.minus(age))
    }
}
