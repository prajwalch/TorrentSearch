package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.MaxNumResults
import com.prajwalch.torrentsearch.data.SearchHistoriesRepository
import com.prajwalch.torrentsearch.data.SearchProviderId
import com.prajwalch.torrentsearch.data.SettingsRepository
import com.prajwalch.torrentsearch.data.TorrentsRepository
import com.prajwalch.torrentsearch.database.entities.SearchHistory
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.providers.SearchProviders
import com.prajwalch.torrentsearch.utils.prettySizeToBytes

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Unique identifier of the search history. */
typealias SearchHistoryId = Long

/** UI state for the Search screen. */
data class SearchUiState(
    // Top bar state.
    val query: String = "",
    val histories: List<SearchHistoryUiState> = emptyList(),
    val categories: List<Category> = Category.entries,
    val selectedCategory: Category = Category.All,
    // Content state.
    val results: List<Torrent> = emptyList(),
    val currentSortKey: SortKey = SortKey.Seeders,
    val currentSortOrder: SortOrder = SortOrder.Descending,
    val resultsNotFound: Boolean = false,
    val isLoading: Boolean = false,
    val isInternetError: Boolean = false,
)

/** Convenient wrapper around the search history entity. */
data class SearchHistoryUiState(
    val id: SearchHistoryId,
    val query: String,
) {
    /** Converts UI state into entity. */
    fun toEntity() = SearchHistory(id = id, query = query)

    companion object {
        /** Creates a new instance from the search history entity. */
        fun fromEntity(entity: SearchHistory) =
            SearchHistoryUiState(id = entity.id, query = entity.query)
    }
}

/** Results sort criteria. */
enum class SortKey {
    Name,
    Seeders,
    Peers,
    FileSize {
        override fun toString() = "File Size"
    },
    Date,
}

/** Results sort order. */
enum class SortOrder {
    Ascending,
    Descending;

    fun opposite() = when (this) {
        Ascending -> Descending
        Descending -> Ascending
    }
}

/** Relevant settings for the Search screen. */
private data class SearchSettings(
    val enableNSFWMode: Boolean = false,
    val hideResultsWithZeroSeeders: Boolean = false,
    val searchProviders: Set<SearchProviderId> = emptySet(),
    val maxNumResults: MaxNumResults = MaxNumResults.Unlimited,
)

