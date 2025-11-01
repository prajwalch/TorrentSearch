package com.prajwalch.torrentsearch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.models.DarkTheme

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class MainUiState(
    val enableDynamicTheme: Boolean = true,
    val darkTheme: DarkTheme = DarkTheme.FollowSystem,
    val pureBlack: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState = combine(
        settingsRepository.enableDynamicTheme,
        settingsRepository.darkTheme,
        settingsRepository.pureBlack,
        ::MainUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = MainUiState(),
    )
}