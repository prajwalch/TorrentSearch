package com.prajwalch.torrentsearch.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.map

data class Settings(
    val enableDynamicTheme: Boolean = true,
    val darkTheme: DarkTheme = DarkTheme.FollowSystem,
    val enableNSFWSearch: Boolean = false,
)

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

class SettingsRepository(private val settingsDataStore: DataStore<Preferences>) {
    val defaultSettings = Settings()
    val settings = settingsDataStore.data.map { prefs -> readSettings(prefs) }

    private fun readSettings(preferences: Preferences): Settings {
        val enableDynamicTheme = preferences[PreferencesKeys.enableDynamicTheme]
            ?: defaultSettings.enableDynamicTheme
        val darkTheme = preferences[PreferencesKeys.darkTheme]
            ?.let { DarkTheme.fromInt(it) }
            ?: defaultSettings.darkTheme
        val enableNSFWSearch = preferences[PreferencesKeys.enableNSFWSearch]
            ?: defaultSettings.enableNSFWSearch

        return Settings(
            enableDynamicTheme = enableDynamicTheme,
            darkTheme = darkTheme,
            enableNSFWSearch = enableNSFWSearch,
        )
    }

    suspend fun updateSettings(settings: Settings) {
        settingsDataStore.edit { prefs ->
            prefs[PreferencesKeys.enableDynamicTheme] = settings.enableDynamicTheme
            prefs[PreferencesKeys.darkTheme] = settings.darkTheme.ordinal
            prefs[PreferencesKeys.enableNSFWSearch] = settings.enableNSFWSearch
        }
    }

    private object PreferencesKeys {
        val enableDynamicTheme = booleanPreferencesKey("enable_dynamic_theme")
        val darkTheme = intPreferencesKey("dark_theme")
        val enableNSFWSearch = booleanPreferencesKey("enable_nsfw_search")
    }
}