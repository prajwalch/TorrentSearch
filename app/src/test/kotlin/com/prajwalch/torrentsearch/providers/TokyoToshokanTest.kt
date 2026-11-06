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

class TokyoToshokanTest {
    private val searchUrl = "https://tokyotosho.info/search.php?terms=heroine&type=0&searchName=true"
    private val latestUrl = "https://tokyotosho.info/index.php"
    private val detailsUrl = "https://tokyotosho.info/details.php?id=1000670"

    @Test
    fun `search parses every result row pair`() = runTest {
        val provider = TokyoToshokan(fakeNetworkClient(searchUrl to fixture("tokyotoshokan/search.html")))

        val torrents = provider.search(query = "heroine", category = Category.All)

        assertEquals(
            listOf(
                "Officially Translated Light Novels",
                "! --THE iDOLM@STER Music (Lossless)-- !",
                // ownText() skips the <span class="s"> </span> word-break hints.
                "Makeine.Too.Many.Losing.Heroines.S01E01-E07.1080p.AMZN.WEB-DL.DDP2.0.H.264.DUAL-VARYG",
                // &amp; decoded.
                "[LowPower-Raws] To Be Hero & To Be Heroine (BD 720P x265 Ma10p AAC)",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("129.87 GB", "190.17 GB", "5.27 GB", "3.76 GB"), torrents.map { it.size })
        assertEquals(listOf(162u, 8u, 4u, 0u), torrents.map { it.seeders })
        assertEquals(listOf(45u, 4u, 0u, 3u), torrents.map { it.peers })
        assertEquals(
            listOf(
                "2025-02-23T15:02:00Z",
                "2024-12-25T23:23:00Z",
                "2024-09-16T12:43:00Z",
                "2021-01-29T18:16:00Z",
            ).map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        // /?cat=3, /?cat=2, /?cat=1, /?cat=7 (Raws).
        assertEquals(
            listOf(Category.Books, Category.Music, Category.Anime, Category.Anime),
            torrents.map { it.category },
        )
        assertEquals(
            listOf(
                "https://tokyotosho.info/details.php?id=1955363",
                "https://tokyotosho.info/details.php?id=1936867",
                "https://tokyotosho.info/details.php?id=1910517",
                "https://tokyotosho.info/details.php?id=1534069",
            ),
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(
            listOf(
                "https://nyaa.si/download/1939932.torrent",
                "https://nyaa.si/download/734186.torrent",
                "https://nyaa.si/download/1874134.torrent",
                "https://nyaa.si/download/1334989.torrent",
            ),
            torrents.map { it.fileDownloadLink },
        )
        val magnetUri = (torrents[0].magnetUri as MagnetUri.Available).value
        assertTrue(
            magnetUri,
            magnetUri.startsWith(
                "magnet:?xt=urn:btih:X34UZPE5JSQ7ZFCPS6UNYZWVJPV2TY2Z" +
                        "&tr=http%3A%2F%2Fnyaa.tracker.wf%3A7777%2Fannounce&tr=",
            ),
        )
        assertTrue(torrents.all { it.providerName == "TokyoToshokan" })
        assertEquals(4, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for a page without the listing table`() = runTest {
        val provider = TokyoToshokan(
            fakeNetworkClient(searchUrl to "<html><body><div id=\"main\"><p>No results.</p></div></body></html>"),
        )

        assertEquals(emptyList<Any>(), provider.search(query = "heroine", category = Category.All))
    }

    @Test
    fun `getLastestTorrents parses the index listing`() = runTest {
        val provider = TokyoToshokan(fakeNetworkClient(latestUrl to fixture("tokyotoshokan/latest.html")))

        val torrents = provider.getLastestTorrents(Category.All)

        assertEquals(
            listOf(
                "[Erai-raws] Rilakkuma - 25 [1080p CR WEB-DL AVC AAC][MultiSub][876A5E79].mkv",
                "[Onalrie] Rilakkuma - S01E25 [1080p WEBRip AV1].mkv",
                "[Erai-raws] Kami no Shizuku - 24 [1080p CR WEBRip HEVC AAC][MultiSub][4DCF3093].mkv",
                "[SubsPlease] Rilakkuma - 25 (480p) [5275DFF3].mkv",
            ),
            torrents.map { it.name },
        )
        // "Authorized: <span>No</span> Submitter: <a>ttsync</a> | Size: 90.22MB | ..."
        assertEquals(listOf("90.22 MB", "16.82 MB", "499.74 MB", "28.94 MB"), torrents.map { it.size })
        assertEquals(listOf(38u, 8u, 0u, 26u), torrents.map { it.seeders })
        assertEquals(listOf(8u, 2u, 0u, 4u), torrents.map { it.peers })
        assertEquals(
            listOf(
                "2026-09-19T08:15:00Z",
                "2026-09-19T07:33:00Z",
                "2026-09-19T02:15:00Z",
                "2026-09-19T01:20:00Z",
            ).map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        // The index page links categories as "?cat=1" (no leading slash), which
        // the parser's "/?cat=" prefix does not strip.
        assertTrue(torrents.all { it.category == Category.Other })
        assertEquals(
            listOf(
                "https://tokyotosho.info/details.php?id=2113509",
                "https://tokyotosho.info/details.php?id=2113495",
                "https://tokyotosho.info/details.php?id=2113462",
                "https://tokyotosho.info/details.php?id=2113451",
            ),
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(
            listOf(
                "https://nyaa.si/download/2163237.torrent",
                "https://onalrie.app/torrent/c1ccd750-054e-44aa-85ca-109c5e36e14c/download",
                "https://nyaa.si/download/2163203.torrent",
                "https://nyaa.si/view/2163235/torrent",
            ),
            torrents.map { it.fileDownloadLink },
        )
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = TokyoToshokan(fakeNetworkClient(detailsUrl to fixture("tokyotoshokan/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        // text() keeps the <span class="s"> </span> word-break hint as a space.
        assertEquals("[naisho] King of Prism by Pretty Rhythm [BD 720p AAC] .mkv", details.name)
        assertEquals("9215b21dbb22679a43b096cd71aaf781e1913834", details.infoHash)
        assertEquals(
            "magnet:?xt=urn:btih:SIK3EHN3EJTZUQ5QS3GXDKXXQHQZCOBU" +
                    "&tr=http%3A%2F%2Fopen.nyaatorrents.info%3A6544%2Fannounce" +
                    "&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80%2Fannounce",
            details.magnetUri,
        )
        assertEquals("http://www.nyaa.se/?page%3Ddownload%26amp%3Btid%3D825658", details.fileDownloadLink)
        assertEquals("1.71 GB", details.size)
        // "/scrape timed out (5s) or refused".
        assertNull(details.seeders)
        assertNull(details.peers)
        // "2016-07-01 00:21 UTC": the "hh" pattern lacks an AM/PM field, so
        // only the date part resolves.
        assertEquals(Instant.parse("2016-07-01T00:00:00Z"), details.uploadDate)
        assertEquals(Category.Anime, details.category)
        assertEquals("ShanaProject", details.uploader)
    }

    @Test
    fun `getDetails returns null when the page has no details list`() = runTest {
        val provider = TokyoToshokan(fakeNetworkClient(detailsUrl to "<html><body><div id=\"main\"></div></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }
}
