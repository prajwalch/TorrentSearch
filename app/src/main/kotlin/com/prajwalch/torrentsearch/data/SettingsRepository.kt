package com.prajwalch.torrentsearch.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

import com.prajwalch.torrentsearch.providers.ProviderId
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

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val enableDynamicTheme: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ENABLE_DYNAMIC_THEME] ?: true
    }

    val darkTheme: Flow<DarkTheme> = dataStore.data.map { preferences ->
        preferences[DARK_THEME]?.let(DarkTheme::fromInt) ?: DarkTheme.FollowSystem
    }

    val enableNSFWSearch: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ENABLE_NSFW_SEARCH] ?: false
    }

    val searchProviders: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[SEARCH_PROVIDERS] ?: SearchProviders.ids()
    }

    suspend fun updateEnableDynamicTheme(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ENABLE_DYNAMIC_THEME] = enabled
        }
    }

    suspend fun updateDarkTheme(darkTheme: DarkTheme) {
        dataStore.edit { preferences ->
            preferences[DARK_THEME] = darkTheme.ordinal
        }
    }

    suspend fun updateEnableNSFWSearch(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ENABLE_NSFW_SEARCH] = enabled
        }
    }

    suspend fun updateSearchProviders(providers: Set<ProviderId>) {
        dataStore.edit { preferences ->
            preferences[SEARCH_PROVIDERS] = providers
        }
    }

    private companion object PreferencesKeys {
        val ENABLE_DYNAMIC_THEME = booleanPreferencesKey("enable_dynamic_theme")
        val DARK_THEME = intPreferencesKey("dark_theme")
        val ENABLE_NSFW_SEARCH = booleanPreferencesKey("enable_nsfw_search")
        val SEARCH_PROVIDERS = stringSetPreferencesKey("search_providers")
    }
}