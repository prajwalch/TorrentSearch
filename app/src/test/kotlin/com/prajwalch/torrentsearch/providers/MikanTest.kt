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

class MikanTest {
    private val searchUrl = "https://mikanani.me/Home/Search?searchstr=Bocchi%20the%20Rock%2001"
    private val detailsUrl = "https://mikanani.me/Home/Episode/a30771713530092565c4a1c2376abb65a912ca5f"

    // Tracker list shared by every magnet link on the fixtures (`&amp;` decoded).
    private val trackers =
        "&tr=http%3a%2f%2ft.nyaatracker.com%2fannounce" +
        "&tr=http%3a%2f%2ftracker.kamigami.org%3a2710%2fannounce" +
        "&tr=http%3a%2f%2fshare.camoe.cn%3a8080%2fannounce" +
        "&tr=http%3a%2f%2fopentracker.acgnx.se%2fannounce" +
        "&tr=http%3a%2f%2fanidex.moe%3a6969%2fannounce" +
        "&tr=http%3a%2f%2ft.acg.rip%3a6699%2fannounce" +
        "&tr=https%3a%2f%2ftr.bangumi.moe%3a9696%2fannounce" +
        "&tr=udp%3a%2f%2ftr.bangumi.moe%3a6969%2fannounce" +
        "&tr=http%3a%2f%2fopen.acgtracker.com%3a1096%2fannounce" +
        "&tr=udp%3a%2f%2ftracker.opentrackr.org%3a1337%2fannounce"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = Mikan(fakeNetworkClient(searchUrl to fixture("mikan/search.html")))

        val torrents = provider.search(query = "Bocchi%20the%20Rock%2001", category = Category.Anime)

        // Names are encoded as hexadecimal character references in the markup.
        assertEquals(
            listOf(
                "[7³ACG] 孤独摇滚！/Bocchi the Rock S01 | 01-12+SPx1 [简繁字幕] BDrip 1080p x265 OPUS 2.0",
                "[7³ACG] 孤独摇滚！/Bocchi the Rock S01 | 01-12+SPx1 [简繁字幕] BDrip 1080p AV1 OPUS 2.0",
                "【悠哈璃羽字幕组】[孤独摇滚_Bocchi the Rock][01-12 + SPs][BDRIP 1920x1080 HEVC-YUV420P10 FLAC][MKV 简繁外挂]",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("7.9 GB", "4.8 GB", "24.6 GB"), torrents.map { it.size })
        assertEquals(
            listOf("2026-06-09T08:40:00Z", "2026-06-07T10:29:00Z", "2025-07-12T19:22:00Z").map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        assertEquals(
            listOf(
                "https://mikanani.me/Home/Episode/2a0bf7fb281a04034b2003854feb46e65181e53a",
                "https://mikanani.me/Home/Episode/a30771713530092565c4a1c2376abb65a912ca5f",
                "https://mikanani.me/Home/Episode/eea983d11588e5eb18be5e969696adf120097fcb",
            ),
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(
            listOf(
                "https://mikanani.me/Download/20260609/2a0bf7fb281a04034b2003854feb46e65181e53a.torrent",
                "https://mikanani.me/Download/20260607/a30771713530092565c4a1c2376abb65a912ca5f.torrent",
                "https://mikanani.me/Download/20250712/eea983d11588e5eb18be5e969696adf120097fcb.torrent",
            ),
            torrents.map { it.fileDownloadLink },
        )
        assertEquals(
            listOf(
                "2a0bf7fb281a04034b2003854feb46e65181e53a",
                "a30771713530092565c4a1c2376abb65a912ca5f",
                "eea983d11588e5eb18be5e969696adf120097fcb",
            ).map { MagnetUri.Available("magnet:?xt=urn:btih:$it$trackers") },
            torrents.map { it.magnetUri },
        )
        assertEquals(listOf(null, null, null), torrents.map { it.seeders })
        assertTrue(torrents.all { it.category == Category.Anime })
        assertTrue(torrents.all { it.providerName == "Mikan" })
        assertEquals(3, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for a page without result rows`() = runTest {
        val provider = Mikan(fakeNetworkClient(searchUrl to "<html><body><table><tbody></tbody></table></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "Bocchi%20the%20Rock%2001", category = Category.Anime))
    }

    @Test
    fun `getDetails parses the episode page`() = runTest {
        val provider = Mikan(fakeNetworkClient(detailsUrl to fixture("mikan/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals(
            "[7³ACG] 孤独摇滚！/Bocchi the Rock S01 | 01-12+SPx1 [简繁字幕] BDrip 1080p AV1 OPUS 2.0 [4.8 GB]",
            details.name,
        )
        assertEquals("a30771713530092565c4a1c2376abb65a912ca5f", details.infoHash)
        assertEquals("magnet:?xt=urn:btih:a30771713530092565c4a1c2376abb65a912ca5f$trackers", details.magnetUri)
        assertEquals("4.8 GB", details.size)
        assertEquals(Instant.parse("2026-06-19T22:46:00Z"), details.uploadDate)
        assertEquals(Category.Anime, details.category)
        assertEquals(
            "https://mikanani.me/Download/20260619/a30771713530092565c4a1c2376abb65a912ca5f.torrent",
            details.fileDownloadLink,
        )
        assertEquals(
            "<a href=\"https://ktnbytes.com/2026/06/07/260605-bocchi-the-rock-s01-av1/kv.jpg\" target=\"_blank\"><img src=\"https://ktnbytes.com/2026/06/07/260605-bocchi-the-rock-s01-av1/kv.jpg\"></a>\n" +
            "<br>\n" +
            "<br>\n" +
            "post via api service\n" +
            "<br>\n" +
            "<br>\n" +
            "<font size=\"2\"><strong>Content Info</strong></font>\n" +
            "<br>\n" +
            "<br>\n" +
            "SUBS : Nest方案(KitaujiSub、Sakurato、Airota、DMG、Nekomoe-kissaten、UHA-WINGS、Netflix)\n" +
            "<br>\n" +
            "SIZE : 4.797 GiB\n" +
            "<br>\n" +
            "<br>\n" +
            "<font size=\"2\"><strong>Team Info</strong></font>\n" +
            "<br>\n" +
            "<br>\n" +
            "<a href=\"https://ktnbytes.com/\" target=\"_blank\">主發布站 : ktnbytes.com</a>\n" +
            "<br>\n" +
            "<a href=\"https://t.me/ktnbytes\" target=\"_blank\">RSS 頻道 : t.me/ktnbytes</a>\n" +
            "<br>\n" +
            "<a href=\"https://t.me/+lOZ13tUZHo8zZWE1\" target=\"_blank\">TG 交流群 : t.me/+lOZ13tUZHo8zZWE1</a>",
            details.description,
        )
    }

    @Test
    fun `getDetails returns null when the page has no episode title`() = runTest {
        val provider = Mikan(fakeNetworkClient(detailsUrl to "<html><body><p class=\"bangumi-info\"></p></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }
}
