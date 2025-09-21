package com.prajwalch.torrentsearch.ui.settings.searchhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prajwalch.torrentsearch.data.database.entities.SearchHistory

import com.prajwalch.torrentsearch.data.database.entities.SearchHistoryId
import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import javax.inject.Inject

data class SearchHistoryItemUiState(val id: SearchHistoryId, val query: String)

/** ViewModel which handles the business logic of Search history screen. */
@HiltViewModel
class SearchHistoryViewModel @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository,
) : ViewModel() {
    val uiState = searchHistoryRepository
        .getAll()
        .map { histories ->
            histories.map { SearchHistoryItemUiState(id = it.id, query = it.query) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    /** Deletes the search history associated with given id. */
    fun deleteSearchHistory(id: SearchHistoryId) {
        viewModelScope.launch {
            val searchHistoryItemState = uiState.value.find { it.id == id }

            searchHistoryItemState?.let {
                val searchHistory = SearchHistory(id = it.id, query = it.query)
                searchHistoryRepository.remove(searchHistory = searchHistory)
            }
        }
    }

    /** Deletes all search history. */
    fun deleteAllSearchHistory() {
        viewModelScope.launch { searchHistoryRepository.clearAll() }
    }
}