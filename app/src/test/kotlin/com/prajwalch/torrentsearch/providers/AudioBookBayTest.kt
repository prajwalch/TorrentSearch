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

class AudioBookBayTest {
    private val searchUrl = "https://audiobookbay.lu/?s=dune"
    private val detailsUrl = "https://audiobookbay.lu/abss/dune-frank-herbert-2/"

    private val infoHash = "061850ead3eb6f1c5c6d8420211b4bbf2d4ee3e2"

    @Test
    fun `search parses every post`() = runTest {
        val provider = AudioBookBay(fakeNetworkClient(searchUrl to fixture("audiobookbay/search.html")))

        val torrents = provider.search(query = "dune", category = Category.Books)

        assertEquals(
            listOf(
                "The Dune Saga - All Six Books, Fully Chaptered - Frank Herbert",
                "Dune - Frank Herbert",
                // The double space in the markup is collapsed and &#8217; is decoded.
                "Kevin S. Decker and William Irwin (editors) - Dune and Philosophy: Minds, Monads, and Muad’Dib",
            ),
            torrents.map { it.name },
        )
        val detailsPageUrls = listOf(
            "https://audiobookbay.lu/abss/the-dune-saga-fully-chaptered-frank-herbert/",
            "https://audiobookbay.lu/abss/dune-frank-herbert-2/",
            "https://audiobookbay.lu/abss/kevin-s-decker-and-william-irwin-editors-dune-and-philosophy-minds-monads-and-muaddib/",
        )
        assertEquals(detailsPageUrls, torrents.map { it.detailsPageUrl })
        assertEquals(detailsPageUrls.map(MagnetUri::RequiresFetch), torrents.map { it.magnetUri })
        // "File Size: <span>4.39</span> GBs" on its own <br>-separated line.
        assertEquals(listOf("4.39 GB", "1.12 GB", "1.17 GB"), torrents.map { it.size })
        assertEquals(
            listOf("2026-05-01", "2025-12-24", "2025-05-03").map { Instant.parse("${it}T00:00:00Z") },
            torrents.map { it.uploadDate },
        )
        assertTrue(torrents.all { it.seeders == null && it.peers == null })
        assertTrue(torrents.all { it.category == Category.Books })
        assertTrue(torrents.all { it.providerName == "AudioBookBay" })
        assertEquals(3, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for a page without posts`() = runTest {
        val provider = AudioBookBay(
            fakeNetworkClient(searchUrl to "<html><body><div id=\"content\"><h2>Not Found</h2></div></body></html>"),
        )

        assertEquals(emptyList<Any>(), provider.search(query = "dune", category = Category.Books))
    }

    @Test
    fun `latest torrents reuse the results page parser`() = runTest {
        val provider = AudioBookBay(
            fakeNetworkClient("https://audiobookbay.lu" to fixture("audiobookbay/search.html")),
        )

        val torrents = provider.getLastestTorrents(category = Category.Books)

        assertEquals(3, torrents.size)
        assertEquals("https://audiobookbay.lu/abss/dune-frank-herbert-2/", torrents[1].detailsPageUrl)
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = AudioBookBay(fakeNetworkClient(detailsUrl to fixture("audiobookbay/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Dune - Frank Herbert", details.name)
        assertEquals(infoHash, details.infoHash)
        assertTrue(details.magnetUri, details.magnetUri.startsWith("magnet:?xt=urn:btih:$infoHash&tr="))
        assertEquals("1.12 GB", details.size)
        // "Wed, 24 Dec 2025 20:07:30 +0100".
        assertEquals(Instant.parse("2025-12-24T19:07:30Z"), details.uploadDate)
        assertEquals(Category.Books, details.category)
        assertEquals("NGC300", details.uploader)
        assertEquals("https://m.media-amazon.com/images/I/71-1WBgjGoL._SL1500_.jpg", details.posterUrl)
        assertEquals(
            listOf(
                "<p style=\"left;\">Written by <a href=\"/?s=frank+herbert&amp;tt=1\"><span class=\"author\" itemprop=\"author\" itemtype=\"https://schema.org/Person\">Frank Herbert</span></a>",
                " <br>",
                " Read by <a href=\"/?s=scott+brick\"><span class=\"narrator\" itemprop=\"author\" itemtype=\"https://schema.org/Person\">Scott Brick</span></a>, <a href=\"/?s=orlagh+cassidy\"><span class=\"narrator\" itemprop=\"author\" itemtype=\"https://schema.org/Person\">Orlagh Cassidy</span></a>, <a href=\"/?s=euan+morton\"><span class=\"narrator\" itemprop=\"author\" itemtype=\"https://schema.org/Person\">Euan Morton</span></a>, <a href=\"/?s=simon+vance\"><span class=\"narrator\" itemprop=\"author\" itemtype=\"https://schema.org/Person\">Simon Vance</span></a>, <a href=\"/?s=ilyana+kadushin\"><span class=\"narrator\" itemprop=\"author\" itemtype=\"https://schema.org/Person\">Ilyana Kadushin</span></a>, <a href=\"/?s=byron+jennings\"><span class=\"narrator\" itemprop=\"author\" itemtype=\"https://schema.org/Person\">Byron Jennings</span></a>, <a href=\"/?s=david+r.+gordon\"><span class=\"narrator\" itemprop=\"author\" itemtype=\"https://schema.org/Person\">David R. Gordon</span></a>, <a href=\"/?s=jason+culp\"><span class=\"narrator\" itemprop=\"author\" itemtype=\"https://schema.org/Person\">Jason Culp</span></a>, <a href=\"/?s=kent+broadhurst\"><span class=\"narrator\" itemprop=\"author\" itemtype=\"https://schema.org/Person\">Kent Broadhurst</span></a>, <a href=\"/?s=oliver+wyman\"><span class=\"narrator\" itemprop=\"author\" itemtype=\"https://schema.org/Person\">Oliver Wyman</span></a>, <a href=\"/?s=patricia+kilgarriff\"><span class=\"narrator\" itemprop=\"author\" itemtype=\"https://schema.org/Person\">Patricia Kilgarriff</span></a>, <a href=\"/?s=scott+sowers\"><span class=\"narrator\" itemprop=\"author\" itemtype=\"https://schema.org/Person\">Scott Sowers</span></a>",
                " <br>",
                " Format: <span class=\"format\" itemprop=\"encodingFormat\">M4B</span>",
                " <br>",
                " <span class=\"is_abridged\">Unabridged</span></p>",
                "<p>Bit Rate: 125 kbps",
                " <br>",
                " Length: 21 hours and 2 minutes",
                " <br>",
                " Narrators: Scott Brick, Orlagh Cassidy, Euan Morton, Simon Vance, Ilyana Kadushin, Byron Jennings, David R. Gordon, Jason Culp, Kent Broadhurst, Oliver Wyman, Patricia Kilgarriff, Scott Sowers</p>",
                "<p>Set on the desert planet Arrakis, Dune is the story of the boy Paul Atreides, who would become the mysterious man known as Muad’dib. He would avenge the traitorous plot against his noble family—and would bring to fruition humankind’s most ancient and unattainable dream.</p>",
            ).joinToString("\n"),
            details.description,
        )
    }

    @Test
    fun `getDetails returns null when the page has no info hash`() = runTest {
        val provider = AudioBookBay(
            fakeNetworkClient(
                detailsUrl to "<html><body><div class=\"postTitle\"><h1>Dune</h1></div></body></html>",
            ),
        )

        assertEquals(null, provider.getDetails(detailsUrl))
    }

    @Test
    fun `getMagnetUri builds the magnet from the info hash on the details page`() = runTest {
        val provider = AudioBookBay(fakeNetworkClient(detailsUrl to fixture("audiobookbay/details.html")))

        val magnetUri = provider.getMagnetUri(detailsUrl)

        assertTrue(magnetUri, magnetUri.startsWith("magnet:?xt=urn:btih:$infoHash&tr="))
    }

    @Test
    fun `getMagnetUri fails when the page has no info hash`() = runTest {
        val provider = AudioBookBay(fakeNetworkClient(detailsUrl to "<html><body></body></html>"))

        val result = runCatching { provider.getMagnetUri(detailsUrl) }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
