package com.prajwalch.torrentsearch.ui.settings.defaultsortoptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.models.SortCriteria
import com.prajwalch.torrentsearch.domain.models.SortOptions
import com.prajwalch.torrentsearch.domain.models.SortOrder

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class DefaultSortOptionsViewModel @Inject constructor(
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