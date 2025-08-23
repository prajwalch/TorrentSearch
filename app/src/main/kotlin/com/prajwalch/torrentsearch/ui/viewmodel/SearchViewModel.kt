package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.database.entities.SearchHistory
import com.prajwalch.torrentsearch.data.repository.MaxNumResults
import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.data.repository.SearchProvidersRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.data.repository.SortCriteria
import com.prajwalch.torrentsearch.data.repository.SortOrder
import com.prajwalch.torrentsearch.data.repository.TorrentsRepository
import com.prajwalch.torrentsearch.data.repository.TorrentsRepositoryResult
import com.prajwalch.torrentsearch.extensions.customSort
import com.prajwalch.torrentsearch.extensions.filterNSFW
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.providers.SearchProviderId

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
) {
    /** Returns `true` if the state can be reset to default. */
    fun isResettable() =
        results.isNotEmpty() || resultsNotFound || isLoading || isSearching || isInternetError
}

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
private data class Settings(
    /** General settings. */
    val enableNSFWMode: Boolean = false,
    /** Search settings. */
    val search: SearchSettings = SearchSettings(),
    /** Search history settings. */
    val saveSearchHistory: Boolean = true,
)

/** Search related settings. */
private data class SearchSettings(
    val enabledSearchProvidersId: Set<SearchProviderId> = emptySet(),
    val defaultCategory: Category = Category.All,
    val defaultSortOptions: DefaultSortOptions = DefaultSortOptions(),
    val hideResultsWithZeroSeeders: Boolean = false,
    val maxNumResults: MaxNumResults = MaxNumResults.Unlimited,
)

