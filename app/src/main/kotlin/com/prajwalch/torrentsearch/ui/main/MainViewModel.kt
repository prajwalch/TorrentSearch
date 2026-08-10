package com.prajwalch.torrentsearch.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.model.DarkTheme

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

import kotlin.time.Duration.Companion.seconds

data class MainUiState(
    val enableDynamicTheme: Boolean = true,
    val darkTheme: DarkTheme = DarkTheme.FollowSystem,
    val pureBlack: Boolean = false,
    val openTorrentDetailsInApp: Boolean = false,
)

@org.koin.android.annotation.KoinViewModel
class MainViewModel(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState = combine(
        settingsRepository.enableDynamicTheme,
        settingsRepository.darkTheme,
        settingsRepository.pureBlack,
        settingsRepository.openTorrentDetailsInApp,
        ::MainUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = MainUiState(),
    )
}