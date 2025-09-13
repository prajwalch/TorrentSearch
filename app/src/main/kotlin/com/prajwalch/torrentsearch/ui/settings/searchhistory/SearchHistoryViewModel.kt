package com.prajwalch.torrentsearch.ui.settings.searchhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.ui.search.SearchHistoryId
import com.prajwalch.torrentsearch.ui.search.SearchHistoryUiState

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import javax.inject.Inject

/** ViewModel which handles the business logic of Search history screen. */
@HiltViewModel
class SearchHistoryViewModel @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository,
) : ViewModel() {
    val uiState = searchHistoryRepository
        .getAll()
        .map { list -> list.map(SearchHistoryUiState.Companion::fromEntity) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    /** Deletes the search history associated with given id. */
    fun deleteSearchHistory(id: SearchHistoryId) {
        viewModelScope.launch {
            val searchHistory = uiState.value.find { it.id == id }

            searchHistory?.let { searchHistoryUiState ->
                searchHistoryRepository.remove(
                    searchHistory = searchHistoryUiState.toEntity()
                )
            }
        }
    }

    /** Deletes all search history. */
    fun deleteAllSearchHistory() {
        viewModelScope.launch { searchHistoryRepository.clearAll() }
    }
}