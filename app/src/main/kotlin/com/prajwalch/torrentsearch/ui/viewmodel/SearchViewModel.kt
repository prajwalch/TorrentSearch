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
import com.prajwalch.torrentsearch.utils.prettySizeToBytes

import kotlinx.coroutines.Job
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
    val currentSortKey: SortKey = SortKey.Seeders,
    val currentSortOrder: SortOrder = SortOrder.Descending,
    val results: List<Torrent> = emptyList(),
)

enum class SortKey {
    Name,
    Seeders,
    Peers,
    FileSize {
        override fun toString() = "File Size"
    },
    Date,
}

enum class SortOrder {
    Ascending,
    Descending;

    fun opposite() = when (this) {
        Ascending -> Descending
        Descending -> Ascending
    }
}

private data class SearchSettings(
    val enableNSFWSearch: Boolean = false,
    val hideResultsWithZeroSeeders: Boolean = false,
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
    private var searchJob: Job? = null

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

    /** Sorts and updates the UI state according to given key and order. */
    fun sort(key: SortKey, order: SortOrder) {
        val sortedResults = sortSearchResults(
            results = mUiState.value.results,
            key = key,
            order = order,
        )

        updateUIState {
            it.copy(
                results = sortedResults,
                currentSortKey = key,
                currentSortOrder = order,
            )
        }
    }

    /** Performs a search. */
    fun performSearch() {
        // Cancel any on-going search.
        //
        // This help to prevent from any un-expected behaviour like when user
        // quickly switch to another category when search is still happening.
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
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

            // Save the original results.
            currentSearchResults = result.torrents.orEmpty()

            // Filter them based on settings.
            val filteredSearchResults = filterSearchResults(
                results = currentSearchResults,
                settings = currentSearchSettings
            )
            // Then sort the filtered results.
            val sortedSearchResults = sortSearchResults(
                results = filteredSearchResults,
                key = DEFAULT_SORT_KEY,
                order = DEFAULT_SORT_ORDER,
            )

            // And update the UI.
            updateUIState {
                it.copy(
                    isLoading = false,
                    isInternetError = result.isNetworkError,
                    resultsNotFound = currentSearchResults.isEmpty(),
                    results = sortedSearchResults,
                    currentSortKey = DEFAULT_SORT_KEY,
                    currentSortOrder = DEFAULT_SORT_ORDER,
                )
            }
        }
    }

    /** Observes the settings and updates the states upon changes. */
    private fun observeSettingsChange() = viewModelScope.launch {
        combine(
            settingsRepository.enableNSFWSearch,
            settingsRepository.hideResultsWithZeroSeeders,
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
        val categories = filterCategories(
            categories = Category.entries,
            isNSFWEnabled = currentSearchSettings.enableNSFWSearch,
        )
        val selectedCategory = changeSelectedCategoryIfNeeded(
            currentSelectedCategory = mUiState.value.selectedCategory,
            isNSFWEnabled = currentSearchSettings.enableNSFWSearch,
        )
        val filteredSearchResults = filterSearchResults(
            results = currentSearchResults,
            settings = currentSearchSettings,
        )
        val sortedSearchResults = sortSearchResults(
            results = filteredSearchResults,
            key = mUiState.value.currentSortKey,
            order = mUiState.value.currentSortOrder
        )

        updateUIState { uIState ->
            uIState.copy(
                categories = categories,
                selectedCategory = selectedCategory,
                results = sortedSearchResults,
            )
        }
    }

    /**
     * Filters and returns the selectable categories based on the current
     * search settings.
     */
    private fun filterCategories(
        categories: List<Category>,
        isNSFWEnabled: Boolean,
    ): List<Category> {
        return categories.filter { isNSFWEnabled || !it.isNSFW }
    }

    /** Returns a new selected category if needed otherwise returns the same. */
    private fun changeSelectedCategoryIfNeeded(
        currentSelectedCategory: Category,
        isNSFWEnabled: Boolean,
    ): Category {
        return if (!isNSFWEnabled && currentSelectedCategory.isNSFW) {
            Category.All
        } else {
            currentSelectedCategory
        }
    }

    /**
     * Filters and returns the search results based on the current search
     * settings.
     */
    private fun filterSearchResults(
        results: List<Torrent>,
        settings: SearchSettings,
    ): List<Torrent> {
        return results
            .filter { torrent -> !settings.hideResultsWithZeroSeeders || torrent.seeders != 0u }
            .filter { torrent ->
                settings.searchProviders.contains(torrent.providerId)
            }
            .filter { torrent ->
                // Torrent with no category is also NSFW.
                val categoryIsNullOrNSFW = torrent.category?.isNSFW ?: true
                settings.enableNSFWSearch || !categoryIsNullOrNSFW
            }
    }

    /** Sorts and returns the given results based on the given key and order. */
    private fun sortSearchResults(
        results: List<Torrent>,
        key: SortKey,
        order: SortOrder,
    ): List<Torrent> {
        val sortedResults = when (key) {
            SortKey.Name -> results.sortedBy { it.name }
            SortKey.Seeders -> results.sortedBy { it.seeders }
            SortKey.Peers -> results.sortedBy { it.peers }
            SortKey.FileSize -> results.sortedBy { prettySizeToBytes(it.size) }
            // FIXME: Sorting by date needs some fixes.
            SortKey.Date -> results.sortedBy { it.uploadDate }
        }

        return if (order == SortOrder.Ascending) sortedResults else sortedResults.reversed()
    }

    /** Updates the current ui states. */
    private inline fun updateUIState(update: (SearchScreenUIState) -> SearchScreenUIState) {
        mUiState.update(function = update)
    }

    private companion object {
        private val DEFAULT_SORT_KEY = SortKey.Seeders
        private val DEFAULT_SORT_ORDER = SortOrder.Descending
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