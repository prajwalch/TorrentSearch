package com.prajwalch.torrentsearch.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.DarkTheme
import com.prajwalch.torrentsearch.models.MaxNumResults
import com.prajwalch.torrentsearch.models.SortCriteria
import com.prajwalch.torrentsearch.models.SortOptions
import com.prajwalch.torrentsearch.models.SortOrder
import com.prajwalch.torrentsearch.providers.SearchProviderId

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

import javax.inject.Inject

class SettingsRepository @Inject constructor(
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

    val pureBlack: Flow<Boolean> = dataStore
        .getOrDefault(key = PURE_BLACK, default = false)

    val enableNSFWMode: Flow<Boolean> = dataStore
        .getOrDefault(key = ENABLE_NSFW_MODE, default = false)

    val enabledSearchProvidersId: Flow<Set<SearchProviderId>> = dataStore
        .getOrDefault(
            key = ENABLED_SEARCH_PROVIDERS_ID,
            default = searchProvidersRepository.getEnabledSearchProvidersId(),
        )

    val defaultCategory: Flow<Category> = dataStore
        .getMapOrDefault(
            key = DEFAULT_CATEGORY,
            map = Category::valueOf,
            default = Category.All,
        )

    val defaultSortOptions: Flow<SortOptions> = combine(
        dataStore.getMapOrDefault(
            key = DEFAULT_SORT_CRITERIA,
            map = SortCriteria::valueOf,
            default = SortCriteria.Default
        ),
        dataStore.getMapOrDefault(
            key = DEFAULT_SORT_ORDER,
            map = SortOrder::valueOf,
            default = SortOrder.Default,
        ),
        ::SortOptions,
    )

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

    val enableShareIntegration: Flow<Boolean> = dataStore
        .getOrDefault(key = ENABLE_SHARE_INTEGRATION, default = true)

    val enableQuickSearch: Flow<Boolean> = dataStore
        .getOrDefault(key = ENABLE_QUICK_SEARCH, default = true)

    val bookmarksSortOptions: Flow<SortOptions> = combine(
        dataStore.getMapOrDefault(
            key = BOOKMARKS_SORT_CRITERIA,
            map = SortCriteria::valueOf,
            default = SortCriteria.Default,
        ),
        dataStore.getMapOrDefault(
            key = BOOKMARKS_SORT_ORDER,
            map = SortOrder::valueOf,
            default = SortOrder.Default,
        ),
        ::SortOptions,
    )

    suspend fun enableDynamicTheme(enable: Boolean) {
        dataStore.setOrUpdate(key = ENABLE_DYNAMIC_THEME, enable)
    }

    suspend fun setDarkTheme(darkTheme: DarkTheme) {
        dataStore.setOrUpdate(key = DARK_THEME, value = darkTheme.name)
    }

    suspend fun enablePureBlack(enable: Boolean) {
        dataStore.setOrUpdate(key = PURE_BLACK, value = enable)
    }

    suspend fun enableNSFWMode(enable: Boolean) {
        dataStore.setOrUpdate(key = ENABLE_NSFW_MODE, value = enable)
    }

    suspend fun setEnabledSearchProvidersId(providersId: Set<SearchProviderId>) {
        dataStore.setOrUpdate(key = ENABLED_SEARCH_PROVIDERS_ID, value = providersId)
    }

    suspend fun setDefaultCategory(category: Category) {
        dataStore.setOrUpdate(key = DEFAULT_CATEGORY, value = category.name)
    }

    suspend fun setDefaultSortCriteria(sortCriteria: SortCriteria) {
        dataStore.setOrUpdate(key = DEFAULT_SORT_CRITERIA, value = sortCriteria.name)
    }

    suspend fun setDefaultSortOrder(sortOrder: SortOrder) {
        dataStore.setOrUpdate(key = DEFAULT_SORT_ORDER, value = sortOrder.name)
    }

    suspend fun setMaxNumResults(maxNumResults: MaxNumResults) {
        dataStore.setOrUpdate(key = MAX_NUM_RESULTS, value = maxNumResults.n)
    }

    suspend fun enableSaveSearchHistory(enable: Boolean) {
        dataStore.setOrUpdate(key = SAVE_SEARCH_HISTORY, value = enable)
    }

    suspend fun enableShowSearchHistory(show: Boolean) {
        dataStore.setOrUpdate(key = SHOW_SEARCH_HISTORY, value = show)
    }

    suspend fun enableShareIntegration(enable: Boolean) {
        dataStore.setOrUpdate(key = ENABLE_SHARE_INTEGRATION, value = enable)
    }

    suspend fun enableQuickSearch(enable: Boolean) {
        dataStore.setOrUpdate(key = ENABLE_QUICK_SEARCH, value = enable)
    }

    suspend fun setBookmarksSortCriteria(criteria: SortCriteria) {
        dataStore.setOrUpdate(key = BOOKMARKS_SORT_CRITERIA, value = criteria.name)
    }

    suspend fun setBookmarksSortOrder(order: SortOrder) {
        dataStore.setOrUpdate(key = BOOKMARKS_SORT_ORDER, value = order.name)
    }

    private companion object PreferencesKeys {
        // Appearance
        val ENABLE_DYNAMIC_THEME = booleanPreferencesKey("enable_dynamic_theme")
        val DARK_THEME = stringPreferencesKey("dark_theme")
        val PURE_BLACK = booleanPreferencesKey("pure_black")

        // General
        val ENABLE_NSFW_MODE = booleanPreferencesKey("enable_nsfw_mode")

        // Search
        val ENABLED_SEARCH_PROVIDERS_ID = stringSetPreferencesKey("enabled_search_providers_id")
        val DEFAULT_CATEGORY = stringPreferencesKey("default_category")
        val DEFAULT_SORT_CRITERIA = stringPreferencesKey("default_sort_criteria")
        val DEFAULT_SORT_ORDER = stringPreferencesKey("default_sort_order")
        val MAX_NUM_RESULTS = intPreferencesKey("max_num_results")

        // Search history
        val SAVE_SEARCH_HISTORY = booleanPreferencesKey("save_search_history")
        val SHOW_SEARCH_HISTORY = booleanPreferencesKey("show_search_history")

        // Advanced
        val ENABLE_SHARE_INTEGRATION = booleanPreferencesKey("enable_share_integration")
        val ENABLE_QUICK_SEARCH = booleanPreferencesKey("enable_quick_search")

        // Bookmarks screen sort options.
        val BOOKMARKS_SORT_CRITERIA = stringPreferencesKey("bookmarks_sort_criteria")
        val BOOKMARKS_SORT_ORDER = stringPreferencesKey("bookmarks_sort_order")
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