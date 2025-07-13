package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.SearchProviderId
import com.prajwalch.torrentsearch.data.SettingsRepository
import com.prajwalch.torrentsearch.data.TorrentsRepository
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.providers.SearchProviders

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchScreenUIState(
    val query: String = "",
    val categories: List<Category> = Category.entries,
    val selectedCategory: Category = Category.All,
    val isLoading: Boolean = false,
    val isInternetError: Boolean = false,
    val resultsNotFound: Boolean = false,
    val results: List<Torrent> = emptyList(),
)

private data class SearchSettings(
    val enableNSFWSearch: Boolean = false,
    val searchProviders: Set<SearchProviderId> = emptySet(),
)

/** Drives the search logic. */
class SearchViewModel(
    private val settingsRepository: SettingsRepository,
    private val torrentsRepository: TorrentsRepository,
) : ViewModel() {
    private val mUiState = MutableStateFlow(SearchScreenUIState())
    val uiState = mUiState.asStateFlow()

    private var currentSearchSettings = SearchSettings()
    private var currentSearchResults = emptyList<Torrent>()

    init {
        observeSettingsChange()
    }

    /** Changes the current query with the given query. */
    fun setQuery(query: String) {
        mUiState.update { it.copy(query = query) }
    }

    /** Changes the current category. */
    fun setCategory(category: Category) {
        mUiState.update { it.copy(selectedCategory = category) }
    }

    /** Performs a search. */
    fun performSearch() = viewModelScope.launch {
        // Prevent unnecessary processing.
        if (mUiState.value.query.isEmpty()) {
            return@launch
        }

        updateUIState { it.copy(isLoading = true) }
        // Clear previous search results.
        currentSearchResults = emptyList()

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

        // Perform the search with enabled providers.
        val searchProviders = SearchProviders.get(currentSearchSettings.searchProviders)
        val result = torrentsRepository.search(
            query = mUiState.value.query,
            category = mUiState.value.selectedCategory,
            providers = searchProviders,
        )

        // Save the results.
        currentSearchResults = result.torrents.orEmpty()

        // Update the UI with current search results only after filtering them.
        updateUIState {
            it.copy(
                isLoading = false,
                isInternetError = result.isNetworkError,
                resultsNotFound = currentSearchResults.isEmpty(),
                results = filterCurrentSearchResults(),
            )
        }
    }

    /** Observes the settings and updates the states upon changes. */
    private fun observeSettingsChange() = viewModelScope.launch {
        combine(
            settingsRepository.enableNSFWSearch,
            settingsRepository.searchProviders,
            ::SearchSettings
        ).collect { searchSettings ->
            // Save the changed settings.
            currentSearchSettings = searchSettings
            updateUiStateOnSettingsChange()
        }
    }

    /** Updates the UI state based on the current settings. */
    private fun updateUiStateOnSettingsChange() {
        val (categories, selectedCategory) = filterCategories()
        val filteredSearchResults = filterCurrentSearchResults()

        updateUIState { uIState ->
            uIState.copy(
                categories = categories,
                selectedCategory = selectedCategory,
                results = filteredSearchResults
            )
        }
    }

    /**
     * Filters and returns the selectable categories as well as the selected
     * category based on the current search settings.
     */
    private fun filterCategories(): Pair<List<Category>, Category> {
        val enableNSFWSearch = currentSearchSettings.enableNSFWSearch
        val currentlySelectedCategory = mUiState.value.selectedCategory

        val categories = Category.entries.filter { enableNSFWSearch || !it.isNSFW }
        val selectedCategory = if (!enableNSFWSearch && currentlySelectedCategory.isNSFW) {
            Category.All
        } else {
            currentlySelectedCategory
        }

        return Pair(categories, selectedCategory)
    }

    /**
     * Filters and returns the current search results based on the current
     * search settings.
     */
    private fun filterCurrentSearchResults(): List<Torrent> {
        return currentSearchResults
            .filter { torrent ->
                currentSearchSettings.searchProviders.contains(torrent.providerId)
            }
            .filter { torrent ->
                // Torrent with no category is also NSFW.
                val categoryIsNullOrNSFW = torrent.category?.isNSFW ?: true
                currentSearchSettings.enableNSFWSearch || !categoryIsNullOrNSFW
            }
    }


    /** Updates the current ui states. */
    private inline fun updateUIState(update: (SearchScreenUIState) -> SearchScreenUIState) {
        mUiState.update(function = update)
    }
}

class SearchViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val torrentsRepository: TorrentsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(
                settingsRepository = settingsRepository,
                torrentsRepository = torrentsRepository,
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}