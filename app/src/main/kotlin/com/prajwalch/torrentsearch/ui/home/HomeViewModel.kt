package com.prajwalch.torrentsearch.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.SearchHistory
import com.prajwalch.torrentsearch.domain.models.SearchHistoryId

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow

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
    val searchHistoryEnabled: Boolean = true,
)

private data class HomeSettings(
    val enableNSFWMode: Boolean,
    val saveSearchHistory: Boolean,
    val showSearchHistory: Boolean,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow(Category.All)

    val uiState = combine(
        searchQuery,
        selectedCategory,
        searchHistoryRepository.observeAllSearchHistories(),
        settingsRepository.getHomeSettings(),
        ::createUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = HomeUiState(),
    )

    init {
        loadDefaultCategory()
    }

    private fun createUiState(
        searchQuery: String,
        selectedCategory: Category,
        histories: List<SearchHistory>,
        settings: HomeSettings,
    ): HomeUiState {
        val histories = when {
            !settings.showSearchHistory -> emptyList()
            searchQuery.isBlank() -> histories
            else -> histories.filter { it.query.contains(searchQuery, ignoreCase = true) }
        }

        val categories = Category.entries.filter { settings.enableNSFWMode || !it.isNSFW }
        val selectedCategory = when {
            selectedCategory in categories -> selectedCategory
            else -> Category.All
        }

        return HomeUiState(
            histories = histories,
            categories = categories,
            selectedCategory = selectedCategory,
            searchHistoryEnabled = settings.saveSearchHistory,
        )
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

    fun filterSearchHistories(query: String) {
        searchQuery.value = query
    }

    fun deleteSearchHistory(id: SearchHistoryId) {
        viewModelScope.launch {
            searchHistoryRepository.deleteSearchHistoryById(id = id)
        }
    }

    private fun SettingsRepository.getHomeSettings(): Flow<HomeSettings> =
        combine(
            this.enableNSFWMode,
            this.saveSearchHistory,
            this.showSearchHistory,
            ::HomeSettings,
        )
}