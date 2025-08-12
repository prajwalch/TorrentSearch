package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.MaxNumResults
import com.prajwalch.torrentsearch.data.SearchHistoryRepository
import com.prajwalch.torrentsearch.data.SettingsRepository
import com.prajwalch.torrentsearch.data.SortCriteria
import com.prajwalch.torrentsearch.data.SortOrder
import com.prajwalch.torrentsearch.data.TorrentsRepository
import com.prajwalch.torrentsearch.data.TorrentsRepositoryResult
import com.prajwalch.torrentsearch.database.entities.SearchHistory
import com.prajwalch.torrentsearch.extensions.customSort
import com.prajwalch.torrentsearch.extensions.filterNSFW
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviders

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
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
    val currentSortCriteria: SortCriteria = SortCriteria.Default,
    val currentSortOrder: SortOrder = SortOrder.Default,
    val resultsNotFound: Boolean = false,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val isInternetError: Boolean = false,
)

/** Convenient wrapper around the search history entity. */
data class SearchHistoryUiState(val id: SearchHistoryId, val query: String) {
    /** Converts UI state into entity. */
    fun toEntity() = SearchHistory(id = id, query = query)

    companion object {
        /** Creates a new instance from the search history entity. */
        fun fromEntity(entity: SearchHistory) =
            SearchHistoryUiState(id = entity.id, query = entity.query)
    }
}

/** Relevant settings for the Search screen. */
private data class SearchSettings(
    val enableNSFWMode: Boolean = false,
    val enabledSearchProvidersId: Set<SearchProviderId> = emptySet(),
    val hideResultsWithZeroSeeders: Boolean = false,
    val maxNumResults: MaxNumResults = MaxNumResults.Unlimited,
    val saveSearchHistory: Boolean = true,
)

