package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.testing.fakeNetworkClient
import com.prajwalch.torrentsearch.testing.fixture

import kotlinx.coroutines.test.runTest

import java.time.Duration
import java.time.Instant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThirteenThirtySevenXTest {
    private val searchUrl = "https://1337x.to/search/ubuntu/1/"
    private val detailsUrl =
        "https://1337x.to/torrent/1001511/The-Treasure-of-The-Sierra-Madre-1948-1080p-BluRay-DTS-HD-x264-BARC0DE/"

    private val magnetUri = "magnet:?xt=urn:btih:8B5D47D19D468281DA97C45854C372223E29F2E5" +
            "&dn=The+Treasure+of+The+Sierra+Madre+1948+1080p+BluRay+DTS-HD+x264-BARC0DE+" +
            "&tr=http%3A%2F%2F121.14.98.151%3A9090%2Fannounce" +
            "&tr=udp%3A%2F%2Fipv4.tracker.harry.lu%3A80%2Fannounce" +
            "&tr=udp%3A%2F%2Ftracker.publicbt.com%3A80%2Fannounce" +
            "&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80%2Fannounce" +
            "&tr=udp%3A%2F%2Ftracker.istole.it%3A80%2Fannounce" +
            "&tr=http%3A%2F%2F94.228.192.98%2Fannounce" +
            "&tr=udp%3A%2F%2F12.rarbg.me%3A80%2Fannounce" +
            "&tr=http%3A%2F%2Fcoppersurfer.tk%3A6969%2Fannounce" +
            "&tr=udp%3A%2F%2Ftracker.token.ro%3A80%2Fannounce" +
            "&tr=udp%3A%2F%2Fbt.rghost.net%3A80%2Fannounce" +
            "&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce" +
            "&tr=http%3A%2F%2Ftracker.openbittorrent.com%3A80%2Fannounce" +
            "&tr=udp%3A%2F%2Fopentracker.i2p.rocks%3A6969%2Fannounce" +
            "&tr=udp%3A%2F%2Ftracker.internetwarriors.net%3A1337%2Fannounce" +
            "&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969%2Fannounce" +
            "&tr=udp%3A%2F%2Fcoppersurfer.tk%3A6969%2Fannounce" +
            "&tr=udp%3A%2F%2Ftracker.zer0day.to%3A1337%2Fannounce"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = ThirteenThirtySevenX(fakeNetworkClient(searchUrl to fixture("thirteenthirtysevenx/search.html")))

        val torrents = provider.search(query = "ubuntu", category = Category.All)

        assertEquals(
            listOf(
                "Ubuntu MATE 16.04.2 [MATE][armhf][img.xz][Uzerus]",
                // Raw '&' in the markup.
                "Linux Command Line (Hands-On) Ubuntu Linux & Linux Scripting",
                // Surrounding whitespace inside the <a> is trimmed.
                "Ubuntu Complete Course",
                "Ubuntu Server 22.04 Fundamentals",
            ),
            torrents.map { it.name },
        )
        // The nested <span class="seeds"> is excluded and "1,004.8 MB" loses its comma.
        assertEquals(listOf("1.1 GB", "2.3 GB", "737.5 MB", "1004.8 MB"), torrents.map { it.size })
        assertEquals(listOf(260u, 65u, 44u, 35u), torrents.map { it.seeders })
        assertEquals(listOf(2u, 6u, 18u, 3u), torrents.map { it.peers })
        assertEquals(
            listOf("2017-03-07", "2023-08-14", "2023-12-23", "2023-05-07")
                .map { Instant.parse("${it}T00:00:00Z") },
            torrents.map { it.uploadDate },
        )
        // /sub/20/0/ -> Apps, /sub/34/0/ -> Other.
        assertEquals(
            listOf(Category.Apps, Category.Other, Category.Other, Category.Other),
            torrents.map { it.category },
        )
        val detailsPageUrls = listOf(
            "https://1337x.to/torrent/2099267/Ubuntu-MATE-16-04-2-MATE-armhf-img-xz-Uzerus/",
            "https://1337x.to/torrent/5767728/Linux-Command-Line-Hands-On-Ubuntu-Linux-Linux-Scripting/",
            "https://1337x.to/torrent/5931509/Ubuntu-Complete-Course/",
            "https://1337x.to/torrent/5649144/Ubuntu-Server-22-04-Fundamentals/",
        )
        assertEquals(detailsPageUrls, torrents.map { it.detailsPageUrl })
        assertEquals(detailsPageUrls, torrents.map { (it.magnetUri as MagnetUri.RequiresFetch).url })
        assertTrue(torrents.all { it.providerName == "1337x" })
        assertEquals(4, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = ThirteenThirtySevenX(
            fakeNetworkClient(searchUrl to "<html><body><div class=\"box-info\"><p>No results were returned.</p></div></body></html>"),
        )

        assertEquals(emptyList<Any>(), provider.search(query = "ubuntu", category = Category.All))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = ThirteenThirtySevenX(fakeNetworkClient(detailsUrl to fixture("thirteenthirtysevenx/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals("The Treasure of The Sierra Madre 1948 1080p BluRay DTS-HD x264-BA...", details.name)
        assertEquals(magnetUri, details.magnetUri)
        assertEquals("8b5d47d19d468281da97c45854c372223e29f2e5", details.infoHash)
        assertEquals("9.8 GB", details.size)
        assertEquals(0u, details.seeders)
        assertEquals(2u, details.peers)
        assertEquals(Category.Movies, details.category)
        assertEquals("harks87", details.uploader)
        // "1 decade ago" is not a supported relative unit.
        assertNull(details.uploadDate)
        // "4 hours ago".
        val now = Instant.now()
        val lastChecked = details.lastChecked!!
        assertTrue(
            lastChecked.toString(),
            lastChecked in now.minus(Duration.ofHours(4).plusMinutes(5))..now.minus(Duration.ofHours(4)),
        )
        // The page only links to "http://itorrents.org/...", which the
        // `https://itorrents.org` prefix selector does not match.
        assertNull(details.fileDownloadLink)
        // Protocol-relative src resolved against the https page URL.
        assertEquals("https://lx1.dyncdn.cc/cdn/69/6952104e990dcdd78e31d18fea979e5d.jpg", details.posterUrl)
        assertEquals(
            """
            <p></p>
            <p class="align-center"><span style="font-size: 15px"><strong>The Treasure of The Sierra Madre 1948 1080p BluRay DTS-HD x264-BARC0DE </strong>
              <br>
              <img src="/images/profile-load.svg" data-original="http://imglooks.com/upload/small/2014/12/07/54847285f0130.jpg" class="img-responsive descrimg lazy">
              <br>Humphrey Bogart, Walter Huston, Tim Holt
              <br></span></p>
            <p><em>Size: 10540998326 bytes (9.82 GiB), duration: 02:06:09, avg.bitrate: 11141 kb/s
              <br>Audio: dts, 48000 Hz, mono (eng)
              <br>Subtitles: eng${" "}
              <br></em></p>
            <p></p>
            """.trimIndent(),
            details.description,
        )
    }

    @Test
    fun `getDetails returns null when the page has no torrent`() = runTest {
        val provider = ThirteenThirtySevenX(fakeNetworkClient(detailsUrl to "<html><body></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }

    @Test
    fun `getMagnetUri extracts the magnet link from the details page`() = runTest {
        val provider = ThirteenThirtySevenX(fakeNetworkClient(detailsUrl to fixture("thirteenthirtysevenx/details.html")))

        assertEquals(magnetUri, provider.getMagnetUri(detailsUrl))
    }
}
