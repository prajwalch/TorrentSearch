package com.prajwalch.torrentsearch.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

import com.prajwalch.torrentsearch.domain.model.DarkTheme
import com.prajwalch.torrentsearch.domain.model.DohProvider
import com.prajwalch.torrentsearch.domain.model.MaxNumResults
import com.prajwalch.torrentsearch.domain.model.SortCriteria
import com.prajwalch.torrentsearch.domain.model.SortOptions
import com.prajwalch.torrentsearch.domain.model.SortOrder
import com.prajwalch.torrentsearch.providers.SearchProviderId

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    /*
     * Appearance settings
     */

    val enableDynamicTheme: Flow<Boolean> =
        dataStore.getOrDefault(ENABLE_DYNAMIC_THEME, true)

    val darkTheme: Flow<DarkTheme> =
        dataStore.getMapOrDefault(DARK_THEME, DarkTheme::valueOf, DarkTheme.FollowSystem)

    val pureBlack: Flow<Boolean> =
        dataStore.getOrDefault(PURE_BLACK, false)

    /*
     * General settings
     */

    val openTorrentDetailsInApp: Flow<Boolean> =
        dataStore.getOrDefault(OPEN_TORRENT_DETAILS_IN_APP, true)

    val enableShareIntegration: Flow<Boolean> =
        dataStore.getOrDefault(ENABLE_SHARE_INTEGRATION, true)

    val enableQuickSearch: Flow<Boolean> =
        dataStore.getOrDefault(ENABLE_QUICK_SEARCH, true)

    /*
     * Content & privacy settings
     */

    val enableNSFWMode: Flow<Boolean> =
        dataStore.getOrDefault(ENABLE_NSFW_MODE, false)

    val blurNSFWImages: Flow<Boolean> =
        dataStore.getOrDefault(BLUR_NSFW_IMAGES, true)

    val saveSearchHistory: Flow<Boolean> =
        dataStore.getOrDefault(SAVE_SEARCH_HISTORY, true)

    val showSearchHistory: Flow<Boolean> =
        dataStore.getOrDefault(SHOW_SEARCH_HISTORY, true)

    /*
     * Search settings
     */

    val enabledSearchProviderIds: Flow<Set<SearchProviderId>?> =
        dataStore.get(ENABLED_SEARCH_PROVIDER_IDS)

    val searchProvidersInitialized: Flow<Boolean> =
        enabledSearchProviderIds.map { it != null }

    val protectionUnlockedProviderIds: Flow<Set<SearchProviderId>> =
        dataStore.getOrDefault(PROTECTION_UNLOCKED_PROVIDER_IDS, emptySet())

    val defaultSortOptions: Flow<SortOptions> =
        combine(
            dataStore.getMapOrDefault(
                DEFAULT_SORT_CRITERIA,
                SortCriteria::valueOf,
                SortCriteria.Default
            ),
            dataStore.getMapOrDefault(
                DEFAULT_SORT_ORDER,
                SortOrder::valueOf,
                SortOrder.Default,
            ),
            ::SortOptions,
        )

    val maxNumResults: Flow<MaxNumResults> =
        dataStore.getMapOrDefault(MAX_NUM_RESULTS, ::MaxNumResults, MaxNumResults.Unlimited)

    /*
     * Network settings
     */

    val dohProvider: Flow<DohProvider> = dataStore
        .getMapOrDefault(DOH_PROVIDER, DohProvider::fromId, DohProvider.Default)

    /*
     * Bookmarks screen related
     */

    val bookmarksSortOptions: Flow<SortOptions> =
        combine(
            dataStore.getMapOrDefault(
                BOOKMARKS_SORT_CRITERIA,
                SortCriteria::valueOf,
                SortCriteria.Default
            ),
            dataStore.getMapOrDefault(
                BOOKMARKS_SORT_ORDER,
                SortOrder::valueOf,
                SortOrder.Default
            ),
            ::SortOptions,
        )

    val showBookmarkSwipeDeleteTip: Flow<Boolean> =
        dataStore.getOrDefault(SHOW_BOOKMARK_SWIPE_DELETE_TIP, true)

    suspend fun enableDynamicTheme(enable: Boolean) {
        dataStore.setOrUpdate(ENABLE_DYNAMIC_THEME, enable)
    }

    suspend fun setDarkTheme(darkTheme: DarkTheme) {
        dataStore.setOrUpdate(DARK_THEME, darkTheme.name)
    }

    suspend fun enablePureBlack(enable: Boolean) {
        dataStore.setOrUpdate(PURE_BLACK, enable)
    }

    suspend fun enableOpenTorrentDetailsInApp(enable: Boolean) {
        dataStore.setOrUpdate(OPEN_TORRENT_DETAILS_IN_APP, enable)
    }

    suspend fun enableShareIntegration(enable: Boolean) {
        dataStore.setOrUpdate(key = ENABLE_SHARE_INTEGRATION, value = enable)
    }

    suspend fun enableQuickSearch(enable: Boolean) {
        dataStore.setOrUpdate(ENABLE_QUICK_SEARCH, enable)
    }

    suspend fun enableNSFWMode(enable: Boolean) {
        dataStore.setOrUpdate(ENABLE_NSFW_MODE, enable)
    }

    suspend fun enableBlurNSFWImages(enable: Boolean) {
        dataStore.setOrUpdate(BLUR_NSFW_IMAGES, enable)
    }

    suspend fun enableSaveSearchHistory(enable: Boolean) {
        dataStore.setOrUpdate(SAVE_SEARCH_HISTORY, enable)
    }

    suspend fun enableShowSearchHistory(show: Boolean) {
        dataStore.setOrUpdate(SHOW_SEARCH_HISTORY, show)
    }

    suspend fun currentEnabledProviderIds(): Set<SearchProviderId>? =
        enabledSearchProviderIds.firstOrNull()

    suspend fun setEnabledSearchProviderIds(ids: Set<SearchProviderId>) {
        dataStore.setOrUpdate(ENABLED_SEARCH_PROVIDER_IDS, ids)
    }

    suspend fun addEnabledSearchProviderId(id: SearchProviderId) {
        dataStore.edit {
            val currentIds = it[ENABLED_SEARCH_PROVIDER_IDS] ?: emptySet()
            it[ENABLED_SEARCH_PROVIDER_IDS] = currentIds + id
        }
    }

    suspend fun addEnabledSearchProviderIds(ids: Set<SearchProviderId>) {
        dataStore.edit {
            val currentIds = it[ENABLED_SEARCH_PROVIDER_IDS] ?: emptySet()
            it[ENABLED_SEARCH_PROVIDER_IDS] = currentIds + ids
        }
    }

    suspend fun removeEnabledSearchProviderId(id: SearchProviderId) {
        dataStore.edit {
            val currentIds = it[ENABLED_SEARCH_PROVIDER_IDS] ?: emptySet()
            it[ENABLED_SEARCH_PROVIDER_IDS] = currentIds - id
        }
    }

    suspend fun removeEnabledSearchProviderIds(ids: Set<SearchProviderId>) {
        dataStore.edit {
            val currentIds = it[ENABLED_SEARCH_PROVIDER_IDS] ?: emptySet()
            it[ENABLED_SEARCH_PROVIDER_IDS] = currentIds - ids
        }
    }

    suspend fun currentProtectionUnlockedProviderIds(): Set<SearchProviderId> =
        protectionUnlockedProviderIds.first()

    suspend fun addProtectionUnlockedProviderId(id: SearchProviderId) {
        dataStore.edit {
            val currentIds = it[PROTECTION_UNLOCKED_PROVIDER_IDS] ?: emptySet()
            it[PROTECTION_UNLOCKED_PROVIDER_IDS] = currentIds + id
        }
    }

    suspend fun removeProtectionUnlockedProviderId(id: SearchProviderId) {
        dataStore.edit {
            it[PROTECTION_UNLOCKED_PROVIDER_IDS]?.let { currentIds ->
                val updatedIds = currentIds - id
                it[PROTECTION_UNLOCKED_PROVIDER_IDS] = updatedIds
            }
        }
    }

    suspend fun setProtectionUnlockedProviderIds(ids: Set<SearchProviderId>) {
        dataStore.setOrUpdate(PROTECTION_UNLOCKED_PROVIDER_IDS, ids)
    }

    suspend fun setDefaultSortCriteria(sortCriteria: SortCriteria) {
        dataStore.setOrUpdate(DEFAULT_SORT_CRITERIA, sortCriteria.name)
    }

    suspend fun setDefaultSortOrder(sortOrder: SortOrder) {
        dataStore.setOrUpdate(DEFAULT_SORT_ORDER, sortOrder.name)
    }

    suspend fun setMaxNumResults(maxNumResults: MaxNumResults) {
        dataStore.setOrUpdate(MAX_NUM_RESULTS, maxNumResults.n)
    }

    suspend fun setDohProvider(provider: DohProvider) {
        dataStore.setOrUpdate(DOH_PROVIDER, provider.id)
    }

    suspend fun setBookmarksSortCriteria(criteria: SortCriteria) {
        dataStore.setOrUpdate(BOOKMARKS_SORT_CRITERIA, criteria.name)
    }

    suspend fun setBookmarksSortOrder(order: SortOrder) {
        dataStore.setOrUpdate(BOOKMARKS_SORT_ORDER, order.name)
    }

    suspend fun showBookmarkSwipeDeleteTip(show: Boolean) {
        dataStore.setOrUpdate(SHOW_BOOKMARK_SWIPE_DELETE_TIP, show)
    }

    private companion object PreferencesKeys {
        // Appearance
        val ENABLE_DYNAMIC_THEME = booleanPreferencesKey("enable_dynamic_theme")
        val DARK_THEME = stringPreferencesKey("dark_theme")
        val PURE_BLACK = booleanPreferencesKey("pure_black")

        // General
        val OPEN_TORRENT_DETAILS_IN_APP = booleanPreferencesKey("open_torrent_details_in_app")
        val ENABLE_SHARE_INTEGRATION = booleanPreferencesKey("enable_share_integration")
        val ENABLE_QUICK_SEARCH = booleanPreferencesKey("enable_quick_search")

        // Content & privacy
        val ENABLE_NSFW_MODE = booleanPreferencesKey("enable_nsfw_mode")
        val BLUR_NSFW_IMAGES = booleanPreferencesKey("blur_nsfw_images")
        val SAVE_SEARCH_HISTORY = booleanPreferencesKey("save_search_history")
        val SHOW_SEARCH_HISTORY = booleanPreferencesKey("show_search_history")

        // Search
        val ENABLED_SEARCH_PROVIDER_IDS = stringSetPreferencesKey("enabled_search_providers_id")
        val PROTECTION_UNLOCKED_PROVIDER_IDS =
            stringSetPreferencesKey("protection_unlocked_provider_ids")
        val DEFAULT_SORT_CRITERIA = stringPreferencesKey("default_sort_criteria")
        val DEFAULT_SORT_ORDER = stringPreferencesKey("default_sort_order")
        val MAX_NUM_RESULTS = intPreferencesKey("max_num_results")

        // Network
        val DOH_PROVIDER = stringPreferencesKey("doh_provider")

        // Bookmarks screen.
        val BOOKMARKS_SORT_CRITERIA = stringPreferencesKey("bookmarks_sort_criteria")
        val BOOKMARKS_SORT_ORDER = stringPreferencesKey("bookmarks_sort_order")
        val SHOW_BOOKMARK_SWIPE_DELETE_TIP =
            booleanPreferencesKey("show_bookmark_swipe_delete_tip")
    }
}

private fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): Flow<T?> =
    data.map { preferences -> preferences[key] }

/** Returns a pre-saved preferences or `default` if it doesn't exist. */
private fun <T> DataStore<Preferences>.getOrDefault(key: Preferences.Key<T>, default: T): Flow<T> =
    data.map { preferences -> preferences[key] ?: default }

/**
 * Returns a pre-saved preferences after applying a function or `default`
 * if it doesn't exist.
 */
private fun <T, U> DataStore<Preferences>.getMapOrDefault(
    key: Preferences.Key<T>,
    map: (T) -> U,
    default: U,
): Flow<U> = data.map { preferences -> preferences[key]?.let(map) ?: default }

/** Sets a preferences or updates if it already exists .*/
private suspend fun <T> DataStore<Preferences>.setOrUpdate(key: Preferences.Key<T>, value: T) {
    edit { preferences -> preferences[key] = value }
}