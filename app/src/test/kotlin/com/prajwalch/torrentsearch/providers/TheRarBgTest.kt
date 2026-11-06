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

class TheRarBgTest {
    private val searchUrl = "https://therarbg.com/get-posts/keywords:linux:category:Apps"
    private val detailsUrl = "https://therarbg.com/post-detail/8b7ad0/linuxmint-22-1-cinnamon-64bit-iso/"

    private val expectedMagnetUri = listOf(
        "https%3A%2F%2Fht.therarbg.to%2Fannounce",
        "udp%3A%2F%2Ftracker.therarbg.to%3A6969%2Fannounce",
        "udp%3A%2F%2Fzer0day.ch%3A1337%2Fannounce",
        "udp%3A%2F%2Ftracker.publictracker.xyz%3A6969%2Fannounce",
        "udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce",
        "udp%3A%2F%2Fopen.demonii.com%3A1337%2Fannounce",
        "udp%3A%2F%2Fopen.stealth.si%3A80%2Fannounce",
        "udp%3A%2F%2Fudp.tracker.projectk.org%3A23333%2Fannounce",
        "udp%3A%2F%2Fuabits.today%3A6990%2Fannounce",
        "udp%3A%2F%2Ftracker.tryhackx.org%3A6969%2Fannounce",
        "udp%3A%2F%2Ftracker.theoks.net%3A6969%2Fannounce",
        "udp%3A%2F%2Ftracker.t-1.org%3A6969%2Fannounce",
        "udp%3A%2F%2Ftracker.startwork.cv%3A1337%2Fannounce",
        "udp%3A%2F%2Ftracker.qu.ax%3A6969%2Fannounce",
    ).joinToString(
        separator = "",
        prefix = "magnet:?xt=urn:btih:5D4D25E0E66647C7E289202D788075D6884EC002" +
                "&dn=linuxmint-22.1-cinnamon-64bit.iso",
    ) { "&tr=$it" }

    @Test
    fun `search parses every result row`() = runTest {
        val provider = TheRarBg(fakeNetworkClient(searchUrl to fixture("therarbg/search.html")))

        val torrents = provider.search(query = "linux", category = Category.Apps)

        assertEquals(
            listOf(
                "Linux Kernel 6 10 incl license KhanPC",
                "linuxmint-22.1-cinnamon-64bit.iso",
                "linuxmint-22.1-xfce-64bit.iso",
                "linuxmint-22.2-cinnamon-64bit.iso",
            ),
            torrents.map { it.name },
        )
        // Sizes are formatted from the byte count in data-order.
        assertEquals(listOf("110.20 MB", "2.78 GB", "2.69 GB", "2.85 GB"), torrents.map { it.size })
        assertEquals(listOf(1u, 868u, 509u, 448u), torrents.map { it.seeders })
        assertEquals(listOf(1u, 1u, 0u, 62u), torrents.map { it.peers })
        assertEquals(
            listOf(1721154408L, 1776825172L, 1747688535L, 1757014466L).map(Instant::ofEpochSecond),
            torrents.map { it.uploadDate },
        )
        assertEquals(
            listOf(
                "https://therarbg.com/post-detail/75b394/linux-kernel-6-10-incl-license-khanpc/",
                "https://therarbg.com/post-detail/8b7ad0/linuxmint-22-1-cinnamon-64bit-iso/",
                "https://therarbg.com/post-detail/8716d1/linuxmint-22-1-xfce-64bit-iso/",
                "https://therarbg.com/post-detail/849085/linuxmint-22-2-cinnamon-64bit-iso/",
            ),
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(
            torrents.map { MagnetUri.RequiresFetch(it.detailsPageUrl!!) },
            torrents.map { it.magnetUri },
        )
        assertTrue(torrents.all { it.category == Category.Apps })
        assertTrue(torrents.all { it.providerName == "TheRarBg" })
        assertEquals(4, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = TheRarBg(fakeNetworkClient(searchUrl to "<html><body><p>No results</p></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "linux", category = Category.Apps))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = TheRarBg(fakeNetworkClient(detailsUrl to fixture("therarbg/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("linuxmint-22.1-cinnamon-64bit.iso", details.name)
        // &amp; in the href must be decoded.
        assertEquals(expectedMagnetUri, details.magnetUri)
        assertEquals("5d4d25e0e66647c7e289202d788075d6884ec002", details.infoHash)
        assertEquals("2.8 GB", details.size)
        assertEquals(868u, details.seeders)
        assertEquals(1u, details.peers)
        // "April 22, 2026, 2:32 a.m." (UTC)
        assertEquals(Instant.parse("2026-04-22T02:32:00Z"), details.uploadDate)
        assertEquals(Category.Apps, details.category)
        assertEquals("metheguy", details.uploader)
        // wholeText() keeps the cell's surrounding whitespace and inner newlines verbatim.
        assertEquals(
            "\n                " +
                    "Features\n" +
                    "What is Linux Mint?\n" +
                    "Linux Mint is an operating system for desktop and laptop computers. It is designed " +
                    "to work 'out of the box' and comes fully equipped with the apps most people need.\n" +
                    "Productivity\n" +
                    "With LibreOffice's complete office suite, use the word processor, make presentations, " +
                    "drawings, spreadsheets or even databases. Easily import from or export to PDF or " +
                    "Microsoft Office documents.\n" +
                    "Graphic Design\n" +
                    "Work in 3D with Blender, draw or edit pictures in Gimp, use Inkscape for vector graphics.\n" +
                    "Multimedia\n" +
                    "Enjoy your music, watch TV and movies, listen to podcasts, Spotify and online radio.\n" +
                    "Gaming\n" +
                    "Access more than 7,800 games with Steam. Install GOG to get even more" +
                    "\n            ",
            details.description,
        )
    }

    @Test
    fun `getDetails returns null when the title is not followed by the details table`() = runTest {
        val html = """
            <html><body>
            <div class="postContL">
              <h4 class="text-center">:Search: </h4>
              <div class="row"></div>
              <a href="magnet:?xt=urn:btih:5D4D25E0E66647C7E289202D788075D6884EC002">Magnet</a>
            </div>
            </body></html>
        """.trimIndent()
        val provider = TheRarBg(fakeNetworkClient(detailsUrl to html))

        assertNull(provider.getDetails(detailsUrl))
    }

    @Test
    fun `getMagnetUri extracts the magnet link from the details page`() = runTest {
        val provider = TheRarBg(fakeNetworkClient(detailsUrl to fixture("therarbg/details.html")))

        assertEquals(expectedMagnetUri, provider.getMagnetUri(detailsUrl))
    }
}
