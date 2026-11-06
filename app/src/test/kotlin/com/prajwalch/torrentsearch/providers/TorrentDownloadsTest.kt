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

class TorrentDownloadsTest {
    private val searchUrl = "https://torrentdownloads.pro/search/?s_cat=0&search=cockos+reaper"
    private val topUrl = "https://torrentdownloads.pro/view/popular/Software.html"
    private val detailsUrl = "https://torrentdownloads.pro/torrent/1002987/TeamViewer-Setup-exe"

    private val expectedMagnetUri = "magnet:?xt=urn:btih:A1319A295C114E442958615E014A64C63A30BBFB" +
            "&dn=TeamViewer+Setup+exe" +
            "&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969%2Fannounce" +
            "&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969%2Fannounce" +
            "&tr=udp%3A%2F%2F9.rarbg.to%3A2710%2Fannounce" +
            "&tr=udp%3A%2F%2Ftracker.pirateparty.gr%3A6969%2Fannounce" +
            "&tr=udp%3A%2F%2Ftracker.internetwarriors.net%3A1337%2Fannounce" +
            "&tr=udp%3A%2F%2Fp4p.arenabg.com%3A1337%2Fannounce" +
            "&tr=udp%3A%2F%2Fdenis.stalker.upeer.me%3A6969%2Fannounce" +
            "&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce" +
            "&tr=udp%3A%2F%2Fopen.demonii.si%3A1337%2Fannounce" +
            "&tr=udp%3A%2F%2Fbt1.archive.org%3A6969%2Fannounce"

    @Test
    fun `search parses the rows of the last results container`() = runTest {
        val provider = TorrentDownloads(fakeNetworkClient(searchUrl to fixture("torrentdownloads/search.html")))

        val torrents = provider.search(query = "cockos+reaper", category = Category.All)

        // The "Fast Search Results" box, the header row and the sponsored row are skipped.
        assertEquals(
            listOf(
                "Cockos - REAPER 5 1",
                "Cockos REAPER v5 61 + Cracked Mac Os X}",
                "Cockos REAPER v5 61 + Cracked For Mac",
                "Cockos_REAPER_v5 70_Incl_Patch_and_Keygen-R2R",
            ),
            torrents.map { it.name },
        )
        // "&nbsp;" between the number and the unit is normalized to a plain space.
        assertEquals(listOf("18.96 MB", "9.18 MB", "9.03 MB", "4.17 MB"), torrents.map { it.size })
        assertEquals(listOf(839u, 835u, 654u, 379u), torrents.map { it.seeders })
        assertEquals(listOf(554u, 555u, 433u, 249u), torrents.map { it.peers })
        assertEquals(
            listOf(Category.Apps, Category.Other, Category.Other, Category.Other),
            torrents.map { it.category },
        )
        assertEquals(
            listOf(
                "https://torrentdownloads.pro/torrent/1660956307/Cockos---REAPER-5-1",
                "https://torrentdownloads.pro/torrent/1666181716/Cockos-REAPER-v5-61-%2B-Cracked-Mac-Os-X%7D",
                "https://torrentdownloads.pro/torrent/1665233207/Cockos-REAPER-v5-61-%2B-Cracked-For-Mac",
                "https://torrentdownloads.pro/torrent/1664406709/Cockos-REAPER-v5-70-Incl-Patch-and-Keygen-R2R",
            ),
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(torrents.map { MagnetUri.RequiresFetch(it.detailsPageUrl!!) }, torrents.map { it.magnetUri })
        assertTrue(torrents.all { it.uploadDate == null })
        assertTrue(torrents.all { it.providerName == "TorrentDownloads" })
        assertEquals(4, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for a page without the results container`() = runTest {
        val provider = TorrentDownloads(
            fakeNetworkClient(searchUrl to "<html><body><div class=\"grey_bar3\"><p>x</p></div></body></html>"),
        )

        assertEquals(emptyList<Any>(), provider.search(query = "cockos+reaper", category = Category.All))
    }

    @Test
    fun `top torrents parses a listing page`() = runTest {
        val provider = TorrentDownloads(fakeNetworkClient(topUrl to fixture("torrentdownloads/top.html")))

        val torrents = provider.getTopTorrents(category = Category.Apps)

        // Listing pages have no sponsored row, but the parser still drops two leading rows,
        // so only the trailing rows are asserted here.
        assertEquals(
            listOf(
                "EaseUS Partition Master Technician 19 6 0 20250116 Full rar",
                "EZ CD Audio Converter 12 0 0 1 x64 + crack",
                "Helium Music Manager 17 3 283 Premium + patch",
                "VideoProc Converter AI v7 9 Portable Multilingua-BG",
            ),
            torrents.takeLast(4).map { it.name },
        )
        assertEquals(
            listOf("151.26 MB", "39.17 MB", "66.18 MB", "725.30 MB"),
            torrents.takeLast(4).map { it.size },
        )
        assertEquals(listOf(2u, 0u, 0u, 0u), torrents.takeLast(4).map { it.seeders })
        assertEquals(listOf(18u, 0u, 0u, 0u), torrents.takeLast(4).map { it.peers })
        assertTrue(torrents.all { it.category == Category.Apps })
        assertEquals(
            "https://torrentdownloads.pro/torrent/1705149589/EZ-CD-Audio-Converter-12-0-0-1-x64-%2B-crack",
            torrents.takeLast(3).first().detailsPageUrl,
        )
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = TorrentDownloads(fakeNetworkClient(detailsUrl to fixture("torrentdownloads/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("TeamViewer Setup exe", details.name)
        assertEquals("a1319a295c114e442958615e014a64c63a30bbfb", details.infoHash)
        assertEquals("1.41 MB", details.size)
        assertEquals(0u, details.seeders)
        assertEquals(0u, details.peers)
        assertEquals(Instant.parse("2009-08-28T17:57:50Z"), details.uploadDate)
        // "2025-02-15 06:04:10 (<a>Update Now</a>)": own text drops the link.
        assertEquals(Instant.parse("2025-02-15T06:04:10Z"), details.lastChecked)
        assertEquals(Category.Apps, details.category)
        assertEquals(expectedMagnetUri, details.magnetUri)
    }

    @Test
    fun `getDetails returns null when the page has no torrent`() = runTest {
        val provider = TorrentDownloads(
            fakeNetworkClient(detailsUrl to "<html><body><div class=\"inner_container\"></div></body></html>"),
        )

        assertNull(provider.getDetails(detailsUrl))
    }

    @Test
    fun `getMagnetUri extracts the magnet link from the details page`() = runTest {
        val provider = TorrentDownloads(fakeNetworkClient(detailsUrl to fixture("torrentdownloads/details.html")))

        assertEquals(expectedMagnetUri, provider.getMagnetUri(detailsUrl))
    }
}
