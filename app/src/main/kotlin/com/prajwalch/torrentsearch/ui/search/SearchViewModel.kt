package com.prajwalch.torrentsearch.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.database.entities.SearchHistory
import com.prajwalch.torrentsearch.data.database.entities.SearchHistoryId
import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.models.Category

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import javax.inject.Inject

data class SearchUiState(
    val histories: List<SearchHistoryItemUiState> = emptyList(),
    val categories: List<Category> = Category.entries,
    val selectedCategory: Category = Category.All,
)

/** Convenient wrapper around the search history entity. */
data class SearchHistoryItemUiState(val id: SearchHistoryId, val query: String)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadDefaultCategory()
        observeNSFWMode()
        observeSearchHistory()
    }

    /** Changes the current category with the given category. */
    fun setCategory(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    /** Deletes the search history associated with given id. */
    fun deleteSearchHistory(id: SearchHistoryId) {
        viewModelScope.launch {
            val searchHistoryItemState = _uiState.value.histories.find { it.id == id }

            searchHistoryItemState?.let {
                val searchHistory = SearchHistory(id = it.id, query = it.query)
                searchHistoryRepository.remove(searchHistory = searchHistory)
            }
        }
    }

    private fun loadDefaultCategory() = viewModelScope.launch {
        val defaultCategory = settingsRepository.defaultCategory.firstOrNull()

        if (defaultCategory != null) {
            _uiState.update {
                it.copy(selectedCategory = defaultCategory)
            }
        }
    }

    private fun observeNSFWMode() = viewModelScope.launch {
        settingsRepository.enableNSFWMode.collect { nsfwModeEnabled ->
            val categories = Category.entries.filter { nsfwModeEnabled || !it.isNSFW }

            val currentlySelectedCategory = _uiState.value.selectedCategory
            val selectedCategory = when {
                currentlySelectedCategory in categories -> currentlySelectedCategory
                else -> Category.All
            }

            _uiState.update {
                it.copy(
                    categories = categories,
                    selectedCategory = selectedCategory,
                )
            }
        }
    }

    /** Observes search history changes and automatically updates the UI. */
    private fun observeSearchHistory() = viewModelScope.launch {
        combine(
            settingsRepository.showSearchHistory,
            searchHistoryRepository.getAll(),
        ) { showSearchHistory, histories ->
            if (showSearchHistory) histories else emptyList()
        }.collect { histories ->
            val histories = histories.map {
                SearchHistoryItemUiState(id = it.id, query = it.query)
            }
            _uiState.update { it.copy(histories = histories) }
        }
    }
}