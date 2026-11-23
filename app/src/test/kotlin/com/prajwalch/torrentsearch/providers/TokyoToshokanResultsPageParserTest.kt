package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.runBlocking

import org.junit.Assert.assertEquals
import org.junit.Test

class TokyoToshokanResultsPageParserTest {
    private val parser = TokyoToshokanResultsPageParser(
        providerId = "tokyotoshokan",
        providerName = "TokyoToshokan",
    )

    @Test
    fun parse_deduplicatesDuplicateMagnetHashes_keepingFirstInOrder() = runBlocking {
        val hashA = "1111111111111111111111111111111111111111"
        val hashB = "2222222222222222222222222222222222222222"
        val html = """
            <html><body>
            <table class="listing"><tbody>
                <tr><th>Category</th><th>Name</th></tr>
                <tr class="category_0">
                    <td><a href="/?cat=1">Anime</a></td>
                    <td class="desc-top">
                        <a href="magnet:?xt=urn:btih:$hashA&amp;dn=first">[M]</a>
                        <a href="https://tokyotosho.info/1.torrent">My Father and My Son 1080p</a>
                    </td>
                    <td class="web"><a href="details.php?id=1">D</a></td>
                </tr>
                <tr class="category_0">
                    <td class="desc-bot">Submitter: anon | Size: 1.40GB | Date: 2026-11-20 12:30 UTC</td>
                    <td class="stats"><span>12</span><span>3</span></td>
                </tr>
                <tr class="category_0">
                    <td><a href="/?cat=1">Anime</a></td>
                    <td class="desc-top">
                        <a href="magnet:?xt=urn:btih:$hashB&amp;dn=second">[M]</a>
                        <a href="https://tokyotosho.info/2.torrent">My Father and My Son 720p</a>
                    </td>
                    <td class="web"><a href="details.php?id=2">D</a></td>
                </tr>
                <tr class="category_0">
                    <td class="desc-bot">Submitter: anon | Size: 700MB | Date: 2026-11-20 12:31 UTC</td>
                    <td class="stats"><span>8</span><span>1</span></td>
                </tr>
                <tr class="category_0">
                    <td><a href="/?cat=1">Anime</a></td>
                    <td class="desc-top">
                        <a href="magnet:?xt=urn:btih:$hashA&amp;dn=duplicate">[M]</a>
                        <a href="https://tokyotosho.info/3.torrent">My Father and My Son Repost</a>
                    </td>
                    <td class="web"><a href="details.php?id=3">D</a></td>
                </tr>
                <tr class="category_0">
                    <td class="desc-bot">Submitter: anon | Size: 1.40GB | Date: 2026-11-20 12:32 UTC</td>
                    <td class="stats"><span>4</span><span>0</span></td>
                </tr>
            </tbody></table>
            </body></html>
        """.trimIndent()

        val results = parser.parse(
            html = html,
            pageUrl = "https://tokyotosho.info/search.php?terms=My+Father+and+My+Son",
        )

        assertEquals(
            listOf("My Father and My Son 1080p", "My Father and My Son 720p"),
            results.map { it.name },
        )
        assertEquals(
            listOf(
                TorrentUtils.createTorrentId("tokyotoshokan", hashA),
                TorrentUtils.createTorrentId("tokyotoshokan", hashB),
            ),
            results.map { it.id },
        )
    }

    @Test
    fun parse_keepsEqualTitlesWithDistinctHashes_andSkipsMissingMagnet() = runBlocking {
        val hashA = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val hashB = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        val html = """
            <html><body>
            <table class="listing"><tbody>
                <tr><th>Category</th><th>Name</th></tr>
                <tr class="category_0">
                    <td><a href="/?cat=1">Anime</a></td>
                    <td class="desc-top">
                        <a href="magnet:?xt=urn:btih:$hashA">[M]</a>
                        <a href="https://tokyotosho.info/a.torrent">My Father and My Son</a>
                    </td>
                </tr>
                <tr class="category_0">
                    <td class="desc-bot">Submitter: anon | Size: 1.40GB | Date: 2026-11-20 12:30 UTC</td>
                    <td class="stats"><span>10</span><span>2</span></td>
                </tr>
                <tr class="category_0">
                    <td><a href="/?cat=1">Anime</a></td>
                    <td class="desc-top">
                        <span>[M]</span>
                        <a href="https://tokyotosho.info/missing.torrent">Missing Magnet Entry</a>
                    </td>
                </tr>
                <tr class="category_0">
                    <td class="desc-bot">Submitter: anon | Size: 500MB | Date: 2026-11-20 12:31 UTC</td>
                    <td class="stats"><span>1</span><span>0</span></td>
                </tr>
                <tr class="category_0">
                    <td><a href="/?cat=1">Anime</a></td>
                    <td class="desc-top">
                        <a href="magnet:?xt=urn:btih:$hashB">[M]</a>
                        <a href="https://tokyotosho.info/b.torrent">My Father and My Son</a>
                    </td>
                </tr>
                <tr class="category_0">
                    <td class="desc-bot">Submitter: anon | Size: 700MB | Date: 2026-11-20 12:32 UTC</td>
                    <td class="stats"><span>5</span><span>1</span></td>
                </tr>
            </tbody></table>
            </body></html>
        """.trimIndent()

        val results = parser.parse(
            html = html,
            pageUrl = "https://tokyotosho.info/search.php?terms=My+Father+and+My+Son",
        )

        assertEquals(
            listOf("My Father and My Son", "My Father and My Son"),
            results.map { it.name },
        )
        assertEquals(
            listOf(
                TorrentUtils.createTorrentId("tokyotoshokan", hashA),
                TorrentUtils.createTorrentId("tokyotoshokan", hashB),
            ),
            results.map { it.id },
        )
    }
}
