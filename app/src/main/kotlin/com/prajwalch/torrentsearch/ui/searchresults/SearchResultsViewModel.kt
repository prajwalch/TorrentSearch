package com.prajwalch.torrentsearch.ui.searchresults

import android.util.Log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.database.entities.SearchHistory
import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.data.repository.SearchProvidersRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.data.repository.TorrentsRepository
import com.prajwalch.torrentsearch.extensions.customSort
import com.prajwalch.torrentsearch.extensions.filterNSFW
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.MaxNumResults
import com.prajwalch.torrentsearch.models.SortCriteria
import com.prajwalch.torrentsearch.models.SortOrder
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.ConnectivityChecker
import com.prajwalch.torrentsearch.providers.SearchProvider
import com.prajwalch.torrentsearch.providers.SearchProviderId

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import javax.inject.Inject

data class SearchResultsUiState(
    val searchQuery: String = "",
    val searchResults: List<Torrent> = emptyList(),
    val filterQuery: String = "",
    val filteredSearchResults: List<Torrent>? = null,
    val currentSortCriteria: SortCriteria = SortCriteria.Default,
    val currentSortOrder: SortOrder = SortOrder.Default,
    val filterOptions: FilterOptionsUiState = FilterOptionsUiState(),
    val resultsNotFound: Boolean = false,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val isInternetError: Boolean = false,
)

data class FilterOptionsUiState(
    val searchProviders: List<SearchProviderFilterUiState> = emptyList(),
    val deadTorrents: Boolean = true,
)

data class SearchProviderFilterUiState(
    val searchProviderId: SearchProviderId,
    val searchProviderName: String,
    val enabled: Boolean = false,
    val selected: Boolean = false,
)

