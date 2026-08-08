package com.prajwalch.torrentsearch.ui.settings.defaultsortoptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.model.SortCriteria
import com.prajwalch.torrentsearch.domain.model.SortOptions
import com.prajwalch.torrentsearch.domain.model.SortOrder

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlin.time.Duration.Companion.seconds

class DefaultSortOptionsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState = settingsRepository
        .defaultSortOptions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = SortOptions(),
        )

    fun setDefaultSortCriteria(criteria: SortCriteria) {
        viewModelScope.launch {
            settingsRepository.setDefaultSortCriteria(criteria)
        }
    }

    fun setDefaultSortOrder(order: SortOrder) {
        viewModelScope.launch {
            settingsRepository.setDefaultSortOrder(order)
        }
    }
}