/** Drives the search logic. */
class SearchViewModel(
    private val settingsRepository: SettingsRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
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
     * Current search settings.
     *
     * [observeSettings] observes it and automatically updates the UI.
     */
    private val settings = combine(
        settingsRepository.enableNSFWMode,
        settingsRepository.enabledSearchProvidersId,
        settingsRepository.hideResultsWithZeroSeeders,
        settingsRepository.maxNumResults,
        settingsRepository.saveSearchHistory,
        ::SearchSettings
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SearchSettings(),
    )

    /** Saved search history. */
    private val searchHistory = combine(
        settingsRepository.showSearchHistory,
        searchHistoryRepository.getAll(),
    ) { showSearchHistory, searchHistory ->
        if (showSearchHistory) searchHistory else emptyList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    /**
     * Current on-going search.
     *
     * Before performing a new search, on-going search must be canceled
     * even if it is not completed.
     */
    private var searchJob: Job? = null

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
    private var searchResults: List<Torrent> = emptyList()

    init {
        loadDefaultCategory()
        observeSettings()
        observeSearchHistory()
    }

    /** Loads the default category and updates the UI state. */
    private fun loadDefaultCategory() = viewModelScope.launch {
        settingsRepository.defaultCategory.firstOrNull()?.let { category ->
            val selectedCategory = if (category in _uiState.value.categories) {
                category
            } else {
                Category.All
            }
            _uiState.update { it.copy(selectedCategory = selectedCategory) }
        }
    }

    /** Observes the settings and automatically updates the UI state. */
    private fun observeSettings() = viewModelScope.launch {
        settings.collect { searchSettings ->
            updateUiStateOnSettingsChange(settings = searchSettings)
        }
    }

    /** Observes search history changes and automatically updates the UI. */
    private fun observeSearchHistory() = viewModelScope.launch {
        searchHistory.collect { searchHistory ->
            _uiState.update { it.copy(histories = searchHistory.toUiStates()) }
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
                searchHistoryRepository.remove(
                    searchHistory = searchHistoryUiState.toEntity()
                )
            }
        }
    }

    /** Sorts and updates the UI state according to given criteria and order. */
    fun sortResults(criteria: SortCriteria, order: SortOrder) {
        val sortedResults = _uiState.value.results.customSort(
            criteria = criteria,
            order = order,
        )
        _uiState.update {
            it.copy(
                results = sortedResults,
                currentSortCriteria = criteria,
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

        // Prepare for new search.
        _uiState.update {
            it.copy(
                results = emptyList(),
                resultsNotFound = false,
                isLoading = true,
                isSearching = false,
                isInternetError = false,
            )
        }

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
            if (settings.value.saveSearchHistory) {
                searchHistoryRepository.add(
                    searchHistory = SearchHistory(query = _uiState.value.query.trim())
                )
            }

            // Acquire the enabled providers.
            val searchProviders = SearchProviders.findByIds(
                ids = settings.value.enabledSearchProvidersId,
            )

            torrentsRepository
                .search(
                    query = _uiState.value.query,
                    category = _uiState.value.selectedCategory,
                    providers = searchProviders,
                )
                .distinctUntilChanged()
                .onEach(::onEachSearchResult)
                .onCompletion { failureCase -> if (failureCase == null) onSearchCompletion() }
                .launchIn(scope = this)
        }
    }

    /** Runs on every search result emitted by repository. */
    private suspend fun onEachSearchResult(result: TorrentsRepositoryResult) {
        // Return as soon as we get the bad internet connection status.
        if (result.isNetworkError) {
            _uiState.update {
                it.copy(
                    resultsNotFound = false,
                    isLoading = false,
                    isSearching = false,
                    isInternetError = true,
                )
            }
            return
        }
        // Save the original results.
        searchResults += result.torrents.orEmpty()
        // Is this the right way?
        //
        // combine() only supports up to 5 flows, so we can't collect
        // and save these two like others.
        val defaultSortCriteria = settingsRepository
            .defaultSortCriteria
            .firstOrNull()
            ?: SortCriteria.Default
        val defaultSortOrder = settingsRepository
            .defaultSortOrder
            .firstOrNull()
            ?: SortOrder.Default

        // Filter (based on settings) and sort them.
        val results = filterSearchResults(
            results = searchResults,
            settings = settings.value
        ).customSort(
            criteria = defaultSortCriteria,
            order = defaultSortOrder,
        )

        // And update the UI.
        _uiState.update {
            it.copy(
                results = results,
                currentSortCriteria = defaultSortCriteria,
                currentSortOrder = defaultSortOrder,
                resultsNotFound = false,
                isLoading = false,
                isSearching = true,
                isInternetError = false,
            )
        }
    }

    /** Runs after the search completes. */
    private fun onSearchCompletion() {
        _uiState.update {
            it.copy(
                resultsNotFound = !it.isInternetError && it.results.isEmpty(),
                isLoading = false,
                isSearching = false,
            )
        }
    }

    /** Updates the UI state based on the given settings. */
    private fun updateUiStateOnSettingsChange(settings: SearchSettings) {
        // Filter selectable categories.
        val categories = Category.entries.filter { settings.enableNSFWMode || !it.isNSFW }
        // Change the currently selected category to 'All' only if needed.
        val currentlySelectedCategory = _uiState.value.selectedCategory
        val selectedCategory = if (currentlySelectedCategory in categories) {
            currentlySelectedCategory
        } else {
            Category.All
        }
        // Filter and sort current results.
        val results = filterSearchResults(
            results = searchResults,
            settings = settings,
        ).customSort(
            criteria = _uiState.value.currentSortCriteria,
            order = _uiState.value.currentSortOrder,
        )

        _uiState.update { uIState ->
            uIState.copy(
                categories = categories,
                selectedCategory = selectedCategory,
                results = results,
                resultsNotFound = searchResults.isNotEmpty() && results.isEmpty(),
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
        val filteredResults = results
            .filterNSFW(isNSFWModeEnabled = settings.enableNSFWMode)
            .filter { it.providerId in settings.enabledSearchProvidersId }
            .filter { !settings.hideResultsWithZeroSeeders || it.seeders != 0u }

        return when {
            settings.maxNumResults.isUnlimited() -> filteredResults
            else -> filteredResults.take(settings.maxNumResults.n)
        }
    }

    companion object {
        /** Provides a factory function for [SearchViewModel]. */
        fun provideFactory(
            settingsRepository: SettingsRepository,
            searchHistoryRepository: SearchHistoryRepository,
            torrentsRepository: TorrentsRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchViewModel(
                    settingsRepository = settingsRepository,
                    searchHistoryRepository = searchHistoryRepository,
                    torrentsRepository = torrentsRepository,
                ) as T
            }
        }
    }
}

/** Converts list of search history entity to list of UI states. */
private fun List<SearchHistory>.toUiStates() = map { SearchHistoryUiState.fromEntity(it) }