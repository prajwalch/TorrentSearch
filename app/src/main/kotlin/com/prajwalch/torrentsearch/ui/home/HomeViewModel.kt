package com.prajwalch.torrentsearch.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.SearchHistory
import com.prajwalch.torrentsearch.models.SearchHistoryId

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class HomeUiState(
    val histories: List<SearchHistory> = emptyList(),
    val categories: List<Category> = Category.entries,
    val selectedCategory: Category = Category.All,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val selectedCategory = MutableStateFlow(Category.All)

    val uiState = combine(
        searchHistoryRepository.observeAllSearchHistories(),
        selectedCategory,
        settingsRepository.enableNSFWMode,
        settingsRepository.showSearchHistory,
    ) { searchHistories, selectedCategory, nsfwModeEnabled, showSearchHistory ->
        val histories = if (showSearchHistory) searchHistories else emptyList()

        val categories = Category.entries.filter { nsfwModeEnabled || !it.isNSFW }
        val selectedCategory = when {
            selectedCategory in categories -> selectedCategory
            else -> Category.All
        }

        HomeUiState(
            histories = histories,
            categories = categories,
            selectedCategory = selectedCategory,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = HomeUiState(),
    )

    init {
        loadDefaultCategory()
    }

    private fun loadDefaultCategory() = viewModelScope.launch {
        val defaultCategory = settingsRepository.defaultCategory.firstOrNull()

        if (defaultCategory != null) {
            selectedCategory.value = defaultCategory
        }
    }

    fun setCategory(category: Category) {
        selectedCategory.value = category
    }

    fun deleteSearchHistory(id: SearchHistoryId) {
        viewModelScope.launch {
            searchHistoryRepository.deleteSearchHistoryById(id = id)
        }
    }
}