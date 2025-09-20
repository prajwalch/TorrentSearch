package com.prajwalch.torrentsearch.ui.searchresults

import android.util.Log

import androidx.lifecycle.SavedStateHandle
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
import com.prajwalch.torrentsearch.network.ConnectivityChecker
import com.prajwalch.torrentsearch.providers.SearchProvider
import com.prajwalch.torrentsearch.ui.settings.DefaultSortOptions

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import javax.inject.Inject

data class SearchResultsUiState(
    val results: List<Torrent> = emptyList(),
    val currentSortCriteria: SortCriteria = SortCriteria.Default,
    val currentSortOrder: SortOrder = SortOrder.Default,
    val resultsNotFound: Boolean = false,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val isInternetError: Boolean = false,
)

/** Relevant settings for the search results screen. */
private data class Settings(
    val enableNSFWMode: Boolean,
    val defaultSortOptions: DefaultSortOptions,
    val hideResultsWithZeroSeeders: Boolean,
    val maxNumResults: MaxNumResults,
    val saveSearchHistory: Boolean,
)

@HiltViewModel
class SearchResultsViewModel @Inject constructor(
    private val torrentsRepository: TorrentsRepository,
    private val searchProvidersRepository: SearchProvidersRepository,
    private val settingsRepository: SettingsRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val connectivityChecker: ConnectivityChecker,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchResultsUiState())
    val uiState = _uiState.asStateFlow()

    private val query = savedStateHandle.get<String>("query")
    private val category = savedStateHandle.get<String>("category")?.let(Category::valueOf)

    /**
     * On-going search.
     *
     * Before initiating a new search, on-going search must be canceled
     * even if it's not completed.
     */
    private var searchJob: Job? = null

    /** Unfiltered search results. */
    private var searchResults = emptyList<Torrent>()

    init {
        Log.i(TAG, "init is invoked")
        Log.d(TAG, "Query = $query, Category = $category")

        performSearch()
        observeNSFWMode()
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

    fun performSearch() {
        Log.i(TAG, "performSearch() called")

        val query = query ?: return
        val category = category ?: return

        if (query.isBlank()) {
            return
        }

        Log.i(TAG, "Cancelling on-going search")
        searchJob?.cancel()
        searchResults = emptyList()

        Log.i(TAG, "Creating new job")
        searchJob = viewModelScope.launch {
            val settings = getLatestSettings()
            Log.d(TAG, "Settings loaded. $settings")

            val defaultSortOptions = settings.defaultSortOptions
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
                val query = query.trim()

                searchHistoryRepository.add(
                    searchHistory = SearchHistory(query = query)
                )
            }

            val isInternetAvailable = withContext(Dispatchers.IO) {
                connectivityChecker.isInternetAvailable()
            }
            if (!isInternetAvailable) {
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

            val enabledSearchProviders = getEnabledSearchProviders()

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
                    query = query,
                    category = category,
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

    /** Fetches and returns the latest settings. */
    private suspend fun getLatestSettings(): Settings {
        val defaultSortOptionsFlow = combine(
            settingsRepository.defaultSortCriteria,
            settingsRepository.defaultSortOrder,
            ::DefaultSortOptions,
        )

        return combine(
            settingsRepository.enableNSFWMode,
            defaultSortOptionsFlow,
            settingsRepository.hideResultsWithZeroSeeders,
            settingsRepository.maxNumResults,
            settingsRepository.saveSearchHistory,
            ::Settings,
        ).first()
    }

    /** Fetches and returns enabled search providers. */
    private suspend fun getEnabledSearchProviders(): List<SearchProvider> {
        return combine(
            searchProvidersRepository.getInstances(),
            settingsRepository.enabledSearchProvidersId,
        ) { searchProviders, enabledSearchProvidersId ->
            searchProviders.filter { it.info.id in enabledSearchProvidersId }
        }.first()
    }

    /** Returns `true` if the search should be continue. */
    private fun shouldContinueSearch(settings: Settings): Boolean {
        Log.i(TAG, "shouldContinueSearch() called")

        val maxNumResults = settings.maxNumResults
        return maxNumResults.isUnlimited() || _uiState.value.results.size < maxNumResults.n
    }

    /** Runs on every search result emitted by repository. */
    private fun onEachSearchResults(results: List<Torrent>, settings: Settings) {
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

    /** Filters the given search results based on the given settings. */
    private fun filterSearchResults(
        results: List<Torrent>,
        settings: Settings,
    ): List<Torrent> {
        val filteredResults = results
            .filterNSFW(isNSFWModeEnabled = settings.enableNSFWMode)
            .filter { !settings.hideResultsWithZeroSeeders || it.seeders != 0u }

        if (settings.maxNumResults.isUnlimited()) {
            return filteredResults
        }

        val numResultsLeft = settings.maxNumResults.n - _uiState.value.results.size
        return results.take(numResultsLeft)
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

    private fun observeNSFWMode() = viewModelScope.launch {
        settingsRepository.enableNSFWMode.distinctUntilChanged().collect { nsfwModeEnabled ->
            if (searchResults.isEmpty()) {
                return@collect
            }

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

    private companion object {
        private const val TAG = "SearchResultsViewModel"
    }
}