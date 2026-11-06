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

class RutorTest {
    private val searchUrl = "https://rutor.info/search/0/9/010/2/linux"
    private val detailsUrl = "https://rutor.info/torrent/1105632/winpe-11-10-sergei-strelec-x64-08.09-2026-pc"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = Rutor(fakeNetworkClient(searchUrl to fixture("rutor/search.html")))

        val torrents = provider.search(query = "linux", category = Category.Apps)

        assertEquals(
            listOf(
                "КОМПАС-3D 23.0.3.2285 (2025) PC",
                "Acronis True Image 2021 Build 39287 (25.10.1.39287) (2021) PC | RePack by KpoJIuK",
                "AnyDesk 9.7.9 (2026) PC | + Portable",
                // Row without the comments column (name cell spans two columns).
                "WinPE 11-10-8 Sergei Strelec [x86/x64/Native x86] [09.07] (2026) PC",
            ),
            torrents.map { it.name },
        )
        // Sizes are separated by &nbsp; in the markup.
        assertEquals(listOf("10.51 GB", "442.92 MB", "7.95 MB", "4.86 GB"), torrents.map { it.size })
        assertEquals(listOf(76u, 50u, 31u, 13u), torrents.map { it.seeders })
        assertEquals(listOf(3u, 1u, 0u, 0u), torrents.map { it.peers })
        // Dates use &nbsp; separators and Russian month abbreviations.
        assertEquals(
            listOf(
                "2025-03-23T00:00:00Z",
                "2022-01-29T00:00:00Z",
                "2026-07-06T00:00:00Z",
                "2026-07-09T00:00:00Z",
            ).map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        assertEquals(
            listOf(
                "https://rutor.info/torrent/514828/kompas-3d-23.0.3.2285-2025-pc",
                "https://rutor.info/torrent/310919/acronis-true-image-2021-build-39287-25.10.1.39287-2021-pc-repack-by-kpojiuk",
                "https://rutor.info/torrent/703122/anydesk-9.7.9-2026-pc-portable",
                "https://rutor.info/torrent/1098187/winpe-11-10-8-sergei-strelec-x86/x64/native-x86-09.07-2026-pc",
            ),
            torrents.map { it.detailsPageUrl },
        )
        // Protocol-relative hrefs resolve against the page scheme.
        assertEquals(
            listOf(
                "https://d.rutor.info/download/514828",
                "https://d.rutor.info/download/310919",
                "https://d.rutor.info/download/703122",
                "https://d.rutor.info/download/1098187",
            ),
            torrents.map { it.fileDownloadLink },
        )
        assertEquals(
            listOf(
                "9988d2cea3c1e659a125b973ccaa8e6ec877431d",
                "2438db79811cf349576e271d0056c3e2c89d66ca",
                "76161c1c6018229422f0227ee71fb2ddc0a4a262",
                "cdc5cf05107262c89cea21327a15d69cd41e0ee3",
            ).map {
                MagnetUri.Available(
                    "magnet:?xt=urn:btih:$it&dn=rutor.info&tr=udp://opentor.net:6969&tr=http://retracker.local/announce",
                )
            },
            torrents.map { it.magnetUri },
        )
        assertTrue(torrents.all { it.category == Category.Apps })
        assertTrue(torrents.all { it.providerName == "Rutor" })
        assertEquals(4, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search leaves category empty when searching all categories`() = runTest {
        val allUrl = "https://rutor.info/search/0/0/010/2/linux"
        val provider = Rutor(fakeNetworkClient(allUrl to fixture("rutor/search.html")))

        val torrents = provider.search(query = "linux", category = Category.All)

        assertEquals(4, torrents.size)
        assertTrue(torrents.all { it.category == null })
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = Rutor(fakeNetworkClient(searchUrl to "<html><body><div id=\"index\"></div></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "linux", category = Category.Apps))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = Rutor(fakeNetworkClient(detailsUrl to fixture("rutor/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("WinPE 11-10 Sergei Strelec [x64] [08.09] (2026) PC", details.name)
        // &amp; in the href must be decoded.
        assertEquals(
            "magnet:?xt=urn:btih:db649e9f81f72fa6b3237ab2a808a47d5ae8c824" +
                    "&dn=rutor.info_WinPE+11-10+Sergei+Strelec+%5Bx64%5D+%5B08.09%5D+%282026%29+PC" +
                    "&tr=udp://opentor.net:6969&tr=http://retracker.local/announce",
            details.magnetUri,
        )
        assertEquals("db649e9f81f72fa6b3237ab2a808a47d5ae8c824", details.infoHash)
        assertEquals("3.50 GB", details.size)
        assertEquals(38u, details.seeders)
        assertEquals(0u, details.peers)
        assertEquals(Instant.parse("2026-09-09T06:49:39Z"), details.uploadDate)
        assertEquals(Instant.parse("2026-10-10T05:19:09Z"), details.lastChecked)
        assertEquals(Category.Apps, details.category)
        assertEquals("-=DoMiNo=-", details.uploader)
        assertEquals("https://d.rutor.info/download/1105632", details.fileDownloadLink)
        assertEquals(
            "https://i128.fastpic.org/big/2026/0909/de/06fcd19ab54ee6a83200357d67777fde.jpg",
            details.posterUrl,
        )
        // Screenshots come from HTML stored as text inside a <textarea>.
        assertEquals(
            listOf(
                "https://i128.fastpic.org/big/2026/0909/01/2f838d868f9911d0618141d7b0520201.jpg",
                "https://i127.fastpic.org/big/2026/0511/cd/12222c55db0ca640a94c3cc1bda84bcd.jpg",
            ),
            details.screenshotUrls,
        )
        // Poster and spoiler blocks are removed from the description.
        assertEquals(
            "<br>\n<br>\n<br>\n" +
                    "<b>Версия программы: </b>2026.09.08\n<br>\n" +
                    "<b>Официальный сайт: </b><a href=\"https://www.microsoft.com/ru-ru/\" target=\"_blank\">ссылка</a>\n<br>\n" +
                    "<b>Язык интерфейса: </b> Русский\n<br>\n<br>",
            details.description,
        )
    }

    @Test
    fun `getDetails returns null when the page has no torrent`() = runTest {
        val provider = Rutor(fakeNetworkClient(detailsUrl to "<html><body><div id=\"all\"></div></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }
}