/** ViewModel which handles the business logic of Search screen. */
class SearchViewModel(
    private val settingsRepository: SettingsRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val torrentsRepository: TorrentsRepository,
    private val searchProvidersRepository: SearchProvidersRepository,
) : ViewModel() {
    /** UI state. */
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    /** All the settings that this ViewModel cares. */
    private val settings = combine(
        settingsRepository.enableNSFWMode,
        settingsRepository.searchSettings(),
        settingsRepository.saveSearchHistory,
        ::Settings,
    )
        .onEach(::onSettingsChange)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Settings(),
        )

    private var isDefaultCategoryLoaded = false

    private val searchProviders = searchProvidersRepository
        .getInstances()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    /**
     * On-going search.
     *
     * Before initiating a new search, on-going search must be canceled
     * even if it's not completed.
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
        observeSearchHistory()
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

    /** Resets the state to default value. */
    fun resetToDefault() {
        searchJob?.cancel()
        searchResults = emptyList()

        val (defaultSortCriteria, defaultSortOrder) = settings.value.search.defaultSortOptions

        _uiState.update {
            it.copy(
                query = "",
                selectedCategory = settings.value.search.defaultCategory,
                results = emptyList(),
                currentSortCriteria = defaultSortCriteria,
                currentSortOrder = defaultSortOrder,
                resultsNotFound = false,
                isLoading = false,
                isSearching = false,
                isInternetError = false,
            )
        }
    }

    /** Performs a search. */
    fun performSearch() {
        // Prevent unnecessary processing.
        if (_uiState.value.query.isEmpty()) {
            return
        }

        val defaultSortOptions = settings.value.search.defaultSortOptions

        // Prepare for the new search.
        _uiState.update {
            it.copy(
                results = emptyList(),
                currentSortCriteria = defaultSortOptions.sortCriteria,
                currentSortOrder = defaultSortOptions.sortOrder,
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
            // We can't trim from the `changeQuery` function simply because
            // doing so won't allow the user to insert a whitespace at all.
            if (settings.value.saveSearchHistory) {
                searchHistoryRepository.add(
                    searchHistory = SearchHistory(query = _uiState.value.query.trim())
                )
            }

            // Acquire the enabled providers.
            val enabledSearchProviders = searchProviders
                .first { it.isNotEmpty() }
                .filter { it.info.id in settings.value.search.enabledSearchProvidersId }

            torrentsRepository
                .search(
                    query = _uiState.value.query,
                    category = _uiState.value.selectedCategory,
                    providers = enabledSearchProviders,
                )
                .distinctUntilChanged()
                .onEach(::onEachSearchResult)
                .onCompletion { failureCase -> if (failureCase == null) onSearchCompletion() }
                .launchIn(scope = this)
        }
    }

    /** Runs on every search result emitted by repository. */
    private fun onEachSearchResult(result: TorrentsRepositoryResult) {
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
        searchResults += result.torrents ?: return

        // Return if the search results are empty.
        if (searchResults.isEmpty()) return

        // Filter (based on settings) and sort them.
        val results = filterSearchResults(
            results = searchResults,
            settings = settings.value
        ).customSort(
            criteria = _uiState.value.currentSortCriteria,
            order = _uiState.value.currentSortOrder,
        )

        // And update the UI.
        _uiState.update {
            it.copy(
                results = results,
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

    /** Runs when settings changes. */
    private fun onSettingsChange(settings: Settings) {
        val categories = Category
            .entries
            .filter { settings.enableNSFWMode || !it.isNSFW }

        val currentlySelectedCategory = _uiState.value.selectedCategory
        val selectedCategory = when {
            !isDefaultCategoryLoaded -> {
                isDefaultCategoryLoaded = true
                settings.search.defaultCategory
            }

            currentlySelectedCategory in categories -> currentlySelectedCategory
            settings.search.defaultCategory in categories -> settings.search.defaultCategory
            else -> Category.All
        }

        _uiState.update {
            it.copy(
                categories = categories,
                selectedCategory = selectedCategory,
            )
        }

        if (searchResults.isEmpty()) return

        // Filter and sort current results.
        val results = filterSearchResults(
            results = searchResults,
            settings = settings,
        ).customSort(
            criteria = _uiState.value.currentSortCriteria,
            order = _uiState.value.currentSortOrder,
        )

        _uiState.update {
            it.copy(
                results = results,
                resultsNotFound = searchResults.isNotEmpty() && results.isEmpty(),
            )
        }
    }

    /** Observes search history changes and automatically updates the UI. */
    private fun observeSearchHistory() {
        combine(
            settingsRepository.showSearchHistory,
            searchHistoryRepository.getAll(),
        ) { showSearchHistory, searchHistory ->
            if (showSearchHistory) searchHistory else emptyList()
        }.onEach { histories ->
            _uiState.update { it.copy(histories = histories.toUiStates()) }
        }.launchIn(scope = viewModelScope)
    }

    /**
     * Filters and returns the search results based on the current search
     * settings.
     */
    private fun filterSearchResults(
        results: List<Torrent>,
        settings: Settings,
    ): List<Torrent> {
        val filteredResults = results
            .filterNSFW(isNSFWModeEnabled = settings.enableNSFWMode)
            .filter { it.providerId in settings.search.enabledSearchProvidersId }
            .filter { !settings.search.hideResultsWithZeroSeeders || it.seeders != 0u }

        return when {
            settings.search.maxNumResults.isUnlimited() -> filteredResults
            else -> filteredResults.take(settings.search.maxNumResults.n)
        }
    }

    companion object {
        /** Provides a factory function for [SearchViewModel]. */
        fun provideFactory(
            settingsRepository: SettingsRepository,
            searchHistoryRepository: SearchHistoryRepository,
            torrentsRepository: TorrentsRepository,
            searchProvidersRepository: SearchProvidersRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchViewModel(
                    settingsRepository = settingsRepository,
                    searchHistoryRepository = searchHistoryRepository,
                    torrentsRepository = torrentsRepository,
                    searchProvidersRepository = searchProvidersRepository,
                ) as T
            }
        }
    }
}

/** Converts list of search history entity to list of UI states. */
private fun List<SearchHistory>.toUiStates() = map { SearchHistoryUiState.fromEntity(it) }

/** Returns a flow for search related settings. */
private fun SettingsRepository.searchSettings(): Flow<SearchSettings> {
    val defaultSortOptionsFlow = combine(
        defaultSortCriteria,
        defaultSortOrder,
        ::DefaultSortOptions,
    )

    return combine(
        enabledSearchProvidersId,
        defaultCategory,
        defaultSortOptionsFlow,
        hideResultsWithZeroSeeders,
        maxNumResults,
        ::SearchSettings
    )
}