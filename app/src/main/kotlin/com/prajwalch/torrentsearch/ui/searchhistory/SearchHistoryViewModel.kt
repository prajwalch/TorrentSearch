package com.prajwalch.torrentsearch.ui.searchhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.domain.model.SearchHistoryId

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlin.time.Duration.Companion.seconds

/** ViewModel which handles the business logic of Search history screen. */
class SearchHistoryViewModel(
    private val searchHistoryRepository: SearchHistoryRepository,
) : ViewModel() {
    val uiState = searchHistoryRepository
        .getAllSearchHistories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = emptyList(),
        )

    /** Deletes the search history associated with given id. */
    fun deleteSearchHistory(id: SearchHistoryId) {
        viewModelScope.launch {
            searchHistoryRepository.deleteSearchHistoryById(id = id)
        }
    }

    /** Deletes all search history. */
    fun deleteAllSearchHistory() {
        viewModelScope.launch {
            searchHistoryRepository.deleteAllSearchHistories()
        }
    }
}