private data class Settings(
    val enableNSFWMode: Boolean,
    val defaultSortOptions: DefaultSortOptions,
    val maxNumResults: MaxNumResults,
    val saveSearchHistory: Boolean,
) {
    data class DefaultSortOptions(
        val sortCriteria: SortCriteria = SortCriteria.Default,
        val sortOrder: SortOrder = SortOrder.Default,
    )
}

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

    private val searchQuery = savedStateHandle.get<String>("query")
    private val searchCategory = savedStateHandle.get<String>("category")?.let(Category::valueOf)

    /**
     * On-going search.
     *
     * Before initiating a new search, on-going search must be canceled
     * even if it's not completed.
     */
    private var searchJob: Job? = null

    init {
        Log.i(TAG, "init is invoked")
        Log.d(TAG, "Query = $searchQuery, Category = $searchCategory")

        performSearch()
        observeNSFWMode()
    }

    fun updateFilterQuery(query: String) {
        _uiState.update { it.copy(filterQuery = query) }
        filterSearchResultsByQuery(searchResults = _uiState.value.searchResults)
    }

    fun sortSearchResults(criteria: SortCriteria, order: SortOrder) {
        viewModelScope.launch {
            val sortedResults = withContext(Dispatchers.Default) {
                _uiState.value.searchResults.customSort(
                    criteria = criteria,
                    order = order,
                )
            }

            _uiState.update {
                it.copy(
                    searchResults = sortedResults,
                    currentSortCriteria = criteria,
                    currentSortOrder = order,
                )
            }
        }
    }

    fun toggleSearchProviderResults(searchProviderId: SearchProviderId) {
        val searchProvidersFilterUiState = _uiState.value.filterOptions.searchProviders.map {
            if (it.searchProviderId == searchProviderId) {
                it.copy(selected = !it.selected)
            } else {
                it
            }
        }

        _uiState.update {
            it.copy(
                filterOptions = it.filterOptions.copy(
                    searchProviders = searchProvidersFilterUiState,
                )
            )
        }
        filterSearchResultsByOptions()
    }

    fun toggleDeadTorrents() {
        _uiState.update {
            it.copy(
                filterOptions = it.filterOptions.copy(
                    deadTorrents = !it.filterOptions.deadTorrents,
                )
            )
        }
        filterSearchResultsByOptions()
    }

    private fun filterSearchResultsByQuery(searchResults: List<Torrent>) {
        viewModelScope.launch {
            val filterQuery = _uiState.value.filterQuery

            if (filterQuery.isBlank()) {
                _uiState.update { it.copy(filteredSearchResults = null) }
                return@launch
            }

            val filteredSearchResults = withContext(Dispatchers.Default) {
                searchResults.filter { it.name.contains(filterQuery, ignoreCase = true) }
            }

            _uiState.update {
                it.copy(
                    searchResults = searchResults,
                    filteredSearchResults = filteredSearchResults,
                )
            }
        }
    }

    private fun filterSearchResultsByOptions(showNSFWResults: Boolean? = null) {
        viewModelScope.launch {
            val filterOptions = _uiState.value.filterOptions

            val selectedSearchProvidersId = filterOptions.searchProviders.mapNotNull {
                if (it.selected) it.searchProviderId else null
            }
            val showNSFWResults = showNSFWResults ?: settingsRepository.enableNSFWMode.first()

            val filteredSearchResults = withContext(Dispatchers.Default) {
                torrentsRepository
                    .getSearchResultsCache()
                    .filter { it.providerId in selectedSearchProvidersId }
                    .filter { filterOptions.deadTorrents || (it.seeders != 0u && it.peers != 0u) }
                    // Global filter option.
                    .filterNSFW(isNSFWModeEnabled = showNSFWResults)
                    .customSort(
                        criteria = _uiState.value.currentSortCriteria,
                        order = _uiState.value.currentSortOrder,
                    )
            }

            if (_uiState.value.filterQuery.isBlank()) {
                _uiState.update { it.copy(searchResults = filteredSearchResults) }
            } else {
                filterSearchResultsByQuery(searchResults = filteredSearchResults)
            }
        }
    }

    fun performSearch() {
        Log.i(TAG, "performSearch() called")

        val searchQuery = searchQuery ?: return
        val searchCategory = searchCategory ?: return

        if (searchQuery.isBlank()) {
            return
        }

        Log.i(TAG, "Cancelling on-going search")
        searchJob?.cancel()

        Log.i(TAG, "Creating new job")
        searchJob = viewModelScope.launch {
            val settings = getLatestSettings()
            Log.d(TAG, "Settings loaded. $settings")

            val defaultSortOptions = settings.defaultSortOptions
            _uiState.update {
                it.copy(
                    searchQuery = searchQuery,
                    searchResults = emptyList(),
                    filterOptions = FilterOptionsUiState(),
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
                val query = searchQuery.trim()

                searchHistoryRepository.add(
                    searchHistory = SearchHistory(query = query),
                )
            }

            val isInternetAvailable = withContext(Dispatchers.IO) {
                connectivityChecker.isInternetAvailable()
            }
            if (!isInternetAvailable) {
                Log.w(TAG, "Internet is not available. Returning...")

                _uiState.update {
                    it.copy(
                        searchResults = emptyList(),
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
                        searchResults = emptyList(),
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
                    query = searchQuery,
                    category = searchCategory,
                    searchProviders = enabledSearchProviders,
                )
                .takeWhile { shouldContinueSearch(maxNumResults = settings.maxNumResults) }
                .mapNotNull { repoResult -> repoResult.torrents }
                .flowOn(Dispatchers.IO)
                .map { filterSearchResultsBySettings(searchResults = it, settings = settings) }
                .flowOn(Dispatchers.Default)
                .onEach { onEachSearchResults(searchResults = it) }
                .onCompletion { onSearchCompletion(cause = it) }
                .launchIn(scope = this)
        }
    }

    private suspend fun getLatestSettings(): Settings {
        val defaultSortOptionsFlow = combine(
            settingsRepository.defaultSortCriteria,
            settingsRepository.defaultSortOrder,
            Settings::DefaultSortOptions,
        )

        return combine(
            settingsRepository.enableNSFWMode,
            defaultSortOptionsFlow,
            settingsRepository.maxNumResults,
            settingsRepository.saveSearchHistory,
            ::Settings,
        ).first()
    }

    private suspend fun getEnabledSearchProviders(): List<SearchProvider> {
        return combine(
            searchProvidersRepository.getInstances(),
            settingsRepository.enabledSearchProvidersId,
        ) { searchProviders, enabledSearchProvidersId ->
            searchProviders.filter { it.info.id in enabledSearchProvidersId }
        }.first()
    }

    private fun shouldContinueSearch(maxNumResults: MaxNumResults): Boolean {
        return maxNumResults.isUnlimited() || _uiState.value.searchResults.size < maxNumResults.n
    }

    private fun filterSearchResultsBySettings(
        searchResults: List<Torrent>,
        settings: Settings,
    ): List<Torrent> {
        val nsfwFilteredResults = searchResults.filterNSFW(
            isNSFWModeEnabled = settings.enableNSFWMode,
        )

        return if (settings.maxNumResults.isUnlimited()) {
            nsfwFilteredResults
        } else {
            val numResultsLeft = settings.maxNumResults.n - _uiState.value.searchResults.size
            nsfwFilteredResults.take(numResultsLeft)
        }
    }

    private suspend fun onEachSearchResults(searchResults: List<Torrent>) {
        Log.i(TAG, "onEachSearchResults() called")

        if (searchResults.isEmpty()) {
            Log.i(TAG, "Received empty or filtered out results. Returning")
            return
        }

        Log.i(TAG, "Received ${searchResults.size} results")
        Log.i(TAG, "Sorting search results")

        val sortedSearchResults = withContext(Dispatchers.Default) {
            with(_uiState.value) {
                this.searchResults
                    .plus(searchResults)
                    .customSort(
                        criteria = this.currentSortCriteria,
                        order = this.currentSortOrder,
                    )
            }
        }

        val searchProviderFilterUiState = with(searchResults.first()) {
            SearchProviderFilterUiState(
                searchProviderId = this.providerId,
                searchProviderName = this.providerName,
                enabled = true,
                selected = true,
            )
        }

        _uiState.update {
            it.copy(
                searchResults = sortedSearchResults,
                filterOptions = it.filterOptions.copy(
                    searchProviders = it.filterOptions.searchProviders.plus(
                        searchProviderFilterUiState,
                    )
                ),
                resultsNotFound = false,
                isLoading = false,
                isSearching = true,
                isInternetError = false,
            )
        }
    }

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
                resultsNotFound = it.searchResults.isEmpty(),
                isLoading = false,
                isSearching = false,
            )
        }
    }

    private fun observeNSFWMode() = viewModelScope.launch {
        settingsRepository.enableNSFWMode.distinctUntilChanged().collect {
            filterSearchResultsByOptions(showNSFWResults = it)
        }
    }

    private companion object {
        private const val TAG = "SearchResultsViewModel"
    }
}