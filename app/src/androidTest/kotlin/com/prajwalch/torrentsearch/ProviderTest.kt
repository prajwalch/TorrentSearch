package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.SearchContext
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Integration test for verifying that a provider returns actual torrents.
 */
class ProviderTest {

    // Change this to test a different provider
    private val provider = AnimeTosho()


    @Test
    fun searchReturnsRealTorrentsFromProvider() = runBlocking {
        val searchQuery = "One Piece"

        val context = SearchContext(
            category = Category.Anime,
            httpClient = HttpClient
        )


        val results: List<Torrent> = provider.search(searchQuery, context)

        // Basic validations
        assertNotNull("Expected non-null result", results)
        assertTrue("Expected non-empty result list", results.isNotEmpty())

        val first = results.first()

        println(
            """
            ✅ First Torrent Result:
            ├── Name        : ${first.name}
            ├── Magnet URI  : ${first.magnetUri()}
            ├── Size        : ${first.size}
            ├── Seeds       : ${first.seeds}
            ├── Peers       : ${first.peers}
            ├── Upload Date : ${first.uploadDate}
            └── Page URL    : ${first.descriptionPageUrl}
            """.trimIndent()
        )

        // Detailed assertions
        assertTrue("Torrent name should not be blank", first.name.isNotBlank())
        assertTrue("Torrent size should not be blank", first.size.isNotBlank())
        assertTrue("Torrent magnet URI should be valid", first.magnetUri().startsWith("magnet:?"))
    }
}
