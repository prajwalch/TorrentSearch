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

class DmhyTest {
    private val searchUrl =
        "https://share.dmhy.org/topics/list?keyword=one piece&sort_id=2&team_id=0&order=date-desc"
    private val detailsUrl =
        "https://share.dmhy.org/topics/view/727909_Skymoon-Raws_One_Piece_1180_ViuTV_WEB-RIP_CHT_SRT_1080p_MKV.html"

    private val magnetTrackers =
        "&dn=&tr=http%3A%2F%2F104.143.10.61%3A8000%2Fannounce&tr=udp%3A%2F%2F104.143.10.61%3A8000%2Fannounce"
    private val lastMagnetTracker = "&tr=udp%3A%2F%2Ftracker4.itzmx.com%3A2710%2Fannounce"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = Dmhy(fakeNetworkClient(searchUrl to fixture("dmhy/search.html")))

        val torrents = provider.search(query = "one piece", category = Category.Anime)

        // The name is the anchor's own text, so the highlighted
        // <span class="keyword"> children are left out.
        assertEquals(
            listOf(
                "[Skymoon-Raws][ 海賊王][1180][ViuTV][WEB-RIP][CHT][SRT][1080p][MKV]",
                "[Skymoon-Raws][ 海賊王][1179][ViuTV][WEB-RIP][CHT][SRT][1080p][MKV]",
                "海贼王外传动画：女英雄们的故事. HEROINES 2026 SUBFRENCH 1080p ADN WEB-DL AAC2.0 x264-Tsundere-Raws.mkv",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("457.9 MB", "463.2 MB", "553.6 MB"), torrents.map { it.size })
        assertEquals(listOf(9u, 7u, null), torrents.map { it.seeders })
        assertEquals(listOf(1u, 0u, null), torrents.map { it.peers })
        assertEquals(
            listOf("2026-09-28T16:47:00Z", "2026-09-21T16:48:00Z", "2026-07-08T22:26:00Z").map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        assertTrue(torrents.all { it.category == Category.Anime })
        assertTrue(torrents.all { it.providerName == "Dmhy" })
        assertEquals(
            listOf(
                detailsUrl,
                "https://share.dmhy.org/topics/view/727506_Skymoon-Raws_One_Piece_1179_ViuTV_WEB-RIP_CHT_SRT_1080p_MKV.html",
                "https://share.dmhy.org/topics/view/" +
                        "722320_ONE_PIECE_HEROINES_2026_SUBFRENCH_1080p_ADN_WEB-DL_AAC2_0_x264-Tsundere-Raws_mkv.html",
            ),
            torrents.map { it.detailsPageUrl },
        )

        val infoHashes = listOf(
            "LJDJSTRGCAW5PUOER34MCTSCQLPWD4XG",
            "VWOE6SU55B373U4CT3RBHQEW6X24BEBC",
            "JBWAEJ3RJDJMYBGEP462MXWVW47X7H4L",
        )
        val lastTrackers = listOf(
            lastMagnetTracker,
            lastMagnetTracker,
            "$lastMagnetTracker&tr=http%3A%2F%2Fnyaa.tracker.wf%3A7777%2Fannounce",
        )
        torrents.indices.forEach { index ->
            val torrent = torrents[index]
            val infoHash = infoHashes[index]
            val magnetUri = (torrent.magnetUri as MagnetUri.Available).value
            assertTrue(magnetUri, magnetUri.startsWith("magnet:?xt=urn:btih:$infoHash$magnetTrackers"))
            assertTrue(magnetUri, magnetUri.endsWith(lastTrackers[index]))
            assertEquals(
                TorrentUtils.createTorrentId(providerId = "dmhy", sourceId = infoHash.lowercase()),
                torrent.id,
            )
        }
    }

    @Test
    fun `search returns no results for a page without the topic table`() = runTest {
        val provider = Dmhy(fakeNetworkClient(searchUrl to "<html><body><p>沒有可顯示資源</p></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "one piece", category = Category.Anime))
    }

    @Test
    fun `getLastestTorrents parses the listing with the results parser`() = runTest {
        val provider = Dmhy(
            fakeNetworkClient("https://share.dmhy.org/topics/list/sort_id/2" to fixture("dmhy/search.html")),
        )

        val torrents = provider.getLastestTorrents(Category.Anime)

        assertEquals(listOf("457.9 MB", "463.2 MB", "553.6 MB"), torrents.map { it.size })
    }

    @Test
    fun `getDetails parses the topic page`() = runTest {
        val provider = Dmhy(fakeNetworkClient(detailsUrl to fixture("dmhy/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("[Skymoon-Raws][One Piece 海賊王][1180][ViuTV][WEB-RIP][CHT][SRT][1080p][MKV]", details.name)
        assertEquals("ljdjstrgcaw5puoer34mctscqlpwd4xg", details.infoHash)
        assertTrue(
            details.magnetUri,
            details.magnetUri.startsWith("magnet:?xt=urn:btih:LJDJSTRGCAW5PUOER34MCTSCQLPWD4XG$magnetTrackers"),
        )
        assertTrue(details.magnetUri, details.magnetUri.endsWith(lastMagnetTracker))
        assertEquals("457.9 MB", details.size)
        assertEquals(Instant.parse("2026-09-28T00:00:00Z"), details.uploadDate)
        assertEquals(Category.Anime, details.category)
        // The leading "簡介:" label and <br> are removed; the rest is serialised.
        assertEquals(
            "<p style=\"text-align: center;\"><span style=\"color: rgb(51, 51, 51); " +
                    "font-family: arial, tahoma, verdana, sans-serif; background-color: rgb(238, 238, 255);\">" +
                    "報錯/意見提供 : </span><a href=\"https://t.me/Skymoon_Raws_Discussion\" target=\"_blank\" " +
                    "style=\"color: rgb(34, 68, 119); text-decoration: none; " +
                    "font-family: arial, tahoma, verdana, sans-serif; background-color: rgb(238, 238, 255);\" " +
                    "rel=\"external nofollow\">https://t.me/Skymoon_Raws_Discussion</a>&nbsp;</p>\n<br>",
            details.description,
        )
    }

    @Test
    fun `getDetails returns null when the page has no topic`() = runTest {
        val provider = Dmhy(fakeNetworkClient(detailsUrl to "<html><body></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }
}
