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
import org.junit.Assert.assertTrue
import org.junit.Test

class FitGirlRepacksTest {
    private val searchUrl = "https://fitgirl-repacks.site/?s=witcher"
    private val latestUrl = "https://fitgirl-repacks.site"
    private val detailsUrl = "https://fitgirl-repacks.site/thronebreaker-the-witcher-tales/"

    @Test
    fun `search parses every repack article`() = runTest {
        val provider = FitGirlRepacks(fakeNetworkClient(searchUrl to fixture("fitgirlrepacks/search.html")))

        val torrents = provider.search(query = "witcher", category = Category.Games)

        // The "Uncategorized" article is not a repack and must be skipped.
        assertEquals(
            listOf(
                "The Witcher 3: Wild Hunt \u2013 Remastered, v5.00b (Steam/GOG) + All DLCs + Bonus Content",
                "The Witcher 3: Wild Hunt \u2013 Complete Edition \u2013 GOG/Steam v4.00 + All DLCs + Bonus Content",
                "Thronebreaker: The Witcher Tales, v1.0.2.12",
            ),
            torrents.map { it.name },
        )
        val detailsPageUrls = listOf(
            "https://fitgirl-repacks.site/the-witcher-3-wild-hunt-remastered/",
            "https://fitgirl-repacks.site/the-witcher-3-wild-hunt-complete-edition/",
            "https://fitgirl-repacks.site/thronebreaker-the-witcher-tales/",
        )
        assertEquals(detailsPageUrls, torrents.map { it.detailsPageUrl })
        // Search results only contain excerpts, so the magnet must be fetched from the post.
        assertEquals(detailsPageUrls.map(MagnetUri::RequiresFetch), torrents.map { it.magnetUri })
        assertEquals(
            listOf(
                "2026-09-30T03:03:17Z",
                "2022-12-16T19:40:28Z",
                "2020-06-10T00:00:48Z",
            ).map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        assertTrue(torrents.all { it.category == Category.Games })
        assertTrue(torrents.all { it.providerName == "FitGirl Repacks" })
        assertTrue(torrents.all { it.size == null && it.seeders == null && it.peers == null })
        assertEquals(3, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for a page without articles`() = runTest {
        val provider = FitGirlRepacks(fakeNetworkClient(searchUrl to "<html><body><p>Nothing found</p></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "witcher", category = Category.Games))
    }

    @Test
    fun `getLastestTorrents extracts magnets from full posts`() = runTest {
        val provider = FitGirlRepacks(fakeNetworkClient(latestUrl to fixture("fitgirlrepacks/latest.html")))

        val torrents = provider.getLastestTorrents(category = Category.Games)

        assertEquals(
            listOf(
                "The Bell Echoes \u2013 v1.0.2026.9.28.2",
                "Cyber Cleaner Simulator \u2013 v1.0 Release",
            ),
            torrents.map { it.name },
        )
        assertEquals(
            listOf(
                "https://fitgirl-repacks.site/the-bell-echoes/",
                "https://fitgirl-repacks.site/cyber-cleaner-simulator/",
            ),
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(
            listOf(Instant.parse("2026-10-09T20:36:14Z"), Instant.parse("2026-10-09T12:40:28Z")),
            torrents.map { it.uploadDate },
        )

        // &#038; entities inside the href must be decoded.
        val magnetUris = torrents.map { (it.magnetUri as MagnetUri.Available).value }
        assertTrue(
            magnetUris[0],
            magnetUris[0].startsWith(
                "magnet:?xt=urn:btih:FD654C72A65EE75AF295858F675EAC5C7C21C499" +
                        "&dn=The+Bell+Echoes+%28v1.0.2026.9.28.2%2C+MULTi4%29+%5BFitGirl+Repack%5D" +
                        "&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce" +
                        "&tr=udp%3A%2F%2Ftracker.torrent.eu.org%3A451%2Fannounce",
            ),
        )
        assertTrue(
            magnetUris[1],
            magnetUris[1].startsWith(
                "magnet:?xt=urn:btih:807F9585978D30612291D7AED01DD15A5A023E0B" +
                        "&dn=Cyber+Cleaner+Simulator+%28v1.0+Release%2C+MULTi11%29+%5BFitGirl+Repack%5D" +
                        "&tr=udp%3A%2F%2Fopentor.net%3A6969%2Fannounce",
            ),
        )
        assertTrue(magnetUris.none { it.contains("&#") || it.contains("&amp;") })
    }

    @Test
    fun `getDetails parses the post page`() = runTest {
        val provider = FitGirlRepacks(fakeNetworkClient(detailsUrl to fixture("fitgirlrepacks/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("Thronebreaker: The Witcher Tales, v1.0.2.12", details.name)
        assertEquals("6bd50985e5911e6917b27eb0c9535851ade457f9", details.infoHash)
        assertEquals("magnet:?xt=urn:btih:6BD50985E5911E6917B27EB0C9535851ADE457F9", details.magnetUri)
        assertEquals(Instant.parse("2020-06-10T00:00:48Z"), details.uploadDate)
        assertEquals("FitGirl", details.uploader)
        assertEquals(Category.Games, details.category)
        assertEquals(
            "http://i92.fastpic.ru/big/2018/1025/3f/84d0dbe301ae3a4d73f161468696233f.jpg",
            details.posterUrl,
        )
        // Images inside the element following the "Screenshots (Click to enlarge)" heading.
        assertEquals(
            listOf(
                "http://s01.riotpixels.net/data/54/e1/54e1bf54-28e9-452e-b888-bcdbb91740fc.jpg.240p.jpg",
                "http://s01.riotpixels.net/data/42/e5/42e5fb42-cd5b-4a36-9060-39c053087e03.jpg.240p.jpg",
                "http://s01.riotpixels.net/data/f9/c5/f9c57009-9130-440c-af0e-8dbc21ef67ad.jpg.240p.jpg",
                "http://s01.riotpixels.net/data/49/d7/49d75eff-8fc2-4bae-9147-5949492aa95e.jpg.240p.jpg",
                "http://s01.riotpixels.net/data/dd/28/dd280e17-3478-4e22-9619-89c91e0ef6d9.jpg.240p.jpg",
                "http://s01.riotpixels.net/data/c3/60/c360d98f-6e11-4974-b1dd-d699da9b3486.jpg.240p.jpg",
            ),
            details.screenshotUrls,
        )

        val description = normalizeHtml(details.description!!)
        assertTrue(
            description,
            description.startsWith(
                "<h3><span style=\"color: #339966;\">#1007 Updated</span><strong>Thronebreaker: The Witcher Tales" +
                        "<span style=\"color: #808080;\">v1.0.2.12</span></strong></h3>" +
                        "<p style=\"height: 200px; display: block;\">" +
                        "<a target=\"_blank\" href=\"http://en.riotpixels.com/games/thronebreaker-the-witcher-tales/\"" +
                        " rel=\"noopener\"><img decoding=\"async\" class=\"alignleft\" width=\"150\"" +
                        " src=\"http://i92.fastpic.ru/big/2018/1025/3f/84d0dbe301ae3a4d73f161468696233f.jpg\"></a><br>" +
                        "Genres/Tags:<a href=\"https://fitgirl-repacks.site/tag/card-game/\">Card game</a>,",
            ),
        )
        // The unclosed <p> is implicitly closed by the following <p>.
        assertTrue(
            description,
            description.contains("Repack Size:<strong>from 4.8 GB</strong>[Selective Download]</p><p></p>"),
        )
        // Unclosed <li>s are implicitly closed.
        assertTrue(description, description.contains("<li>Game version: v1.0.2.12</li>"))
        // Entities: &#038; is re-escaped as &amp;, &#8217; is emitted as the character itself.
        assertTrue(description, description.contains("100% Lossless &amp; MD5 Perfect"))
        assertTrue(description, description.contains("voiceovers you don\u2019t need"))
        assertTrue(
            description,
            description.endsWith(
                "<p><strong>Backwards Compatibility</strong></p>" +
                        "<p>This repack is NOT backwards compatible with previous Thronebreaker repack.</p>",
            ),
        )
        assertFalse(description, description.contains("entry-content"))
    }

    @Test
    fun `getDetails returns null when the post has no magnet`() = runTest {
        val provider = FitGirlRepacks(
            fakeNetworkClient(detailsUrl to "<html><body><h1 class=\"entry-title\">Post</h1></body></html>"),
        )

        assertEquals(null, provider.getDetails(detailsUrl))
    }

    @Test
    fun `getMagnetUri returns the first magnet of the post`() = runTest {
        val provider = FitGirlRepacks(fakeNetworkClient(detailsUrl to fixture("fitgirlrepacks/details.html")))

        assertEquals(
            "magnet:?xt=urn:btih:6BD50985E5911E6917B27EB0C9535851ADE457F9",
            provider.getMagnetUri(detailsUrl),
        )
    }

    @Test
    fun `getMagnetUri fails when the post has no magnet`() = runTest {
        val provider = FitGirlRepacks(fakeNetworkClient(detailsUrl to "<html><body></body></html>"))

        val result = runCatching { provider.getMagnetUri(detailsUrl) }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    /** Removes whitespace around tags and collapses whitespace runs, ignoring pretty-printing. */
    private fun normalizeHtml(html: String): String =
        html.replace(Regex("""\s*(<[^>]+>)\s*"""), "$1").replace(Regex("""\s+"""), " ")
}
