package com.prajwalch.torrentsearch.ui.viewmodel

import android.util.Log

import androidx.lifecycle.ViewModel
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
import com.prajwalch.torrentsearch.network.ConnectivityObserver
import com.prajwalch.torrentsearch.providers.SearchProviderId

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import javax.inject.Inject

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
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val torrentsRepository: TorrentsRepository,
    searchProvidersRepository: SearchProvidersRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val settingsRepository: SettingsRepository,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {
    /** UI state. */
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    /** Search providers instance. */
    private val searchProviders = searchProvidersRepository
        .getInstances()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    /** Internet connection status. */
    private val isInternetAvailable = connectivityObserver
        .isInternetAvailable
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    /**
     * On-going search.
     *
     * Before initiating a new search, on-going search must be canceled
     * even if it's not completed.
     */
    private var activeSearchJob: Job? = null

    /**
     * Unfiltered search results.
     *
     * It will contain the results which we originally receive from the
     * repository and the results which are given to UI are filtered ones.
     *
     * By saving the original results, it allows us to hand over the original
     * results back to UI when settings are reverted or altered. For e.x.: when
     * enabling/disabling NSFW mode.
     */
    private var searchResults: List<Torrent> = emptyList()

    /** Settings relevant to this ViewModel. */
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

    /**
     * This can be removed once we have a better way to load default category
     * for the first time.
     */
    private var isDefaultCategoryLoaded = false

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
        activeSearchJob?.cancel()
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
        Log.i(TAG, "performSearch() called")

        if (_uiState.value.query.isEmpty()) {
            return
        }

        Log.i(TAG, "Cancelling on-going search")
        activeSearchJob?.cancel()

        val defaultSortOptions = settings.value.search.defaultSortOptions
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

        // Clear previous search results.
        searchResults = emptyList()

        activeSearchJob = viewModelScope.launch {
            if (settings.value.saveSearchHistory) {
                // Trim the query to prevent same query (e.g. 'one' and 'one ')
                // from end upping into the database.
                //
                // NOTE: Trimming from `changeQuery()` is not possible because
                // doing so won't allow the user to insert a whitespace at all.
                val query = _uiState.value.query.trim()

                searchHistoryRepository.add(
                    searchHistory = SearchHistory(query = query)
                )
            }

            if (!isInternetAvailable.value) {
                Log.w(TAG, "Internet is not available. Returning...")

                _uiState.update {
                    it.copy(
                        results = emptyList(),
                        resultsNotFound = false,
                        isLoading = false,
                        isSearching = false,
                        isInternetError = true,
                    )
                }
                return@launch
            }

            if (settings.value.search.enabledSearchProvidersId.isEmpty()) {
                Log.i(TAG, "All search providers are disabled. Returning...")

                _uiState.update {
                    it.copy(
                        results = emptyList(),
                        resultsNotFound = true,
                        isLoading = false,
                        isSearching = false,
                        isInternetError = false,
                    )
                }
                return@launch
            }

            val enabledSearchProviders = searchProviders
                .first { it.isNotEmpty() }
                .filter { it.info.id in settings.value.search.enabledSearchProvidersId }
            Log.i(TAG, "Num enabled search providers = ${enabledSearchProviders.size}")

            torrentsRepository
                .search(
                    query = _uiState.value.query,
                    category = _uiState.value.selectedCategory,
                    searchProviders = enabledSearchProviders,
                )
                .flowOn(Dispatchers.IO)
                .onEach(::onEachSearchResult)
                .onCompletion { onSearchCompletion(cause = it) }
                .launchIn(scope = this)
        }
    }

    /**
     * Runs on every search result emitted by repository.
     *
     * This function updates the UI only when successful results are received.
     * Any network or other errors are ignored though it's best to report them
     * to the UI but we don't have any implementation for that yet.
     */
    private fun onEachSearchResult(result: TorrentsRepositoryResult) {
        Log.i(TAG, "onEachSearchResult() called")

        if (result.torrents == null) {
            Log.i(TAG, "Didn't received any results. Returning...")
            return
        }

        searchResults += result.torrents
        Log.i(TAG, "Received ${result.torrents.size} results, ${searchResults.size} total")

        if (searchResults.isEmpty()) {
            Log.i(TAG, "Empty results. Returning...")
            return
        }

        Log.i(TAG, "Processing results based on current settings")
        val filteredResults = filterSearchResults(
            results = searchResults,
            settings = settings.value,
        )

        if (filteredResults.isEmpty()) {
            Log.i(TAG, "All results are filtered out. Returning...")
            return
        }

        Log.i(TAG, "Final num of results = ${filteredResults.size}")

        val sortedResults = filteredResults.customSort(
            criteria = _uiState.value.currentSortCriteria,
            order = _uiState.value.currentSortOrder,
        )

        _uiState.update {
            it.copy(
                results = sortedResults,
                resultsNotFound = false,
                isLoading = false,
                isSearching = true,
                isInternetError = false,
            )
        }
    }

    /** Runs after the search completes. */
    private fun onSearchCompletion(cause: Throwable?) {
        Log.i(TAG, "onSearchCompletion() called")

        if (cause is CancellationException) {
            Log.w(TAG, "Search is cancelled. Returning...")
            return
        }

        Log.d(TAG, "cause = $cause")
        Log.i(TAG, "Search completed")

        _uiState.update {
            it.copy(
                resultsNotFound = it.results.isEmpty(),
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

    private companion object {
        private const val TAG = "SearchViewModel"
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