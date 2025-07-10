package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.Settings
import com.prajwalch.torrentsearch.data.SettingsRepository

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings = repository
        .settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = repository.defaultSettings,
        )

    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            repository.updateSettings(settings = settings)
        }
    }
}

class SettingsViewModelFactory(private val settingsManager: SettingsRepository) :
    ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(repository = settingsManager) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}