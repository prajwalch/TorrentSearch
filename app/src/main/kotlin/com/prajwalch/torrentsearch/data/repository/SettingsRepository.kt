package com.prajwalch.torrentsearch.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.providers.SearchProviderId

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    searchProvidersRepository: SearchProvidersRepository,
) {
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

    val enabledSearchProvidersId: Flow<Set<SearchProviderId>> = dataStore
        .getOrDefault(
            key = ENABLED_SEARCH_PROVIDERS_ID,
            default = searchProvidersRepository.defaultEnabledIds(),
        )

    val defaultSortCriteria: Flow<SortCriteria> = dataStore
        .getMapOrDefault(
            key = DEFAULT_SORT_CRITERIA,
            map = SortCriteria::valueOf,
            default = SortCriteria.Default,
        )

    val defaultSortOrder: Flow<SortOrder> = dataStore
        .getMapOrDefault(
            key = DEFAULT_SORT_ORDER,
            map = SortOrder::valueOf,
            default = SortOrder.Default,
        )

    val hideResultsWithZeroSeeders: Flow<Boolean> = dataStore
        .getOrDefault(key = HIDE_RESULTS_WITH_ZERO_SEEDERS, default = false)

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

    suspend fun updateEnabledSearchProvidersId(providersId: Set<SearchProviderId>) {
        dataStore.setOrUpdate(key = ENABLED_SEARCH_PROVIDERS_ID, value = providersId)
    }

    suspend fun updateDefaultSortCriteria(sortCriteria: SortCriteria) {
        dataStore.setOrUpdate(key = DEFAULT_SORT_CRITERIA, value = sortCriteria.name)
    }

    suspend fun updateDefaultSortOrder(sortOrder: SortOrder) {
        dataStore.setOrUpdate(key = DEFAULT_SORT_ORDER, value = sortOrder.name)
    }

    suspend fun updateHideResultsWithZeroSeeders(enable: Boolean) {
        dataStore.setOrUpdate(key = HIDE_RESULTS_WITH_ZERO_SEEDERS, value = enable)
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
        val ENABLE_NSFW_MODE = booleanPreferencesKey("enable_nsfw_mode")

        // Search keys.
        val ENABLED_SEARCH_PROVIDERS_ID = stringSetPreferencesKey("enabled_search_providers_id")
        val DEFAULT_CATEGORY = stringPreferencesKey("default_category")
        val DEFAULT_SORT_CRITERIA = stringPreferencesKey("default_sort_criteria")
        val DEFAULT_SORT_ORDER = stringPreferencesKey("default_sort_order")
        val HIDE_RESULTS_WITH_ZERO_SEEDERS = booleanPreferencesKey("hide_results_with_zero_seeders")
        val MAX_NUM_RESULTS = intPreferencesKey("max_num_results")

        // Search history.
        val SAVE_SEARCH_HISTORY = booleanPreferencesKey("save_search_history")
        val SHOW_SEARCH_HISTORY = booleanPreferencesKey("show_search_history")
    }
}

/** Returns a pre-saved preferences or `default` if it doesn't exist. */
private fun <T> DataStore<Preferences>.getOrDefault(key: Preferences.Key<T>, default: T): Flow<T> {
    return data.map { preferences -> preferences[key] ?: default }
}

/**
 * Returns a pre-saved preferences after applying a function or `default`
 * if it doesn't exist.
 */
private fun <T, U> DataStore<Preferences>.getMapOrDefault(
    key: Preferences.Key<T>,
    map: (T) -> U,
    default: U,
): Flow<U> {
    return data.map { preferences -> preferences[key]?.let(map) ?: default }
}

/** Sets a preferences or updates if it already exists .*/
private suspend fun <T> DataStore<Preferences>.setOrUpdate(key: Preferences.Key<T>, value: T) {
    edit { preferences -> preferences[key] = value }
}

/** Dark theme options. */
enum class DarkTheme {
    On,
    Off,
    FollowSystem {
        override fun toString() = "Follow System"
    };
}

/** Results sort criteria. */
enum class SortCriteria {
    Name,
    Seeders,
    Peers,
    FileSize {
        override fun toString() = "File size"
    },
    Date;

    companion object {
        /** The default criteria. */
        val Default = Seeders
    }
}

/** Results sort order. */
enum class SortOrder {
    Ascending,
    Descending;

    /** Returns the opposite order. */
    fun opposite() = when (this) {
        Ascending -> Descending
        Descending -> Ascending
    }

    companion object {
        /** The default sort order. */
        val Default = Descending
    }
}

/** Defines maximum number of results to be shown. */
data class MaxNumResults(val n: Int) {
    fun isUnlimited() = n == UNLIMITED_N

    companion object {
        private const val UNLIMITED_N = -1

        val Unlimited = MaxNumResults(n = UNLIMITED_N)
    }
}