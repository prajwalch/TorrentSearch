package com.prajwalch.torrentsearch.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviders

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class DarkTheme {
    On,
    Off,
    FollowSystem {
        override fun toString() = "Follow System"
    };
}

data class MaxNumResults(val n: Int) {
    fun isUnlimited() = n == UNLIMITED_N

    companion object {
        private const val UNLIMITED_N = -1

        val Unlimited = MaxNumResults(n = UNLIMITED_N)
    }
}

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val enableDynamicTheme: Flow<Boolean> = dataStore
        .getOrDefault(key = ENABLE_DYNAMIC_THEME, default = true)

    val darkTheme: Flow<DarkTheme> = dataStore
        .getMapOrDefault(
            key = DARK_THEME,
            map = DarkTheme::valueOf,
            default = DarkTheme.FollowSystem,
        )

    val pureBlack: Flow<Boolean> = dataStore.getOrDefault(key = PURE_BLACK, default = false)

    val defaultCategory: Flow<Category> = dataStore
        .getMapOrDefault(
            key = DEFAULT_CATEGORY,
            map = Category::valueOf,
            default = Category.All,
        )

    val enableNSFWMode: Flow<Boolean> = dataStore
        .getOrDefault(key = ENABLE_NSFW_MODE, default = false)

    val hideResultsWithZeroSeeders: Flow<Boolean> = dataStore
        .getOrDefault(key = HIDE_RESULTS_WITH_ZERO_SEEDERS, default = false)

    val searchProviders: Flow<Set<String>> = dataStore
        .getOrDefault(key = SEARCH_PROVIDERS, default = SearchProviders.enabledIds())

    val maxNumResults: Flow<MaxNumResults> = dataStore
        .getMapOrDefault(
            key = MAX_NUM_RESULTS,
            map = ::MaxNumResults,
            default = MaxNumResults.Unlimited,
        )

    val saveSearchHistory: Flow<Boolean> = dataStore
        .getOrDefault(key = SAVE_SEARCH_HISTORY, default = true)

    val showSearchHistory: Flow<Boolean> = dataStore
        .getOrDefault(key = SHOW_SEARCH_HISTORY, default = true)

    suspend fun updateEnableDynamicTheme(enable: Boolean) {
        dataStore.setOrUpdate(key = ENABLE_DYNAMIC_THEME, enable)
    }

    suspend fun updateDarkTheme(darkTheme: DarkTheme) {
        dataStore.setOrUpdate(key = DARK_THEME, value = darkTheme.name)
    }

    suspend fun updatePureBlack(enable: Boolean) {
        dataStore.setOrUpdate(key = PURE_BLACK, value = enable)
    }

    suspend fun updateDefaultCategory(category: Category) {
        dataStore.setOrUpdate(key = DEFAULT_CATEGORY, value = category.name)
    }

    suspend fun updateEnableNSFWMode(enable: Boolean) {
        dataStore.setOrUpdate(key = ENABLE_NSFW_MODE, value = enable)
    }

    suspend fun updateHideResultsWithZeroSeeders(enable: Boolean) {
        dataStore.setOrUpdate(key = HIDE_RESULTS_WITH_ZERO_SEEDERS, value = enable)
    }

    suspend fun updateSearchProviders(providers: Set<SearchProviderId>) {
        dataStore.setOrUpdate(key = SEARCH_PROVIDERS, value = providers)
    }

    suspend fun updateMaxNumResults(maxNumResults: MaxNumResults) {
        dataStore.setOrUpdate(key = MAX_NUM_RESULTS, value = maxNumResults.n)
    }

    suspend fun updateSaveSearchHistory(save: Boolean) {
        dataStore.setOrUpdate(key = SAVE_SEARCH_HISTORY, value = save)
    }

    suspend fun updateShowSearchHistory(show: Boolean) {
        dataStore.setOrUpdate(key = SHOW_SEARCH_HISTORY, value = show)
    }

    private companion object PreferencesKeys {
        // Appearance keys.
        val ENABLE_DYNAMIC_THEME = booleanPreferencesKey("enable_dynamic_theme")
        val DARK_THEME = stringPreferencesKey("dark_theme")
        val PURE_BLACK = booleanPreferencesKey("pure_black")

        // General keys.
        val DEFAULT_CATEGORY = stringPreferencesKey("default_category")
        val ENABLE_NSFW_MODE = booleanPreferencesKey("enable_nsfw_mode")

        // Search keys.
        val HIDE_RESULTS_WITH_ZERO_SEEDERS = booleanPreferencesKey("hide_results_with_zero_seeders")
        val SEARCH_PROVIDERS = stringSetPreferencesKey("search_providers")
        val MAX_NUM_RESULTS = intPreferencesKey("max_num_results")

        // Search history.
        val SAVE_SEARCH_HISTORY = booleanPreferencesKey("save_search_history")
        val SHOW_SEARCH_HISTORY = booleanPreferencesKey("show_search_history")
    }
}

private fun <T> DataStore<Preferences>.getOrDefault(key: Preferences.Key<T>, default: T): Flow<T> {
    return data.map { preferences -> preferences[key] ?: default }
}

private fun <T, U> DataStore<Preferences>.getMapOrDefault(
    key: Preferences.Key<T>,
    map: (T) -> U,
    default: U,
): Flow<U> {
    return data.map { preferences -> preferences[key]?.let(map) ?: default }
}

private suspend fun <T> DataStore<Preferences>.setOrUpdate(key: Preferences.Key<T>, value: T) {
    edit { preferences -> preferences[key] = value }
}