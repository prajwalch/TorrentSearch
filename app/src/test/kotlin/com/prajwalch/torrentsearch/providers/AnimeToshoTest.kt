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

class AnimeToshoTest {
    private val searchUrl = "https://animetosho.org/search?q=naruto"
    private val detailsUrl =
        "https://animetosho.org/view/asw-girls-und-panzer-das-finale-03-1080p.n1620314"
    private val batchDetailsUrl =
        "https://animetosho.org/view/naruto-box-05-remaster-v2-bdrip-1080p-x265.n2097591"

    private val trackers = "&tr=http%3A%2F%2Fnyaa.tracker.wf%3A7777%2Fannounce" +
            "&tr=udp%3A%2F%2Fopen.stealth.si%3A80%2Fannounce" +
            "&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce" +
            "&tr=udp%3A%2F%2Fexodus.desync.com%3A6969%2Fannounce" +
            "&tr=udp%3A%2F%2Ftracker.torrent.eu.org%3A451%2Fannounce"

    @Test
    fun `search parses every result entry`() = runTest {
        val provider = AnimeTosho(fakeNetworkClient(searchUrl to fixture("animetosho/search.html")))

        val torrents = provider.search(query = "naruto", category = Category.Anime)

        assertEquals(
            listOf(
                "Naruto Shippuden 163 Remaster (BDRip 1080p x265 FLAC Multi) - Ryūjin 竜神",
                "Naruto Shippuden 088 Remaster (BDRip 1080p x265 FLAC Multi) - Ryūjin 竜神",
                "Naruto Shippuden 052 Remaster (*with subs eng*) (BDRip 1080p x265 FLAC Multi) - Ryūjin 竜神",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("1.309 GB", "1.169 GB", "1.137 GB"), torrents.map { it.size })
        // Parsed from "[105↑/145↓]" inside the titled stats span.
        assertEquals(listOf(105u, 301u, 26u), torrents.map { it.seeders })
        assertEquals(listOf(145u, 9u, 31u), torrents.map { it.peers })
        assertEquals(
            listOf("2026-05-07", "2026-05-02", "2026-05-01")
                .map { Instant.parse("${it}T00:00:00Z") },
            torrents.map { it.uploadDate },
        )
        assertEquals(
            listOf(
                "https://animetosho.org/view/naruto-shippuden-163-remaster-bdrip-1080p-x265-flac-multi.n2107283",
                "https://animetosho.org/view/naruto-shippuden-088-remaster-bdrip-1080p-x265-flac-multi.n2105469",
                "https://animetosho.org/view/naruto-shippuden-052-remaster-subs-eng-bdrip-1080p.n2105162",
            ),
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(
            listOf(
                "https://nyaa.si/download/2107283.torrent",
                "https://nyaa.si/download/2105469.torrent",
                "https://nyaa.si/download/2105162.torrent",
            ),
            torrents.map { it.fileDownloadLink },
        )
        assertTrue(torrents.all { it.category == Category.Anime })
        assertTrue(torrents.all { it.providerName == "AnimeTosho" })
        assertEquals(3, torrents.map { it.id }.toSet().size)

        // HTML entities (&amp;) inside the href must be decoded.
        assertEquals(
            MagnetUri.Available(
                "magnet:?xt=urn:btih:VWAFIX4VQVMYME5BODB5LDIJNNJ2O72V" + trackers +
                        "&dn=Naruto%20Shippuden%20163%20Remaster%20%28BDRip%201080p%20x265%20FLAC" +
                        "%20Multi%29%20-%20Ry%C5%ABjin%20%E7%AB%9C%E7%A5%9E",
            ),
            torrents[0].magnetUri,
        )
    }

    @Test
    fun `search returns no results for a page without entries`() = runTest {
        val provider = AnimeTosho(
            fakeNetworkClient(searchUrl to "<html><body><div id=\"content\"><p>No results</p></div></body></html>"),
        )

        assertEquals(emptyList<Any>(), provider.search(query = "naruto", category = Category.Anime))
    }

    @Test
    fun `getDetails parses a single file torrent page`() = runTest {
        val provider = AnimeTosho(fakeNetworkClient(detailsUrl to fixture("animetosho/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("[ASW] Girls und Panzer das Finale - 03 [1080p HEVC x265 10Bit][AAC]", details.name)
        assertEquals("suw2cwzqaemu3e6vctgdvff2nvo2zeb4", details.infoHash)
        assertEquals(
            "magnet:?xt=urn:btih:SUW2CWZQAEMU3E6VCTGDVFF2NVO2ZEB4" + trackers +
                    "&dn=%5BASW%5D%20Girls%20und%20Panzer%20das%20Finale%20-%2003%20%5B1080p%20HEVC" +
                    "%20x265%2010Bit%5D%5BAAC%5D",
            details.magnetUri,
        )
        // From <span title="File size: ...">(1.491 GB)</span>.
        assertEquals("1.491 GB", details.size)
        // First tracker row with numeric stats.
        assertEquals(19u, details.seeders)
        assertEquals(2u, details.peers)
        assertEquals(Instant.parse("2023-01-04T09:01:00Z"), details.uploadDate)
        assertEquals(Category.Anime, details.category)
        assertEquals(
            "https://animetosho.org/storage/torrent/952da15b3001194d93d514cc3a94ba6d5dac903c/" +
                    "%5BASW%5D%20Girls%20und%20Panzer%20das%20Finale%20-%2003%20%5B1080p%20HEVC%5D" +
                    "%5B446BA50E%5D.torrent",
            details.fileDownloadLink,
        )
        // Only the <a class="screenthumb"> links, not the <img class="screenthumb"> inside them.
        assertEquals(
            listOf(13560, 719680, 1420760, 2129630, 2834300).map {
                "https://storage.animetosho.org/sframes/000fd732_$it.png?s=3"
            },
            details.screenshotUrls,
        )
    }

    @Test
    fun `getDetails falls back to the text after the magnet link for the size`() = runTest {
        val provider = AnimeTosho(
            fakeNetworkClient(batchDetailsUrl to fixture("animetosho/details_batch.html")),
        )

        val details = provider.getDetails(batchDetailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Naruto - Box 05 Remaster [V2] (BDRip 1080p x265 FLAC Multi) - Ryūjin 竜神", details.name)
        assertEquals("wxnvljclx3ijdx4nk5c2q5o625k4fhpp", details.infoHash)
        assertEquals(
            "magnet:?xt=urn:btih:WXNVLJCLX3IJDX4NK5C2Q5O625K4FHPP" + trackers +
                    "&dn=Naruto%20-%20Box%2005%20Remaster%20%5BV2%5D%20%28BDRip%201080p%20x265%20FLAC" +
                    "%20Multi%29%20-%20Ry%C5%ABjin%20%E7%AB%9C%E7%A5%9E",
            details.magnetUri,
        )
        // No "File size:" span: the text node " (36.40 GB) | " following the magnet link is
        // trimmed, then '(', ')' and '|' are removed, which leaves the inner trailing space.
        assertEquals("36.40 GB ", details.size)
        assertEquals(6u, details.seeders)
        assertEquals(1u, details.peers)
        assertEquals(Instant.parse("2026-04-13T11:21:00Z"), details.uploadDate)
        assertEquals(null, details.fileDownloadLink)
        assertEquals(emptyList<String>(), details.screenshotUrls)
    }

    @Test
    fun `getDetails returns null when the page has no magnet link`() = runTest {
        val provider = AnimeTosho(
            fakeNetworkClient(detailsUrl to "<html><body><h2 id=\"title\">Removed</h2></body></html>"),
        )

        assertEquals(null, provider.getDetails(detailsUrl))
    }
}
