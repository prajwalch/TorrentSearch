package com.prajwalch.torrentsearch.ui.searchhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.models.SearchHistoryId

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import javax.inject.Inject

/** ViewModel which handles the business logic of Search history screen. */
@HiltViewModel
class SearchHistoryViewModel @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository,
) : ViewModel() {
    val uiState = searchHistoryRepository
        .observeAllSearchHistories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    /** Deletes the search history associated with given id. */
    fun deleteSearchHistory(id: SearchHistoryId) {
        viewModelScope.launch {
            uiState.value
                .find { it.id == id }
                ?.let {
                    searchHistoryRepository.deleteSearchHistory(searchHistory = it)
                }
        }
    }

    /** Deletes all search history. */
    fun deleteAllSearchHistory() {
        viewModelScope.launch { searchHistoryRepository.deleteAllSearchHistories() }
    }
}