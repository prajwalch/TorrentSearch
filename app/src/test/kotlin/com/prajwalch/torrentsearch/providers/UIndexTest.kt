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

class UIndexTest {
    private val searchUrl = "https://uindex.org/search.php?search=the&c=0"
    private val topUrl = "https://uindex.org/top.php?t=24h&c=0"
    private val detailsUrl = "https://uindex.org/details.php?id=102020745"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = UIndex(fakeNetworkClient(searchUrl to fixture("uindex/search.html")))
        val before = Instant.now()

        val torrents = provider.search(query = "the", category = Category.All)

        assertEquals(
            listOf(
                "Them",
                "TheBigMac",
                "The_Rising_of_the_Shield_Hero_S04E08_1080p_WEBRip_AAC_EngSUB_H265...",
                // Highlighted <mark> fragments are joined into the full name.
                "The Neighborhood S08E17 Welcome to the Purse Dog 1080p AMZN WEB-DL DDP5 1 H 264-Kitsune",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("856.75 MB", "0.06 kB", "454.70 MB", "1.52 GB"), torrents.map { it.size })
        assertEquals(listOf(0u, 0u, 0u, 43u), torrents.map { it.seeders })
        assertEquals(listOf(0u, 0u, 0u, 28u), torrents.map { it.peers })
        assertEquals(
            listOf(Category.Games, Category.Other, Category.Anime, Category.Series),
            torrents.map { it.category },
        )
        assertEquals(
            listOf(
                "https://uindex.org/details.php?id=9549878",
                "https://uindex.org/details.php?id=306459",
                "https://uindex.org/details.php?id=14292342",
                "https://uindex.org/details.php?id=69646199",
            ),
            torrents.map { it.detailsPageUrl },
        )
        // Relative ("10 months ago", "1.1 hours ago") dates are all in the past.
        assertTrue(torrents.all { it.uploadDate != null && it.uploadDate!! < before })
        assertEquals(4, torrents.map { it.id }.toSet().size)

        val magnetUri = (torrents[0].magnetUri as MagnetUri.Available).value
        assertTrue(
            magnetUri,
            magnetUri.startsWith(
                "magnet:?xt=urn:btih:78DB3C47AC7D578D343C9F3BF33CABD6AC45B264" +
                        "&dn=Them%20%5B%20UIndex.org%20%5D&tr=udp://tracker.bittor.pw:1337/announce&tr=",
            ),
        )
    }

    @Test
    fun `getTopTorrents parses the top table with thousands separators`() = runTest {
        val provider = UIndex(fakeNetworkClient(topUrl to fixture("uindex/top.html")))

        val torrents = provider.getTopTorrents(Category.All)

        assertEquals(
            listOf(
                "Ted Lasso S04E01 1080p WEB H264-CAKES",
                "Ted Lasso S04E01 1080p HEVC x265-MeGusta",
                "X-Men 97 S02E08 1080p WEB H264-TheCuteness",
            ),
            torrents.take(3).map { it.name },
        )
        assertEquals(listOf(15584u, 7055u, 5734u), torrents.take(3).map { it.seeders })
        assertEquals(listOf(2757u, 3631u, 1320u), torrents.take(3).map { it.peers })
        assertEquals(listOf("3.10 GB", "573.00 MB", "1.60 GB"), torrents.take(3).map { it.size })
        assertEquals("https://uindex.org/details.php?id=155755936", torrents[0].detailsPageUrl)
        assertTrue(torrents.all { it.category == Category.Series })
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = UIndex(fakeNetworkClient(searchUrl to "<html><body><p>No results</p></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "the", category = Category.All))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = UIndex(fakeNetworkClient(detailsUrl to fixture("uindex/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals(
            "Ted Lasso S02E06 The Signal with Audio Description 1080p ATVP WEB-DL DDP5 1 Atmos H 264-Kitsune",
            details.name,
        )
        assertEquals("5936affbe6d2be4fd11cb936f935b2cf9ba61b16", details.infoHash)
        assertTrue(
            details.magnetUri,
            details.magnetUri.startsWith(
                "magnet:?xt=urn:btih:5936affbe6d2be4fd11cb936f935b2cf9ba61b16" +
                        "&dn=Ted+Lasso+S02E06+The+Signal+with+Audio+Description+1080p+ATVP+",
            ),
        )
        assertEquals("2.61 GB", details.size)
        assertEquals(184u, details.seeders)
        assertEquals(20u, details.peers)
        assertEquals(Instant.parse("2026-05-31T20:01:42Z"), details.uploadDate)
        assertEquals(Instant.parse("2026-08-24T02:54:38Z"), details.lastChecked)
        assertEquals(Category.Series, details.category)
        assertEquals("https://uindex.org/posters/tmdb/w500_uRHsiw1wLxPHFXkkv4Ix1s0O6f4.jpg", details.posterUrl)

        // html() pretty-prints <br> onto its own line; compare without layout newlines.
        val description = details.description?.replace("\n", "")
        assertNotNull(description)
        description!!
        assertTrue(description, description.startsWith("★★★ Downloaded from UIndex.org ★★★<br>Visit us"))
        assertTrue(description, description.contains("<br>File Size : 2.61 GB<br>Duration : 35 min 8 s<br>"))
        assertTrue(description, description.contains("<img src=\"http://lookpic.com/cdn/i2/t/"))
    }

    @Test
    fun `getDetails returns null when the page has no torrent`() = runTest {
        val provider = UIndex(fakeNetworkClient(detailsUrl to "<html><body><div id=\"content\"></div></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }
}
