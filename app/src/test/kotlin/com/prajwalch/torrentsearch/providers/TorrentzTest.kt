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

class TorrentzTest {
    private val searchUrl = "https://torrentz2.nz/search?q=ms+access"
    private val latestUrl = "https://torrentz2.nz/latest"
    private val detailsUrl = "https://torrentz2.nz/torrent/69dcc9c502685fd7b8019768"

    private val expectedMagnetUri = "magnet:?xt=urn:btih:618FE5B1B557181E547C8326C876642F3B4E2E7B" +
            "&dn=%5Bbitsearch.to%5D%20Angine%20de%20Poitrine%20-%20Vol.1" +
            "&tr=DHT&tr=udp%3A%2F%2Ftracker.bitsearch.to%3A1337%2Fannounce"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = Torrentz(fakeNetworkClient(searchUrl to fixture("torrentz/search.html")))

        val torrents = provider.search(query = "ms+access", category = Category.All)

        assertEquals(
            listOf(
                "ms-access-password-recovery-software-7.0.exe",
                "[ FreeCourseWeb.com ] Udemy - MS Access FORMS Series-3.zip",
                "Семенова И.И. - SQL стандарт в СУБД MS SQL SERVER, ORACLE, VFP И ACCESS " +
                        "манипулирование данными - 2008.pdf",
                "Программирование MS Office - Access - Excel - VBa",
                "[ DevCourseWeb.com ] Udemy - Data Processing and Analysis with MS EXCEL & MS ACCESS.zip",
            ),
            torrents.map { it.name },
        )
        assertEquals(
            listOf("846.46 KB", "2.35 GB", "535.99 KB", "509.57 MB", "548.02 MB"),
            torrents.map { it.size },
        )
        assertEquals(listOf(0u, 3u, 0u, 0u, 3u), torrents.map { it.seeders })
        assertEquals(listOf(1u, 0u, 0u, 0u, 1u), torrents.map { it.peers })
        // "Thu Jan 03 2019 15:19:49 GMT+0000 (Coordinated Universal Time)" style titles.
        assertEquals(
            listOf(
                "2019-01-03T15:19:49Z",
                "2021-07-27T18:47:39Z",
                "2018-10-28T01:13:41Z",
                "2019-01-05T02:31:56Z",
                "2021-03-04T05:08:06Z",
            ).map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        // "Other/Program", "Ebook/Course", "Other/Document", "Other" all fall back to Other.
        assertTrue(torrents.all { it.category == Category.Other })
        assertTrue(torrents.all { it.providerName == "Torrentz" })
        assertEquals(5, torrents.map { it.id }.toSet().size)

        val torrent = torrents[1]
        assertEquals("https://torrentz2.nz/torrent/610054cbbc8490874e73eab2", torrent.detailsPageUrl)
        assertEquals(
            MagnetUri.RequiresFetch("https://torrentz2.nz/torrent/610054cbbc8490874e73eab2"),
            torrent.magnetUri,
        )
    }

    @Test
    fun `search returns no results for a page without the results container`() = runTest {
        val provider = Torrentz(fakeNetworkClient(searchUrl to "<html><body><dl><dt>x</dt></dl></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "ms+access", category = Category.All))
    }

    @Test
    fun `latest parses ISO upload dates`() = runTest {
        val provider = Torrentz(fakeNetworkClient(latestUrl to fixture("torrentz/latest.html")))

        val torrents = provider.getLastestTorrents(category = Category.All)

        assertEquals(
            listOf(
                "Patty Waters - You Loved Me - 2022 (24-44)",
                "archive-the-scarlet-letter-1926-wealthxin.zip",
                "James Barr - Lords of the Desert",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("356.22 MB", "43.68 MB", "385.08 MB"), torrents.map { it.size })
        assertEquals(listOf(3u, 1u, 16u), torrents.map { it.seeders })
        assertEquals(listOf(0u, 0u, 1u), torrents.map { it.peers })
        assertEquals(
            listOf(
                "2026-04-16T19:23:56.900Z",
                "2026-04-16T19:23:56.899Z",
                "2026-04-16T19:23:42.893Z",
            ).map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        assertEquals("https://torrentz2.nz/torrent/69e1373e02685fd7b805be0b", torrents[2].detailsPageUrl)
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = Torrentz(fakeNetworkClient(detailsUrl to fixture("torrentz/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Angine de Poitrine - Vol.1", details.name)
        assertEquals("618fe5b1b557181e547c8326c876642f3b4e2e7b", details.infoHash)
        assertEquals("212.98 MB", details.size)
        assertEquals(60u, details.seeders)
        assertEquals(0u, details.peers)
        assertEquals(Instant.parse("2026-04-13T00:00:00Z"), details.uploadDate)
        assertEquals(Category.Other, details.category)
        // "&#x3D;" and "&amp;" inside the href must be decoded.
        assertEquals(expectedMagnetUri, details.magnetUri)
    }

    @Test
    fun `getDetails returns null when the page has no magnet link`() = runTest {
        val provider = Torrentz(
            fakeNetworkClient(detailsUrl to "<html><body><div class=\"download\"><h2>Gone</h2></div></body></html>"),
        )

        assertNull(provider.getDetails(detailsUrl))
    }

    @Test
    fun `getMagnetUri extracts the magnet link from the details page`() = runTest {
        val provider = Torrentz(fakeNetworkClient(detailsUrl to fixture("torrentz/details.html")))

        assertEquals(expectedMagnetUri, provider.getMagnetUri(detailsUrl))
    }
}
