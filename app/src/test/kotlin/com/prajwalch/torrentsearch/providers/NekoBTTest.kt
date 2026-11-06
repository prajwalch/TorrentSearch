package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.testing.fakeNetworkClient
import com.prajwalch.torrentsearch.testing.fixture

import kotlinx.coroutines.test.runTest

import java.time.Instant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NekoBTTest {
    private val searchUrl = "https://nekobt.to/search?query=bocchi"
    private val detailsUrl = "https://nekobt.to/torrents/7307330170119"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = NekoBT(fakeNetworkClient(searchUrl to fixture("nekobt/search.html")))

        val torrents = provider.search(query = "bocchi", category = Category.Anime)

        assertEquals(
            listOf(
                "BOCCHI THE ROCK! S01 - VOSTFR BD 1080p x265 AAC -Fuceo",
                "[GJM] Hitoribocchi no Marumaru Seikatsu - 10 [119E1A1A].mkv",
                "[GJM] Hitoribocchi no Marumaru Seikatsu - 09 [C9F6C6D6].mkv",
                "[GJM] Hitoribocchi no Marumaru Seikatsu - 08 [08B0388B].mkv",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("9.64 GiB", "346.14 MiB", "387.81 MiB", "367.66 MiB"), torrents.map { it.size })
        assertEquals(listOf(3u, 0u, 0u, 0u), torrents.map { it.seeders })
        assertEquals(listOf(0u, 0u, 0u, 0u), torrents.map { it.peers })
        assertEquals(
            listOf(
                "2024-01-01T12:13:58Z",
                "2024-01-10T19:39:05Z",
                "2023-08-21T14:45:13Z",
                "2023-03-25T14:03:35Z",
            ).map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        val torrentIds = listOf("8183194521860", "7307330170119", "7307330013190", "7307329859591")
        assertEquals(torrentIds.map { "https://nekobt.to/torrents/$it" }, torrents.map { it.detailsPageUrl })
        assertEquals(
            torrentIds.map { "https://nekobt.to/api/v1/torrents/$it/download?public=true" },
            torrents.map { it.fileDownloadLink },
        )
        val infoHashes = listOf(
            "4119835863ad87f4e5f99df0ea6d2d270595abc5",
            "6a44980aa97251cc7b28dcb12b2067008a2505fb",
            "1bfd2f0b0b04a0b9b288dc4fa3de5165455c3be4",
            "1a3fc52667a9b236ae68c4307b5b6322b29b843a",
        )
        val magnetUris = torrents.map { (it.magnetUri as MagnetUri.Available).value }
        infoHashes.zip(magnetUris).forEach { (infoHash, magnetUri) ->
            assertTrue(magnetUri, magnetUri.startsWith("magnet:?xt=urn:btih:$infoHash&dn="))
            assertFalse(magnetUri, magnetUri.contains("&amp;"))
        }
        assertTrue(
            magnetUris[0],
            magnetUris[0].startsWith(
                "magnet:?xt=urn:btih:4119835863ad87f4e5f99df0ea6d2d270595abc5" +
                        "&dn=BOCCHI%20THE%20ROCK!%20S01%20-%20VOSTFR%20BD%201080p%20x265%20AAC%20-Fuceo" +
                        "%20%7BTags%3AL4%3BV13%3BC2%3BA%3Dja%3BF%3Dfrfr%3B%7D" +
                        "&tr=https%3A%2F%2Ftracker.nekobt.to%2Fapi%2Ftracker%2Fpublic%2Fannounce" +
                        "&tr=http%3A%2F%2Fnyaa.tracker.wf%3A7777%2Fannounce",
            ),
        )
        assertTrue(torrents.all { it.category == Category.Anime })
        assertTrue(torrents.all { it.providerName == "NekoBT" })
        assertEquals(4, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = NekoBT(fakeNetworkClient(searchUrl to "<html><body><p>No torrents found</p></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "bocchi", category = Category.Anime))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = NekoBT(fakeNetworkClient(detailsUrl to fixture("nekobt/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("[GJM] Hitoribocchi no Marumaru Seikatsu - 10 [119E1A1A].mkv", details.name)
        assertEquals("6a44980aa97251cc7b28dcb12b2067008a2505fb", details.infoHash)
        assertTrue(
            details.magnetUri,
            details.magnetUri.startsWith(
                "magnet:?xt=urn:btih:6a44980aa97251cc7b28dcb12b2067008a2505fb" +
                        "&dn=%5BGJM%5D%20Hitoribocchi%20no%20Marumaru%20Seikatsu%20-%2010%20%5B119E1A1A%5D.mkv" +
                        "%20%7BTags%3AOTL%3BL3%3BV8%3BC1%3BA%3Dja%3BF%3Den%3B%7D" +
                        "&tr=https%3A%2F%2Ftracker.nekobt.to%2Fapi%2Ftracker%2Fpublic%2Fannounce",
            ),
        )
        assertFalse(details.magnetUri.contains("&amp;"))
        assertEquals("346.14 MiB", details.size)
        assertEquals(0u, details.seeders)
        assertEquals(0u, details.peers)
        assertEquals(Instant.parse("2024-01-10T19:39:05Z"), details.uploadDate)
        assertEquals("Fyurie", details.uploader)
        assertEquals(Category.Anime, details.category)
        assertEquals(
            "https://nekobt.to/api/v1/torrents/7307330170119/download?public=true",
            details.fileDownloadLink,
        )
        assertEquals(
            "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx101386-ZJN6JFf9gKrV.png",
            details.posterUrl,
        )
        assertEquals(
            "<p>[GJM] Hitoribocchi no Marumaru Seikatsu - 10</p>\n" +
            "<p>- <a href=\"https://www.goodjobmedia.com/\">https://www.goodjobmedia.com/</a>\n" +
            " <br>\n" +
            " - <a href=\"https://www.twitter.com/goodjobmedia\">http://www.twitter.com/goodjobmedia</a>\n" +
            " <br>\n" +
            " - <a>#<span class=\"__cf_email__\" data-cfemail=\"b5d2dadad1dfdad7f5dcc7d69bc7dccfdadb9bdbd0c1\">[email&nbsp;protected]</span></a>\n" +
            " <br>\n" +
            " - <a href=\"https://discord.gg/0jeKKhzGn3EBNDI2\">https://discord.gg/0jeKKhzGn3EBNDI2</a></p>\n" +
            "<p>If you encounter any playback issues with a GJM release, please make sure you are using the most recent version of mpv.</p>\n" +
            "<p>If you wish to report any other general issues with a GJM release, please reach out to us using our website or our Discord server (both linked above) as we are no longer checking torrent comment sections.</p>\n" +
            "<p>Enjoy the release!</p>",
            details.description,
        )
    }

    @Test
    fun `getDetails returns null when the page has no info card`() = runTest {
        val provider = NekoBT(fakeNetworkClient(detailsUrl to "<html><body><div class=\"grid\"></div></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }
}
