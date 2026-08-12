package com.prajwalch.torrentsearch

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.providers.Knaben
import com.prajwalch.torrentsearch.providers.SearchProvider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ✅ Generic provider tests
 *
 * This test suite verifies that any [SearchProvider] implementation can return
 * meaningful search results across a variety of queries and categories.
 */
class ProviderTest {
    private val testContext = ApplicationProvider.getApplicationContext<Context>()
    private val testDataStore = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO),
        produceFile = { testContext.preferencesDataStoreFile("test_settings") },
    )

    // Change this to test any specific provider implementation
    private val provider = Knaben(NetworkClient(SettingsRepository(testDataStore)))

    /**
     * 🔎 Basic Search Test
     *
     * Tests whether a single query returns valid torrent results
     * for a specific category from the current provider.
     */
    @Test
    fun searchReturnsRealTorrentsFromProvider() = runBlocking {
        val searchQuery = "One Piece"
        val results: List<Torrent> = provider.search(searchQuery, Category.Books)

        assertNotNull("Expected non-null result", results)
        assertTrue("Expected non-empty result list", results.isNotEmpty())

        val first = results.first()
        println(
            """
            ✅ First Torrent Result:
            ├── Name          : ${first.name}
            ├── Magnet Uri    : ${first.magnetUri()}
            ├── Size          : ${first.size}
            ├── Seeders       : ${first.seeders}
            ├── Peers         : ${first.peers}
            ├── Upload Date   : ${first.uploadDate}
            ├── Category      : ${first.category}
            └── Page URL      : ${first.descriptionPageUrl}
            └── Provider Name : ${first.providerName}
            """.trimIndent()
        )

        assertTrue("Torrent name should not be blank", first.name.isNotBlank())
        assertTrue("Torrent size should not be blank", first.size?.isNotBlank() == true)
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
            val results = provider.search(query, category)

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
                ├── Seeders       : ${first.seeders}
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