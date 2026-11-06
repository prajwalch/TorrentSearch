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

class EztvTest {
    private val searchUrl = "https://eztvx.to/search/1080p-ethel"
    private val detailsUrl = "https://eztvx.to/ep/1410019/pointless-s22e53-720p-web-h264-pfa/"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = Eztv(fakeNetworkClient(searchUrl to fixture("eztv/search.html")))

        val torrents = provider.search(query = "1080p-ethel", category = Category.Series)

        assertEquals(
            listOf(
                "Stumble S01E01 1080p WEB h264-ETHEL [eztv]",
                "Happys Place S02E01 1080p WEB h264-ETHEL [eztv]",
                "Boston Blue S01E04 1080p WEB h264-ETHEL [eztv]",
                "Fire Country S04E04 1080p WEB h264-ETHEL [eztv]",
                "Oahu Shores S01E03 1080p WEB h264-ETHEL [eztv]",
            ),
            torrents.map { it.name },
        )
        assertEquals(
            listOf("1.24 GB", "1.23 GB", "2.01 GB", "2.91 GB", "2.68 GB"),
            torrents.map { it.size },
        )
        // Seed counts are wrapped in <font> (or are "-"), so the cell has no own text.
        assertEquals(listOf<UInt?>(null, null, null, null, null), torrents.map { it.seeders })
        assertTrue(torrents.all { it.peers == 0u })
        assertTrue(torrents.all { it.category == Category.Series })
        assertTrue(torrents.all { it.providerName == "Eztv" })
        assertEquals(5, torrents.map { it.id }.toSet().size)

        val torrent = torrents[0]
        assertEquals(
            "https://eztvx.to/ep/2837260/stumble-s01e01-1080p-web-h264-ethel/",
            torrent.detailsPageUrl,
        )
        assertEquals(
            "https://zoink.ch/torrent/Stumble.S01E01.1080p.WEB.h264-ETHEL[eztvx.to].mkv.torrent",
            torrent.fileDownloadLink,
        )
        // HTML entities (&amp;) inside the href must be decoded.
        assertEquals(
            MagnetUri.Available(
                "magnet:?xt=urn:btih:5d2a8bf0c3e1f4a6b7c8d9e0f1a2b3c4d5e6f708" +
                        "&dn=Stumble.S01E01.1080p.WEB.h264-ETHEL%5Beztv%5D" +
                        "&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce",
            ),
            torrent.magnetUri,
        )
        assertEquals(
            "https://eztvx.to/ep/2805353/oahu-shores-s01e03-1080p-web-h264-ethel/",
            torrents[4].detailsPageUrl,
        )
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = Eztv(fakeNetworkClient(searchUrl to "<html><body><p>Nothing here</p></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "1080p-ethel", category = Category.Series))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = Eztv(fakeNetworkClient(detailsUrl to fixture("eztv/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Pointless S22E53 720p WEB h264-PFa EZTV", details.name)
        assertEquals("f7aec1eb059e71ce314b9f5587e03832134ade75", details.infoHash)
        assertEquals(
            "magnet:?xt=urn:btih:f7aec1eb059e71ce314b9f5587e03832134ade75" +
                    "&dn=pfa-pointless.s22e53.720p%5Beztv%5D.mkv%5Beztvx.to%5D" +
                    "&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce" +
                    "&tr=udp%3A%2F%2Fopen.stealth.si%3A80%2Fannounce" +
                    "&tr=udp%3A%2F%2Ftracker.torrent.eu.org%3A451%2Fannounce" +
                    "&tr=udp%3A%2F%2Ftracker.dler.org%3A6969%2Fannounce" +
                    "&tr=https%3A%2F%2Ftracker.moeblog.cn%3A443%2Fannounce" +
                    "&tr=https%3A%2F%2Ftracker.zhuqiy.com%3A443%2Fannounce" +
                    "&tr=udp%3A%2F%2Fopen.dstud.io%3A6969%2Fannounce",
            details.magnetUri,
        )
        // Size and release date are the text nodes following <b>Filesize:</b> / <b>Released:</b>.
        assertEquals("1.42 GB", details.size)
        assertEquals(Instant.parse("2020-01-23T00:00:00Z"), details.uploadDate)
        assertEquals(0u, details.seeders)
        assertEquals(0u, details.peers)
        assertEquals(Category.Series, details.category)
        assertEquals("https://eztvx.to/go.php?t=27eb242ab85ee1b4d3e448cbc41f0022", details.fileDownloadLink)
        assertEquals("https://eztvx.to/ezimg/thumbs/pointless-3083.jpg", details.posterUrl)
        assertEquals(
            "<div style=\"float: right; width: 280px;\">" +
                    "<a href=\"//ezimg.ch/thumbs/pointless-s22e53-720p-web-h264-pfa-large.jpg\" media=\"single\"" +
                    " class=\"pirobox\" title=\"Pointless S22E53 720p WEB h264-PFa EZTV\">" +
                    "<img src=\"//ezimg.ch/thumbs/pointless-s22e53-720p-web-h264-pfa-small.jpg\" width=\"280\"" +
                    " border=\"0\" alt=\"Pointless S22E53 720p WEB h264-PFa EZTV\"></a><br>" +
                    "<div style=\"-moz-border-radius: 10px; border-radius: 10px; background-color: #C8E2F9; padding: 7px;\">" +
                    "<b><i>Pointless S22E53 Screenshot</i></b></div></div>" +
                    "<div class=\"desWrap\">GOOD MOVIES KEEPS ROLLING<br><br>Pointless.S22E53.720p.WEB.h264-PFa<br><br>" +
                    "RELEASE INFO<br><br>Format: H264<br>FPS: 50<br>Audio: AAC</div>",
            details.description?.let(::normalizeHtml),
        )
        assertEquals(emptyList<String>(), details.screenshotUrls)
    }

    @Test
    fun `getDetails returns null when the page has no torrent`() = runTest {
        val provider = Eztv(fakeNetworkClient(detailsUrl to "<html><body></body></html>"))

        assertEquals(null, provider.getDetails(detailsUrl))
    }

    /** Removes whitespace around tags and collapses whitespace runs, ignoring pretty-printing. */
    private fun normalizeHtml(html: String): String =
        html.replace(Regex("""\s*(<[^>]+>)\s*"""), "$1").replace(Regex("""\s+"""), " ")
}
