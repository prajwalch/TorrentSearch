package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.testing.fakeNetworkClient
import com.prajwalch.torrentsearch.testing.fixture

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.test.runTest

import java.time.Instant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AniRenaTest {
    private val searchUrl = "https://anirena.com?q=naruto&page=1&cat=anime"
    private val detailsUrl = "https://anirena.com/torrent/01a11952-5793-7599-8001-158241e0588c"
    private val magnetSourceUrl = "https://anirena.com/torrents/01a11952-5793-7599-8001-158241e0588c/magnet"

    @Test
    fun `search parses every table row`() = runTest {
        val provider = AniRena(fakeNetworkClient(searchUrl to fixture("anirena/search.html")))

        val torrents = provider.search(query = "naruto", category = Category.Anime)

        assertEquals(
            listOf(
                "Boruto - Naruto Next Generations [1-293][COMPLETE][BD 1920x1080 x264 10bit FLAC]" +
                        "[Dual Audio][Multiple Subs] [Almighty]",
                "[WZF]Naruto Shippuuden - 401~451[HD][X264-AAC][1280x720][Sub Esp][MP4]",
                "[WZF]Naruto Shippuuden - 301~400[HD][X264-AAC][1280x720][Sub Esp][MP4]",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("462.6 GB", "13.7 GB", "26.6 GB"), torrents.map { it.size })
        assertEquals(listOf(1u, 3u, 3u), torrents.map { it.seeders })
        assertEquals(listOf(2u, 0u, 0u), torrents.map { it.peers })
        assertEquals(
            listOf(1761943756L, 1747447523L, 1747447499L).map(Instant::ofEpochSecond),
            torrents.map { it.uploadDate },
        )
        // "Anime / Subtitle(s) and/or Audio(s)" -> "Anime".
        assertTrue(torrents.all { it.category == Category.Anime })
        assertTrue(torrents.all { it.providerName == "AniRena" })
        assertEquals(3, torrents.map { it.id }.toSet().size)

        val torrent = torrents[1]
        // Relative hrefs must be resolved against the page URL.
        assertEquals(
            "https://anirena.com/torrent/019d5dfa-b83d-7a90-941e-5d55c65c8c68",
            torrent.detailsPageUrl,
        )
        assertEquals(
            "https://anirena.com/torrents/019d5dfa-b83d-7a90-941e-5d55c65c8c68.torrent",
            torrent.fileDownloadLink,
        )
        assertEquals(
            MagnetUri.RequiresFetch("https://anirena.com/torrents/019d5dfa-b83d-7a90-941e-5d55c65c8c68/magnet"),
            torrent.magnetUri,
        )
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = AniRena(
            fakeNetworkClient(searchUrl to "<html><body><p>No torrents found.</p></body></html>"),
        )

        assertEquals(emptyList<Any>(), provider.search(query = "naruto", category = Category.Anime))
    }

    @Test
    fun `latest torrents reuse the results page parser`() = runTest {
        val provider = AniRena(fakeNetworkClient("https://anirena.com" to fixture("anirena/search.html")))

        val torrents = provider.getLastestTorrents(category = Category.All)

        assertEquals(3, torrents.size)
        assertEquals(
            "https://anirena.com/torrent/019d5dfc-31b8-79d2-8ba7-8e574aae17ac",
            torrents[0].detailsPageUrl,
        )
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val magnetUri = "magnet:?xt=urn:btih:71ad86485da245805b6b4a4f2b7422df93772878"
        val magnetResponse = mockk<HttpResponse>()
        every { magnetResponse.headers } returns headersOf(HttpHeaders.Location, magnetUri)
        val networkClient = fakeNetworkClient(detailsUrl to fixture("anirena/details.html"))
        coEvery { networkClient.get(magnetSourceUrl, any()) } returns magnetResponse
        val provider = AniRena(networkClient)

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        // The nested group link text is part of the title.
        assertEquals(
            "[Erai-raws]Magic Knight Rayearth (2026) - 01 [480p CR WEB-DL AVC AAC][MultiSub][E895733F]",
            details.name,
        )
        assertEquals("71ad86485da245805b6b4a4f2b7422df93772878", details.infoHash)
        assertEquals("197.7 MB", details.size)
        // Own text only: the "(97)" / "(41)" spans are excluded.
        assertEquals(14u, details.seeders)
        assertEquals(3u, details.peers)
        assertEquals(Instant.parse("2026-10-08T02:23:00Z"), details.uploadDate)
        assertEquals(Category.Anime, details.category)
        assertEquals("Erai-raws", details.uploader)
        assertEquals(magnetUri, details.magnetUri)
        assertEquals(
            "https://anirena.com/torrents/01a11952-5793-7599-8001-158241e0588c.torrent",
            details.fileDownloadLink,
        )
        // Raw JSON string data of the <script> element without the surrounding quotes;
        // escape sequences such as \n are kept verbatim.
        assertEquals(
            """[Erai-raws( https://www.erai-raws.info/anime-list/magic-knight-rayearth-2026/ )](https://www.erai-raws.info/anime-list/magic-knight-rayearth-2026/)\n""" +
                """\n""" +
                """- `Video Info:` \n""" +
                """**Japanese** ([Erai-raws]_AVC_CR) | **AVC** | **Main@L3** | **854x480** | **972 kbps** | **23.976 FPS**\n""" +
                """\n""" +
                """- `Audio Info:` \n""" +
                """**Japanese** ([Erai-raws]_AAC_CR) | **AAC** | **2.0** | **192 kbps**\n""" +
                """\n""" +
                """- `Subtitles Info:` \n""" +
                """**English** (CR_English) | ASS\n""" +
                """**Portuguese** (CR_Portuguese(Brazil)) | ASS\n""" +
                """**Spanish** (CR_Spanish(Latin_America)) | ASS\n""" +
                """**Spanish** (CR_Spanish) | ASS\n""" +
                """**Arabic** (CR_Arabic) | ASS\n""" +
                """**French** (CR_French) | ASS\n""" +
                """**German** (CR_German) | ASS\n""" +
                """**Italian** (CR_Italian) | ASS\n""" +
                """**Russian** (CR_Russian) | ASS\n""" +
                """**Indonesian** (CR_Indonesian) | ASS\n""" +
                """**Malay** (CR_Malay) | ASS\n""" +
                """**Vietnamese** (CR_Vietnamese) | ASS\n""" +
                """**Thai** (CR_Thai) | ASS\n""" +
                """**Chinese** (CR_Chinese(Simplified)) | ASS\n""" +
                """**Chinese** (CR_Chinese(Traditional)) | ASS\n""" +
                """**Polish** (CR_Polish) | ASS\n""" +
                """- `Chapters:` **Yes**\n""" +
                """- `Duration:` **~00:23:24**\n""" +
                """***\n""" +
                """####""",
            details.description,
        )
        // The live page has no "div.td-anime-poster" element.
        assertEquals(null, details.posterUrl)
    }

    @Test
    fun `getDetails returns null when the page has no info hash`() = runTest {
        val provider = AniRena(fakeNetworkClient(detailsUrl to "<html><body><h1 class=td-title>Gone</h1></body></html>"))

        assertEquals(null, provider.getDetails(detailsUrl))
    }
}
