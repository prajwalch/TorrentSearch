package com.prajwalch.torrentsearch.ui.settings.defaultcategory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.model.Category

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlin.time.Duration.Companion.seconds

@org.koin.android.annotation.KoinViewModel
class DefaultCategoryViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState = settingsRepository
        .defaultCategory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = Category.All,
        )

    fun setDefaultCategory(category: Category) {
        viewModelScope.launch {
            settingsRepository.setDefaultCategory(category)
        }
    }
}