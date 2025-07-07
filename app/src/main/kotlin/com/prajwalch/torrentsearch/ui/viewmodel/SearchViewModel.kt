package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.Category
import com.prajwalch.torrentsearch.data.TorrentsRepository
import com.prajwalch.torrentsearch.models.Torrent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchScreenUIState(
    val query: String = "",
    val category: Category = Category.All,
    val isLoading: Boolean = false,
    val isInternetError: Boolean = false,
    val resultsNotFound: Boolean = false,
    val results: List<Torrent> = emptyList(),
)

/** Drives the search logic. */
class SearchViewModel(private val torrentsRepository: TorrentsRepository) : ViewModel() {
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
    fun performSearch() = viewModelScope.launch {
        if (mUiState.value.query.isEmpty()) {
            return@launch
        }

        updateUIState { it.copy(isLoading = true) }

        if (!torrentsRepository.isInternetAvailable()) {
            updateUIState {
                it.copy(
                    isLoading = false,
                    isInternetError = true,
                    resultsNotFound = false
                )
            }
            return@launch
        }

        val result = torrentsRepository.search(
            query = mUiState.value.query,
            category = mUiState.value.category,
        )
        updateUIState {
            it.copy(
                isLoading = false,
                isInternetError = result.isNetworkError,
                resultsNotFound = result.torrents?.isEmpty() ?: false,
                results = result.torrents.orEmpty(),
            )
        }
    }

    /** Updates the current ui states. */
    private inline fun updateUIState(update: (SearchScreenUIState) -> SearchScreenUIState) {
        mUiState.update(function = update)
    }
}

class SearchViewModelFactory(private val torrentsRepository: TorrentsRepository) :
    ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(torrentsRepository = torrentsRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}