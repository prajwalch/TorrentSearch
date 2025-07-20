package com.prajwalch.torrentsearch.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

import com.prajwalch.torrentsearch.providers.SearchProviders

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class DarkTheme {
    On,
    Off,
    FollowSystem;

    override fun toString(): String = when (this) {
        On -> "On"
        Off -> "Off"
        FollowSystem -> "Follow System"
    }

    companion object {
        fun fromInt(ordinal: Int): DarkTheme {
            return entries.first { it.ordinal == ordinal }
        }
    }
}

data class MaxNumResults(val n: Int) {
    fun isUnlimited() = n == UNLIMITED_N

    companion object {
        private const val UNLIMITED_N = -1

        val Unlimited = MaxNumResults(n = UNLIMITED_N)
    }
}

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val enableDynamicTheme: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ENABLE_DYNAMIC_THEME] ?: true
    }

    val darkTheme: Flow<DarkTheme> = dataStore.data.map { preferences ->
        preferences[DARK_THEME]?.let(DarkTheme::fromInt) ?: DarkTheme.FollowSystem
    }

    val pureBlack: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PURE_BLACK] ?: false
    }

    val enableNSFWMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ENABLE_NSFW_MODE] ?: false
    }

    val hideResultsWithZeroSeeders: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HIDE_RESULTS_WITH_ZERO_SEEDERS] ?: false
    }

    val searchProviders: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[SEARCH_PROVIDERS] ?: SearchProviders.enabledIds()
    }

    val maxNumResults: Flow<MaxNumResults> = dataStore.data.map { preferences ->
        preferences[MAX_NUM_RESULTS]?.let(::MaxNumResults) ?: MaxNumResults.Unlimited
    }

    suspend fun updateEnableDynamicTheme(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[ENABLE_DYNAMIC_THEME] = enable
        }
    }

    suspend fun updateDarkTheme(darkTheme: DarkTheme) {
        dataStore.edit { preferences ->
            preferences[DARK_THEME] = darkTheme.ordinal
        }
    }

    suspend fun updatePureBlack(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[PURE_BLACK] = enable
        }
    }

    suspend fun updateEnableNSFWMode(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[ENABLE_NSFW_MODE] = enable
        }
    }

    suspend fun updateHideResultsWithZeroSeeders(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[HIDE_RESULTS_WITH_ZERO_SEEDERS] = enable
        }
    }

    suspend fun updateSearchProviders(providers: Set<SearchProviderId>) {
        dataStore.edit { preferences ->
            preferences[SEARCH_PROVIDERS] = providers
        }
    }

    suspend fun updateMaxNumResults(maxNumResults: MaxNumResults) {
        dataStore.edit { preferences ->
            if (maxNumResults.isUnlimited()) {
                preferences.remove(MAX_NUM_RESULTS)
            } else {
                preferences[MAX_NUM_RESULTS] = maxNumResults.n
            }
        }
    }

    private companion object PreferencesKeys {
        // Appearance keys.
        val ENABLE_DYNAMIC_THEME = booleanPreferencesKey("enable_dynamic_theme")
        val DARK_THEME = intPreferencesKey("dark_theme")
        val PURE_BLACK = booleanPreferencesKey("pure_black")

        // General keys.
        val ENABLE_NSFW_MODE = booleanPreferencesKey("enable_nsfw_mode")

        // Search keys.
        val HIDE_RESULTS_WITH_ZERO_SEEDERS = booleanPreferencesKey("hide_results_with_zero_seeders")
        val SEARCH_PROVIDERS = stringSetPreferencesKey("search_providers")
        val MAX_NUM_RESULTS = intPreferencesKey("max_num_results")
    }
}