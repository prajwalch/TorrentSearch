package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.SearchHistoryRepository

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel which handles the business logic of Search history screen. */
class SearchHistoryViewModel(
    private val searchHistoryRepository: SearchHistoryRepository,
) : ViewModel() {
    val uiState = searchHistoryRepository
        .getAll()
        .map { list -> list.map(SearchHistoryUiState::fromEntity) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
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

    companion object {
        /** Provides a factory function for [SearchHistoryViewModel]. */
        fun providerFactory(
            searchHistoryRepository: SearchHistoryRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SearchHistoryViewModel(
                        searchHistoryRepository = searchHistoryRepository
                    ) as T
                }
            }
        }
    }
}