/** Drives the search logic. */
class SearchViewModel(
    private val settingsRepository: SettingsRepository,
    private val searchHistoriesRepository: SearchHistoriesRepository,
    private val torrentsRepository: TorrentsRepository,
) : ViewModel() {
    /**
     * Current UI state
     *
     * State change occurs in 3 conditions:
     * - on UI demand
     * - on settings change
     * - on search histories change
     */
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Current on-going search.
     *
     * Before performing a new search, on-going search must be canceled
     * even if it is not completed.
     */
    private var searchJob: Job? = null

    /**
     * Current search settings.
     *
     * Automatically updated through [observeSettings] when settings
     * changes.
     */
    private var settings = SearchSettings()

    /**
     * Unfiltered search results.
     *
     * This is the original results which we get from the repository after a new
     * search is performed. The UI receives results only after we perform a filter
     * (based on settings) and sort operations on this.
     *
     * By saving like this, it allows us to give original results to UI when
     * user reverts to previous settings.
     */
    private var searchResults = emptyList<Torrent>()

    init {
        observeSettings()
        observeSearchHistories()
    }

    /** Observes the settings change and automatically updates the state. */
    private fun observeSettings() = viewModelScope.launch {
        combine(
            settingsRepository.enableNSFWMode,
            settingsRepository.hideResultsWithZeroSeeders,
            settingsRepository.searchProviders,
            settingsRepository.maxNumResults,
            ::SearchSettings
        ).collect { searchSettings ->
            // Save the changed settings.
            settings = searchSettings
            updateUiStateOnSettingsChange()
        }
    }

    /** Observes search history changes and automatically updates the UI. */
    private fun observeSearchHistories() = viewModelScope.launch {
        searchHistoriesRepository
            .all()
            .map { searchHistories -> searchHistories.toUiStates() }
            .collect { searchHistoryUiStates ->
                _uiState.update { it.copy(histories = searchHistoryUiStates) }
            }
    }

    /** Changes the current query with the given query. */
    fun changeQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    /** Changes the current category with the given category. */
    fun changeCategory(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    /** Deletes the search history associated with given id. */
    fun deleteSearchHistory(id: SearchHistoryId) {
        viewModelScope.launch {
            val searchHistory = _uiState.value.histories.find { it.id == id }

            searchHistory?.let { searchHistoryUiState ->
                searchHistoriesRepository.remove(
                    searchHistory = searchHistoryUiState.toEntity()
                )
            }
        }
    }

    /** Sorts and updates the UI state according to given key and order. */
    fun sortResults(key: SortKey, order: SortOrder) {
        val sortedResults = _uiState.value.results.customSort(key = key, order = order)
        _uiState.update {
            it.copy(
                results = sortedResults,
                currentSortKey = key,
                currentSortOrder = order,
            )
        }
    }

    /** Performs a search. */
    fun performSearch() {
        // Prevent unnecessary processing.
        if (_uiState.value.query.isEmpty()) {
            return
        }

        // Shift to loading state.
        _uiState.update { it.copy(isLoading = true) }

        // Cancel any on-going search.
        //
        // This helps to prevent from any un-expected behaviour like when user
        // quickly switch to another category when search is still happening.
        searchJob?.cancel()

        // Clear previous search results.
        searchResults = emptyList()

        // Then initiates a new search.
        searchJob = viewModelScope.launch {
            // Save current query.
            //
            // Trim the query before saving, this helps to prevent from
            // inserting same query (one without the leading/trailing whitespace
            // and the other with whitespace).
            //
            // We can't trim from the `setQuery` function simply because doing so
            // won't allow the user to insert a whitespace at all.
            searchHistoriesRepository.add(
                searchHistory = SearchHistory(query = _uiState.value.query.trim())
            )

            // Return as soon as we get the bad internet connection status.
            if (!torrentsRepository.isInternetAvailable()) {
                _uiState.update {
                    it.copy(
                        resultsNotFound = false,
                        isLoading = false,
                        isInternetError = true,
                    )
                }
                return@launch
            }

            // Acquire the enabled providers.
            val searchProviders = SearchProviders.get(settings.searchProviders)
            // Perform the search using them.
            val torrentsRepositoryResult = torrentsRepository.search(
                query = _uiState.value.query,
                category = _uiState.value.selectedCategory,
                providers = searchProviders,
            )

            // Save the original results.
            searchResults = torrentsRepositoryResult.torrents.orEmpty()

            // Filter (based on settings) and sort them.
            val results = filterSearchResults(
                results = searchResults,
                settings = settings
            ).customSort(
                key = DEFAULT_SORT_KEY,
                order = DEFAULT_SORT_ORDER,
            )

            // And update the UI.
            _uiState.update {
                it.copy(
                    results = results,
                    currentSortKey = DEFAULT_SORT_KEY,
                    currentSortOrder = DEFAULT_SORT_ORDER,
                    resultsNotFound = searchResults.isEmpty(),
                    isLoading = false,
                    isInternetError = torrentsRepositoryResult.isNetworkError,
                )
            }
        }
    }

    /** Updates the UI state based on the current settings. */
    private fun updateUiStateOnSettingsChange() {
        // Filter selectable categories.
        val categories = Category.entries.filter { settings.enableNSFWMode || !it.isNSFW }
        // Change the currently selected category to 'All' only if needed.
        val currentlySelectedCategory = _uiState.value.selectedCategory
        val selectedCategory = if (!settings.enableNSFWMode && currentlySelectedCategory.isNSFW) {
            Category.All
        } else {
            currentlySelectedCategory
        }
        // Filter and sort current results.
        val results = filterSearchResults(
            results = searchResults,
            settings = settings,
        ).customSort(
            key = _uiState.value.currentSortKey,
            order = _uiState.value.currentSortOrder,
        )

        _uiState.update { uIState ->
            uIState.copy(
                categories = categories,
                selectedCategory = selectedCategory,
                results = results,
            )
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
        val results = results
            .filter { torrent -> !settings.hideResultsWithZeroSeeders || torrent.seeders != 0u }
            .filter { torrent -> settings.searchProviders.contains(torrent.providerId) }
            .filter { torrent ->
                // Torrent with no category is also NSFW.
                val categoryIsNullOrNSFW = torrent.category?.isNSFW ?: true
                settings.enableNSFWMode || !categoryIsNullOrNSFW
            }

        return when {
            settings.maxNumResults.isUnlimited() -> results
            else -> results.take(settings.maxNumResults.n)
        }
    }

    companion object {
        private val DEFAULT_SORT_KEY = SortKey.Seeders
        private val DEFAULT_SORT_ORDER = SortOrder.Descending

        /** Provides a factory function for [SearchViewModel]. */
        fun provideFactory(
            settingsRepository: SettingsRepository,
            searchHistoriesRepository: SearchHistoriesRepository,
            torrentsRepository: TorrentsRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchViewModel(
                    settingsRepository = settingsRepository,
                    searchHistoriesRepository = searchHistoriesRepository,
                    torrentsRepository = torrentsRepository,
                ) as T
            }
        }
    }
}

/** Converts list of search history entity to list of UI states. */
private fun List<SearchHistory>.toUiStates() = map { SearchHistoryUiState.fromEntity(it) }

/** Sorts the list based on the given key (criteria) and order. */
private fun List<Torrent>.customSort(key: SortKey, order: SortOrder): List<Torrent> {
    val sortedResults = when (key) {
        SortKey.Name -> this.sortedBy { it.name }
        SortKey.Seeders -> this.sortedBy { it.seeders }
        SortKey.Peers -> this.sortedBy { it.peers }
        SortKey.FileSize -> this.sortedBy { prettySizeToBytes(it.size) }
        // FIXME: Sorting by date needs some fixes.
        SortKey.Date -> this.sortedBy { it.uploadDate }
    }

    return if (order == SortOrder.Ascending) sortedResults else sortedResults.reversed()
}