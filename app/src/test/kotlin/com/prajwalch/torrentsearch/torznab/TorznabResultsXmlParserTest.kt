package com.prajwalch.torrentsearch.torznab

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.testing.fixture

import kotlinx.coroutines.test.runTest

import java.time.Instant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TorznabResultsXmlParserTest {
    private val parser = TorznabResultsXmlParser(providerId = "jackett", providerName = "Jackett")

    @Test
    fun `parse reads every item including namespaced torznab attributes`() = runTest {
        val torrents = parser.parse(fixture("torznab/results.xml"))

        assertEquals(
            listOf(
                "Linux Distro ISO x64",
                // CDATA content is taken verbatim.
                "Some Show S01E01 & Friends <1080p WEB-DL>",
                // Character and entity references are decoded.
                "Café & Bar Documentary",
                "Magnet Only Release",
            ),
            torrents.map { it.name },
        )
        // <size> element first, torznab:attr size as fallback.
        assertEquals(listOf("1.40 GB", "700.00 MB", "50.00 MB", "4.50 GB"), torrents.map { it.size })
        assertEquals(listOf(250u, 41u, null, 0u), torrents.map { it.seeders })
        assertEquals(listOf(280u, 57u, null, 1u), torrents.map { it.peers })
        assertEquals(
            listOf(
                Instant.parse("2026-10-10T07:15:00Z"),
                Instant.parse("2026-10-10T01:03:11Z"),
                Instant.parse("2026-09-28T12:00:00Z"),
                null,
            ),
            torrents.map { it.uploadDate },
        )
        // Only ids in the custom range (> 100000) are considered, so items with
        // just predefined category ids have no category.
        assertEquals(
            listOf(Category.Other, null, null, Category.Other),
            torrents.map { it.category },
        )
        assertEquals(
            listOf(
                "https://1337x.to/torrent/5432101/Linux-Distro-ISO-x64/",
                "https://torrentgalaxy.example/torrent/90210/some-show-s01e01",
                null,
                null,
            ),
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(
            listOf(
                "http://127.0.0.1:9117/dl/1337x/?jackett_apikey=abc123&path=Q2ZESjhP&file=Linux+Distro+ISO+x64",
                "http://127.0.0.1:9696/12/download?apikey=def456&link=dG9ycmVudA",
                "https://tracker.example.org/download.php?id=77&passkey=abc",
                // <link> holds a magnet, which is not a file download link.
                null,
            ),
            torrents.map { it.fileDownloadLink },
        )
        assertTrue(torrents.all { it.providerName == "Jackett" })
        assertEquals(4, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `parse resolves the magnet uri from each supported source`() = runTest {
        val magnetUris = parser.parse(fixture("torznab/results.xml"))
            .map { (it.magnetUri as MagnetUri.Available).value }

        // torznab:attr magneturl wins over infohash, <link> and <enclosure>.
        assertEquals(
            "magnet:?xt=urn:btih:2C6B6858D61DA9543D4231A71DB4B1C9264B0685" +
                    "&dn=Linux+Distro+ISO+x64&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce",
            magnetUris[0],
        )
        // Built from torznab:attr infohash with public trackers appended.
        assertTrue(
            magnetUris[1],
            magnetUris[1].startsWith(
                "magnet:?xt=urn:btih:5D41402ABC4B2A76B9719D911017C592DAD1E1C3" +
                        "&tr=udp://tracker.opentrackr.org:1337/announce&",
            ),
        )
        // Torrent-typed <enclosure> url.
        assertEquals("https://tracker.example.org/download.php?id=77&passkey=abc", magnetUris[2])
        // Magnet in <link>; the enclosure type is not exactly application/x-bittorrent.
        assertEquals(
            "magnet:?xt=urn:btih:aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d&dn=Magnet+Only+Release",
            magnetUris[3],
        )
    }

    @Test
    fun `parse returns no results for an empty channel`() = runTest {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:torznab="http://torznab.com/schemas/2015/feed">
              <channel><title>Jackett</title><link>http://127.0.0.1:9117/</link></channel>
            </rss>
        """.trimIndent()

        assertEquals(emptyList<Any>(), parser.parse(xml))
    }
}
