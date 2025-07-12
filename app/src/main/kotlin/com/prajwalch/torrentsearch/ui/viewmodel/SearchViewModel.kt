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
import kotlinx.coroutines.flow.collectLatest
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

/** Drives the search logic. */
class SearchViewModel(
    private val settingsRepository: SettingsRepository,
    private val torrentsRepository: TorrentsRepository,
) : ViewModel() {
    private val mUiState = MutableStateFlow(SearchScreenUIState())
    val uiState = mUiState.asStateFlow()

    private var enabledSearchProviders: Set<SearchProviderId> = emptySet()

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

        val searchProviders = SearchProviders.get(enabledSearchProviders)
        val result = torrentsRepository.search(
            query = mUiState.value.query,
            category = mUiState.value.selectedCategory,
            providers = searchProviders,
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

    /** Observes the settings and updates the states upon changes. */
    private fun observeSettingsChange() = viewModelScope.launch {
        combine(
            settingsRepository.enableNSFWSearch,
            settingsRepository.searchProviders,
            ::SearchSettings
        ).collectLatest(::updateStateOnSettingsChange)
    }

    /** Updates the state based on the given settings. */
    private fun updateStateOnSettingsChange(searchSettings: SearchSettings) {
        val (enableNSFWSearch, searchProviders) = searchSettings
        val currentUiState = mUiState.value

        // Update the enabled search providers.
        enabledSearchProviders = searchProviders

        val categories = Category.entries.filter { enableNSFWSearch || !it.isNSFW }
        val category = if (!enableNSFWSearch && currentUiState.selectedCategory.isNSFW) {
            Category.All
        } else {
            currentUiState.selectedCategory
        }

        val results = currentUiState.results.filter { torrent ->
            // Torrent with no category is also NSFW.
            val categoryIsNullOrNSFW = torrent.category?.isNSFW ?: true
            enableNSFWSearch || !categoryIsNullOrNSFW
        }

        updateUIState { uIState ->
            uIState.copy(
                categories = categories,
                selectedCategory = category,
                results = results
            )
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