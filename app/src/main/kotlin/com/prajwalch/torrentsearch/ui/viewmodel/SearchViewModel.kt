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
import com.prajwalch.torrentsearch.extensions.customSort
import com.prajwalch.torrentsearch.extensions.filterNSFW
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.ConnectivityObserver
import com.prajwalch.torrentsearch.providers.SearchProvider

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
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

    /**
     * On-going search.
     *
     * Before initiating a new search, on-going search must be canceled
     * even if it's not completed.
     */
    private var activeSearchJob: Job? = null

    /** Unfiltered search results. */
    private var searchResults = emptyList<Torrent>()

    /** Internet connection status. */
    private val isInternetAvailable = connectivityObserver
        .isInternetAvailable
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    private val searchProvidersLoader = SearchProvidersLoader(
        coroutineScope = viewModelScope,
        searchProvidersRepository = searchProvidersRepository,
        settingsRepository = settingsRepository,
    )

    private val settingsLoader = SettingsLoader(
        coroutineScope = viewModelScope,
        settingsRepository = settingsRepository,
    )

    init {
        loadDefaultCategory()
        observeNSFWMode()
        observeSearchHistory()
    }

    private fun loadDefaultCategory() = viewModelScope.launch {
        val defaultCategory = settingsLoader.getDefaultCategory()
        _uiState.update { it.copy(selectedCategory = defaultCategory) }
    }

    private fun observeNSFWMode() = viewModelScope.launch {
        settingsLoader.observeNSFWMode { nsfwModeEnabled ->
            val categories = Category.entries.filter { nsfwModeEnabled || !it.isNSFW }

            val currentlySelectedCategory = _uiState.value.selectedCategory
            val selectedCategory = when {
                currentlySelectedCategory in categories -> currentlySelectedCategory
                else -> Category.All
            }

            _uiState.update {
                it.copy(
                    categories = categories,
                    selectedCategory = selectedCategory,
                )
            }

            if (searchResults.isNotEmpty()) {
                val nsfwFilteredResults = searchResults
                    .filterNSFW(isNSFWModeEnabled = nsfwModeEnabled)
                    .customSort(
                        criteria = _uiState.value.currentSortCriteria,
                        order = _uiState.value.currentSortOrder,
                    )

                _uiState.update {
                    it.copy(
                        results = nsfwFilteredResults,
                        resultsNotFound = nsfwFilteredResults.isEmpty(),
                    )
                }
            }
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

        _uiState.update {
            it.copy(
                query = "",
                selectedCategory = Category.All,
                results = emptyList(),
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
        searchResults = emptyList()

        Log.i(TAG, "Creating new job")
        activeSearchJob = viewModelScope.launch {
            val settings = settingsLoader.getLatestSettings()
            Log.d(TAG, "Settings loaded. $settings")

            val defaultSortOptions = settings.search.defaultSortOptions
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

            if (settings.saveSearchHistory) {
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

            if (!isInternetAvailable.first()) {
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

            val enabledSearchProviders = searchProvidersLoader.getEnabledSearchProviders()

            if (enabledSearchProviders.isEmpty()) {
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
            Log.i(TAG, "Num enabled search providers = ${enabledSearchProviders.size}")

            torrentsRepository
                .search(
                    query = _uiState.value.query,
                    category = _uiState.value.selectedCategory,
                    searchProviders = enabledSearchProviders,
                )
                .takeWhile { shouldContinueSearch(settings = settings) }
                .flowOn(Dispatchers.IO)
                // Any network or other errors are ignored though it's best to
                // report them to the UI but we don't have any implementation
                // for that yet.
                .mapNotNull { repoResult -> repoResult.torrents }
                .onEach { onEachSearchResults(results = it, settings = settings) }
                .onCompletion { onSearchCompletion(cause = it) }
                .launchIn(scope = this)
        }
    }

    /** Returns `true` if the search should be continue. */
    private fun shouldContinueSearch(settings: SettingsLoader.Settings): Boolean {
        Log.i(TAG, "shouldContinueSearch() called")

        val maxNumResults = settings.search.maxNumResults
        return maxNumResults.isUnlimited() || _uiState.value.results.size < maxNumResults.n
    }

    /** Filters the given search results based on the given settings. */
    private fun filterSearchResults(
        results: List<Torrent>,
        settings: SettingsLoader.Settings,
    ): List<Torrent> {
        val filteredResults = results
            .filterNSFW(isNSFWModeEnabled = settings.enableNSFWMode)
            .filter { !settings.search.hideResultsWithZeroSeeders || it.seeders != 0u }

        if (settings.search.maxNumResults.isUnlimited()) {
            return filteredResults
        }

        val numResultsLeft = settings.search.maxNumResults.n - _uiState.value.results.size
        return results.take(numResultsLeft)
    }

    /** Runs on every search result emitted by repository. */
    private fun onEachSearchResults(results: List<Torrent>, settings: SettingsLoader.Settings) {
        Log.i(TAG, "onEachSearchResults() called")

        searchResults += results

        val allSearchResults = searchResults
        if (allSearchResults.isEmpty()) {
            Log.i(TAG, "Empty results. Returning...")
            return
        }

        Log.i(TAG, "Received ${results.size} results, ${allSearchResults.size} total")
        Log.i(TAG, "Filtering results..")

        val filteredResults = filterSearchResults(
            results = allSearchResults,
            settings = settings,
        )
        if (filteredResults.isEmpty()) {
            Log.i(TAG, "All results are filtered out. Returning...")
            return
        }

        Log.i(TAG, "Sorting results...")
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

    private companion object {
        private const val TAG = "SearchViewModel"
    }
}

/** Converts list of search history entity to list of UI states. */
private fun List<SearchHistory>.toUiStates() = map { SearchHistoryUiState.fromEntity(it) }

private class SearchProvidersLoader(
    coroutineScope: CoroutineScope,
    searchProvidersRepository: SearchProvidersRepository,
    settingsRepository: SettingsRepository,
) {
    private val enabledSearchProvidersFlow = combine(
        searchProvidersRepository.getInstances(),
        settingsRepository.enabledSearchProvidersId,
    ) { searchProviders, enabledSearchProvidersId ->
        searchProviders.filter { it.info.id in enabledSearchProvidersId }
    }

    private val enabledSearchProviders = enabledSearchProvidersFlow
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    suspend fun getEnabledSearchProviders(): List<SearchProvider> {
        return enabledSearchProviders.first { it != null }.orEmpty()
    }
}

private class SettingsLoader(
    coroutineScope: CoroutineScope,
    settingsRepository: SettingsRepository,
) {
    val settings = combine(
        settingsRepository.enableNSFWMode,
        settingsRepository.searchSettings(),
        settingsRepository.saveSearchHistory,
        ::Settings,
    ).stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    suspend fun getLatestSettings(): Settings = settings.mapNotNull { it }.first()

    suspend fun getDefaultCategory(): Category = getLatestSettings().search.defaultCategory

    suspend fun observeNSFWMode(action: suspend (Boolean) -> Unit) {
        settings.mapNotNull { it?.enableNSFWMode }.collect { action(it) }
    }

    /** Returns a flow for search related settings. */
    private fun SettingsRepository.searchSettings(): Flow<SearchSettings> {
        val defaultSortOptionsFlow = combine(
            defaultSortCriteria,
            defaultSortOrder,
            ::DefaultSortOptions,
        )

        return combine(
            defaultCategory,
            defaultSortOptionsFlow,
            hideResultsWithZeroSeeders,
            maxNumResults,
            ::SearchSettings
        )
    }

    /** Relevant settings for the Search screen. */
    data class Settings(
        /** General settings. */
        val enableNSFWMode: Boolean,
        /** Search settings. */
        val search: SearchSettings,
        /** Search history settings. */
        val saveSearchHistory: Boolean,
    )

    /** Search related settings. */
    data class SearchSettings(
        val defaultCategory: Category,
        val defaultSortOptions: DefaultSortOptions,
        val hideResultsWithZeroSeeders: Boolean,
        val maxNumResults: MaxNumResults,
    )
}