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

class TorrentDatabaseTest {
    private val searchUrl = "https://developify.ca/search?q=pro&category=software"
    private val detailsUrl = "https://developify.ca/id/2960852"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = TorrentDatabase(fakeNetworkClient(searchUrl to fixture("torrentdatabase/search.html")))

        val torrents = provider.search(query = "pro", category = Category.Apps)

        assertEquals(
            listOf(
                "Adobe Premier Pro 7.1 v31265 Lego +Patch",
                "uTorrent Pro 5.5.1 Build 43580 Stable + Portable",
                "Microsoft Office Professional Plus 2019 Engl + Full Activation",
            ),
            torrents.map { it.name },
        )
        assertEquals(listOf("950.0 MB", "11.0 MB", "1.23 GB"), torrents.map { it.size })
        // Counts sit next to an <img> inside the spans.
        assertEquals(listOf(39400u, 39385u, 39346u), torrents.map { it.seeders })
        assertEquals(listOf(26254u, 26266u, 26226u), torrents.map { it.peers })
        assertEquals(
            listOf("2024-02-02T18:58:15Z", "2024-02-02T18:54:24Z", "2024-02-02T18:54:43Z")
                .map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        assertTrue(torrents.all { it.category == Category.Apps })
        assertEquals(
            listOf(
                "https://developify.ca/id/2960802",
                "https://developify.ca/id/2960887",
                "https://developify.ca/id/2960852",
            ),
            torrents.map { it.detailsPageUrl },
        )
        // Info hash comes from the "/track/magnet/<hash>?redirect=1&amp;sig=..." href.
        val infoHashes = listOf(
            "84EA34F1FA0149BE23D753CC99AC2A505B78E3B7",
            "48B72A1A4F3B32DB8884110893D1210C6A582DAB",
            "5707549D14DAE8882CA23E2F8460613AA0B3BC03",
        )
        torrents.zip(infoHashes).forEach { (torrent, infoHash) ->
            val magnetUri = (torrent.magnetUri as MagnetUri.Available).value
            assertTrue(magnetUri, magnetUri.startsWith("magnet:?xt=urn:btih:$infoHash&tr="))
            assertTrue(magnetUri, magnetUri.endsWith("&tr=https%3A%2F%2Fdevelopify.ca%2Fannounce"))
        }
        assertTrue(torrents.all { it.providerName == "TorrentDatabase" })
        assertEquals(3, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = TorrentDatabase(
            fakeNetworkClient(searchUrl to "<html><body><div class=\"main-container\"><p>No torrents found.</p></div></body></html>"),
        )

        assertEquals(emptyList<Any>(), provider.search(query = "pro", category = Category.Apps))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = TorrentDatabase(fakeNetworkClient(detailsUrl to fixture("torrentdatabase/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        // ownText() excludes the nested "ID: ..." span.
        assertEquals("Microsoft Office Professional Plus 2019 Engl + Full Activation", details.name)
        assertEquals(
            "magnet:?xt=urn:btih:5707549D14DAE8882CA23E2F8460613AA0B3BC03" +
                    "&dn=Microsoft%20Office%20Professional%20Plus%202019%20Engl%20%2B%20Full%20Activation" +
                    "&tr=https%3A%2F%2Fdevelopify.ca%2Fannounce",
            details.magnetUri,
        )
        assertEquals("5707549d14dae8882ca23e2f8460613aa0b3bc03", details.infoHash)
        assertEquals("1.23 GB", details.size)
        assertEquals(39346u, details.seeders)
        assertEquals(26226u, details.peers)
        assertEquals(Instant.parse("2024-02-02T18:54:43Z"), details.uploadDate)
        assertEquals(Instant.parse("2026-09-29T21:22:28Z"), details.lastChecked)
        assertEquals(Category.Apps, details.category)
        assertEquals("DevelopifyBOT", details.uploader)
        assertEquals(
            """
            Microsoft Office Professional Plus 2019
            <br>
            Language : English
            <br>
            Architecture : x86 &amp; x64
            <br>
            <br>
            Includes:
            <br>
            Word, Excel, PowerPoint, Outlook
            <br>
            <a href="https://www.microsoft.com/microsoft-365" target="_blank" rel="noopener noreferrer" class="text-primary">https://www.microsoft.com/microsoft-365</a>
            """.trimIndent(),
            details.description,
        )
    }

    @Test
    fun `getDetails returns null when the page has no torrent card`() = runTest {
        val provider = TorrentDatabase(fakeNetworkClient(detailsUrl to "<html><body></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }
}
