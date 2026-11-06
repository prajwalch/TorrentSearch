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

class MegaPeerTest {
    private val searchUrl =
        "https://megapeer.vip/browse.php?search=windows&age=&cat=29&stype=0&sort=0&ascdesc=0"
    private val detailsUrl =
        "https://megapeer.vip/torrent/158695/windows-7-professionalnaya-vl-sp1-2in1-build-61760126321-2023-pc-by-ivandubskoj"
    private val magnetUri =
        "magnet:?xt=urn:btih:52ec113bc604110589fa2357710aa2b90331a0ae" +
                "&dn=windows-7-professionalnaya-vl-sp1-2in1-build-61760126321-2023-pc-by-ivandubskoj" +
                "&tr=udp://tracker.dler.org:6969/announce&tr=udp://tracker.opentrackr.org:1337"

    @Test
    fun `search parses every result row`() = runTest {
        val provider = MegaPeer(fakeNetworkClient(searchUrl to fixture("megapeer/search.html")))

        val torrents = provider.search(query = "windows", category = Category.Apps)

        assertEquals(
            listOf(
                "WOBID: Windows Optimizer by ivandubskoj 1.0 [x64] (2026) PC",
                "Windows 11 3in1 x64 25H2 [Build 26200.8246] [Update 17.04.2026] (2026) PC от ivandubskoj | RUS",
                "Windows Server 2022 LTSC, Version 21H2 Build 20348.1487 [Updated January 2023] - " +
                        "Оригинальные образы от Microsoft MSDN (2023) PC | RUS ENG",
                "Windows 7 Профессиональная VL SP1 2in1 [build 6.1.7601.26321] (2023) PC by ivandubskoj | RUS",
            ),
            torrents.map { it.name },
        )
        // The first row has an extra comments column, which shifts the size and
        // peers cells: the parser reads the comment count as size and finds no
        // seeders/peers.
        assertEquals(listOf("1", "3.52 GB", "9.70 GB", "3.23 GB"), torrents.map { it.size })
        assertEquals(listOf(null, 2u, 16u, 4u), torrents.map { it.seeders })
        assertEquals(listOf(null, 0u, 28u, 5u), torrents.map { it.peers })
        // Russian month abbreviations ("4 Июн 26", "17 Апр 26", "18 Янв 23", "14 Янв 23").
        assertEquals(
            listOf(
                "2026-06-04T00:00:00Z",
                "2026-04-17T00:00:00Z",
                "2023-01-18T00:00:00Z",
                "2023-01-14T00:00:00Z",
            ).map(Instant::parse),
            torrents.map { it.uploadDate },
        )
        assertEquals(
            listOf(
                "https://megapeer.vip/torrent/207626/wobid-windows-optimizer-by-ivandubskoj-10-x64-2026-pc",
                "https://megapeer.vip/torrent/204874/windows-11-3in1-x64-25h2-build-262008246-update-17042026-2026-pc",
                "https://megapeer.vip/torrent/160118/windows-server-2022-ltsc-version-21h2-build-203481487-" +
                        "updated-january-2023-originalnie-obrazi-ot-microsoft-msdn-2023-pc",
                detailsUrl,
            ),
            torrents.map { it.detailsPageUrl },
        )
        assertEquals(
            listOf(207626, 204874, 160118, 158695).map { "https://megapeer.vip/download/$it" },
            torrents.map { it.fileDownloadLink },
        )
        assertEquals(torrents.map { MagnetUri.RequiresFetch(it.detailsPageUrl!!) }, torrents.map { it.magnetUri })
        assertTrue(torrents.all { it.providerName == "MegaPeer" })
        assertEquals(4, torrents.map { it.id }.toSet().size)
    }

    @Test
    fun `search returns no results for a page without the results table`() = runTest {
        val provider = MegaPeer(fakeNetworkClient(searchUrl to "<html><body><div id=\"index\"></div></body></html>"))

        assertEquals(emptyList<Any>(), provider.search(query = "windows", category = Category.Apps))
    }

    @Test
    fun `getDetails parses the details page`() = runTest {
        val provider = MegaPeer(fakeNetworkClient(detailsUrl to fixture("megapeer/details.html")))

        val details = provider.getDetails(detailsUrl)

        assertNotNull(details)
        details!!
        assertEquals(
            "Windows 7 Профессиональная VL SP1 2in1 [build 6.1.7601.26321] (2023) PC by ivandubskoj | RUS",
            details.name,
        )
        assertEquals("52ec113bc604110589fa2357710aa2b90331a0ae", details.infoHash)
        assertEquals(magnetUri, details.magnetUri)
        assertEquals("3.23 GB", details.size)
        assertEquals(4u, details.seeders)
        assertEquals(5u, details.peers)
        assertEquals(Category.Apps, details.category)
        assertNull(details.uploadDate)
        assertEquals("https://megapeer.vip/download/158695", details.fileDownloadLink)
        assertEquals(
            "https://i121.fastpic.org/big/2023/0114/ab/3f8d43d574f2289368e1401e6d016aab.jpg",
            details.posterUrl,
        )
        assertEquals(
            listOf(
                "https://i121.fastpic.org/thumb/2023/0114/eb/1d6d090f9fb16a3cd168c70e0aeb28eb.jpeg",
                "https://i121.fastpic.org/thumb/2023/0114/a8/8dfd3555eb73abf9af56cae6283b7aa8.jpeg",
            ),
            details.screenshotUrls,
        )
        // Poster and spoilers (checksums, screenshots) are stripped from the description.
        assertEquals(
            "<br>\n" +
            "<br>\n" +
            "<font size=\"5\"><font color=\"brown\"><span style=\"font-family: Century Gothic;\">Windows 7 Профессиональная VL SP1 2in1 </span></font></font>\n" +
            "<br>\n" +
            "<font size=\"2\"><span style=\"font-family: Century Gothic;\"><b>Год: </b>12.01.2023\n" +
            "  <br> \n" +
            "  <b>Язык интерфейса:</b> Русский\n" +
            "  <br> \n" +
            "  <b>Описание:</b> <font color=\"mediumblue\">Авторская сборка из 2-х русских редакций Windows 7 Профессиональная VL SP1 (x86-x64) на основе оригинальных образов.</font>\n" +
            "  <br> \n" +
            "  <br> </span></font>\n" +
            "<br>\n" +
            "<center>\n" +
            " <img src=\"https://i120.fastpic.org/big/2022/1117/7f/17cc38711679c6165f7ffcbdef48817f.gif\">\n" +
            " <br>\n" +
            "</center>\n" +
            "<br>",
            details.description,
        )
    }

    @Test
    fun `getDetails returns null when the page has no magnet link`() = runTest {
        val provider = MegaPeer(fakeNetworkClient(detailsUrl to "<html><body><h1>Нет раздачи</h1></body></html>"))

        assertNull(provider.getDetails(detailsUrl))
    }

    @Test
    fun `getMagnetUri extracts the magnet link from the details page`() = runTest {
        val provider = MegaPeer(fakeNetworkClient(detailsUrl to fixture("megapeer/details.html")))

        assertEquals(magnetUri, provider.getMagnetUri(detailsUrl))
    }
}
