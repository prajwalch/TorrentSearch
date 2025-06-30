package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.Category
import com.prajwalch.torrentsearch.data.Torrent
import com.prajwalch.torrentsearch.data.TorrentsRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchScreenUIState(
    val query: String = "",
    val category: Category = Category.All,
    val results: List<Torrent> = emptyList(),
    val isLoading: Boolean = false,
    val isInternetError: Boolean = false,
)

/** Drives the search logic. */
class SearchViewModel : ViewModel() {
    private val repository = TorrentsRepository()

    private val mUiState = MutableStateFlow(SearchScreenUIState())
    val uiState = mUiState.asStateFlow()

    /** Changes the current query with the given query. */
    fun setQuery(query: String) {
        mUiState.update { it.copy(query = query) }
    }

    /** Changes the current category. */
    fun setCategory(category: Category) {
        mUiState.update { it.copy(category = category) }
    }

    /** Performs a search. */
    fun performSearch() {
        if (mUiState.value.query.isEmpty()) {
            return
        }

        viewModelScope.launch {
            updateUIState { it.copy(isLoading = true) }

            if (!repository.isInternetAvailable()) {
                updateUIState { it.copy(isLoading = false, isInternetError = true) }
                return@launch
            }

            val results = repository.search(mUiState.value.query, mUiState.value.category)
            updateUIState {
                it.copy(results = results, isLoading = false, isInternetError = false)
            }
        }
    }

    /** Updates the current ui states. */
    private inline fun updateUIState(update: (SearchScreenUIState) -> SearchScreenUIState) {
        mUiState.update(function = update)
    }

    /** Closes the internal connection. */
    fun closeConnection() {
        repository.close()
    }
}