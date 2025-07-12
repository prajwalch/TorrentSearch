package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.SearchContext
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * ✅ Generic provider tests
 *
 * This test suite verifies that any [Provider] implementation can return
 * meaningful search results across a variety of queries and categories.
 */
class ProviderTest {

    // Change this to test any specific provider implementation
    private val provider = Knaben(id = "")

    /**
     * 🔎 Basic Search Test
     *
     * Tests whether a single query returns valid torrent results
     * for a specific category from the current provider.
     */
    @Test
    fun searchReturnsRealTorrentsFromProvider() = runBlocking {
        val searchQuery = "One Piece"

        val context = SearchContext(
            category = Category.Books,
            httpClient = HttpClient
        )

        val results: List<Torrent> = provider.search(searchQuery, context)

        assertNotNull("Expected non-null result", results)
        assertTrue("Expected non-empty result list", results.isNotEmpty())

        val first = results.first()
        println(
            """
            ✅ First Torrent Result:
            ├── Name          : ${first.name}
            ├── Magnet Uri    : ${first.magnetUri()}
            ├── Size          : ${first.size}
            ├── Seeds         : ${first.seeds}
            ├── Peers         : ${first.peers}
            ├── Upload Date   : ${first.uploadDate}
            ├── Category      : ${first.category}
            └── Page URL      : ${first.descriptionPageUrl}
            └── Provider Name : ${first.providerName}
            """.trimIndent()
        )

        assertTrue("Torrent name should not be blank", first.name.isNotBlank())
        assertTrue("Torrent size should not be blank", first.size.isNotBlank())
    }

    /**
     * 🧪 Multi-query, Multi-category Test
     *
     * Runs a series of queries across various content categories
     * to verify that the provider supports a broad spectrum of searches.
     */
    @Test
    fun searchMultipleQueriesWithCategoriesReturnsResults() = runBlocking {
        val testCases = listOf(
            "One Piece" to Category.Anime,
            "The Boys" to Category.Series,
            "Wild West Murim" to Category.Books,
            "Computer Science" to Category.Books,
            "Nothing" to Category.All
        )

        testCases.forEach { (query, category) ->
            val context = SearchContext(
                category = category,
                httpClient = HttpClient
            )

            val results = provider.search(query, context)

            println("\n🔎 Testing query: \"$query\" in category: ${category.name}")
            assertNotNull("Expected non-null results for query: $query", results)
            assertTrue("Expected at least one result for query: $query", results.isNotEmpty())

            val first = results.first()
            println(
                """
                ✅ First Torrent Result:
                ├── Name          : ${first.name}
                ├── Magnet Uri    : ${first.magnetUri()}
                ├── Size          : ${first.size}
                ├── Seeds         : ${first.seeds}
                ├── Peers         : ${first.peers}
                ├── Upload Date   : ${first.uploadDate}
                ├── Category      : ${first.category}
                └── Page URL      : ${first.descriptionPageUrl}
                └── Provider Name : ${first.providerName}
                """.trimIndent()
            )
        }
    }
}