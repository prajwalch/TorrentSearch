package com.prajwalch.torrentsearch.ui.searchresults

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.data.repository.SearchResult
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.extensions.customSort
import com.prajwalch.torrentsearch.extensions.filterNSFW
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.SortCriteria
import com.prajwalch.torrentsearch.models.SortOrder
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.ConnectivityChecker
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.usecases.SearchTorrentsUseCase

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/** ViewModel that handles the business logic of SearchResultsScreen. */
@HiltViewModel
class SearchResultsViewModel @Inject constructor(
    private val searchTorrentsUseCase: SearchTorrentsUseCase,
    private val settingsRepository: SettingsRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val connectivityChecker: ConnectivityChecker,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    // Let app crash if these two are not present.
    private val searchQuery = savedStateHandle.get<String>("query")!!
    private val searchCategory = savedStateHandle.get<String>("category")?.let(Category::valueOf)!!

    /** The state which drives the logic and used for producing UI state. */
    private val internalState = MutableStateFlow(InternalState())

    /** The UI state. */
    val uiState = combine(
        internalState,
        settingsRepository.enableNSFWMode,
        ::createUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = SearchResultsUiState(isLoading = true),
    )

    init {
        Log.i(TAG, "init is invoked")
        Log.d(TAG, "query = $searchQuery, category = $searchCategory")

        viewModelScope.launch {
            // TODO: This ViewModel shouldn't be responsible for maintaining
            //       search history.
            if (settingsRepository.saveSearchHistory.first()) {
                // Trim the query to prevent same query (e.g. 'one' and 'one ')
                // from end upping into the database.
                val query = searchQuery.trim()
                searchHistoryRepository.createNewSearchHistory(query = query)
            }
            performSearch()
        }
    }

    /** Returns the default sort options. */
    private suspend fun getDefaultSortOptions(): SortOptions {
        return combine(
            settingsRepository.defaultSortCriteria,
            settingsRepository.defaultSortOrder,
            ::SortOptions
        ).first()
    }

    /** Produces UI state from the given internal state and other parameters. */
    private suspend fun createUiState(
        internalState: InternalState,
        nsfwModeEnabled: Boolean,
    ): SearchResultsUiState {
        val (
            filterQuery,
            sortOptions,
            filterOptions,
            searchResults,
            isLoading,
            isSearching,
            isInternetError,
        ) = internalState
        Log.i(TAG, "Creating UI state")

        if (isLoading) {
            return SearchResultsUiState(isLoading = true)
        }

        if (isInternetError) {
            return SearchResultsUiState(isInternetError = true)
        }

        val filteredSearchResults = withContext(Dispatchers.Default) {
            searchResults
                .filterNSFW(isNSFWModeEnabled = nsfwModeEnabled)
                .run {
                    // Don't apply filter options until search completes.
                    if (isSearching) this else this.filterByOptions(filterOptions)
                }
                .filter {
                    filterQuery.isBlank() || it.name.contains(filterQuery, ignoreCase = true)
                }
                .customSort(criteria = sortOptions.criteria, order = sortOptions.order)
        }

        return SearchResultsUiState(
            searchQuery = searchQuery,
            searchCategory = searchCategory,
            searchResults = filteredSearchResults,
            currentSortCriteria = sortOptions.criteria,
            currentSortOrder = sortOptions.order,
            filterOptions = filterOptions,
            isLoading = false,
            isSearching = isSearching,
            isInternetError = false,
        )
    }

    /** Shows only those search results that contains the given query. */
    fun filterSearchResults(query: String) {
        val query = if (query.isNotBlank()) query.trim() else query
        internalState.update { it.copy(filterQuery = query) }
    }

    /** Updates the current sort options with the given options. */
    fun updateSortOptions(criteria: SortCriteria, order: SortOrder) {
        internalState.update {
            it.copy(sortOptions = SortOptions(criteria = criteria, order = order))
        }
    }

    /**
     * Shows or hides search results which are fetched from the search
     * provider whose ID matches with the given one.
     */
    fun toggleSearchProviderResults(searchProviderId: SearchProviderId) {
        val searchProviders = internalState.value.filterOptions.searchProviders.map {
            if (it.searchProviderId == searchProviderId) {
                it.copy(selected = !it.selected)
            } else {
                it
            }
        }
        val filterOptions = internalState.value.filterOptions.copy(
            searchProviders = searchProviders,
        )

        internalState.update { it.copy(filterOptions = filterOptions) }
    }

    /** Shows or hides dead torrents. */
    fun toggleDeadTorrents() {
        val filterOptions = with(internalState.value.filterOptions) {
            this.copy(deadTorrents = !this.deadTorrents)
        }
        internalState.update {
            it.copy(filterOptions = filterOptions)
        }
    }

    /** Refreshes the data by performing a new search. */
    fun refresh() {
        viewModelScope.launch { performSearch() }
    }

    /** Performs the search job. */
    private suspend fun performSearch() {
        Log.i(TAG, "performSearch() called")

        if (searchQuery.isBlank()) {
            internalState.update {
                it.copy(
                    isLoading = false,
                    isSearching = false,
                    isInternetError = false,
                )
            }
            return
        }

        if (!connectivityChecker.isInternetAvailable()) {
            Log.w(TAG, "Internet connection not available. Returning")
            internalState.update { it.copy(isLoading = false, isInternetError = true) }
            return
        }

        // TODO: Do this inside the init block if possible because this
        //       function is not supposed to load default value/s.
        val defaultSortOptions = getDefaultSortOptions()
        internalState.update { it.copy(sortOptions = defaultSortOptions) }

        searchTorrentsUseCase(query = searchQuery, category = searchCategory)
            .onCompletion { onSearchCompletion(cause = it) }
            .collect { onSearchResultsReceived(searchResults = it) }
    }

    /** Invoked when search results are received. */
    private fun onSearchResultsReceived(searchResults: List<SearchResult>) {
        Log.i(TAG, "onSearchResultsReceived() called")

        // TODO: Collect failures/errors as well and generate search results
        //       summary which can be used to display on UI or for debugging
        //       purpose.
        val searchResults = searchResults.mapNotNull { it.getOrNull() }.flatten()

        if (searchResults.isEmpty()) {
            Log.i(TAG, "Received empty results. Returning")
            return
        }
        Log.i(TAG, "Received ${searchResults.size} results")

        val searchProviders = searchResults
            .map { Pair(it.providerId, it.providerName) }
            .distinct()
            .sortedBy { (_, searchProviderName) -> searchProviderName }
            .map { (searchProviderId, searchProviderName) ->
                SearchProviderFilterOption(
                    searchProviderId = searchProviderId,
                    searchProviderName = searchProviderName,
                    enabled = true,
                    selected = true,
                )
            }

        internalState.update {
            it.copy(
                filterOptions = it.filterOptions.copy(
                    searchProviders = searchProviders,
                ),
                searchResults = searchResults,
                isLoading = false,
                isSearching = true,
            )
        }
    }

    /** Invoked when search completes or cancelled. */
    private fun onSearchCompletion(cause: Throwable?) {
        Log.i(TAG, "onSearchCompletion() called")

        if (cause is CancellationException) {
            Log.w(TAG, "Search is cancelled")
            return
        }

        Log.i(TAG, "Search completed", cause)
        internalState.update { it.copy(isLoading = false, isSearching = false) }
    }

    private companion object {
        private const val TAG = "SearchResultsViewModel"
    }
}

/** Applies the given filter options to this list. */
private fun List<Torrent>.filterByOptions(options: FilterOptions): List<Torrent> {
    val selectedSearchProvidersId = options.searchProviders.mapNotNull {
        if (it.selected) it.searchProviderId else null
    }

    return this
        .filter { options.deadTorrents || !it.isDead() }
        .filter {
            selectedSearchProvidersId.isEmpty() || it.providerId in selectedSearchProvidersId
        }
}

data class SearchResultsUiState(
    val searchQuery: String = "",
    val searchCategory: Category = Category.All,
    val searchResults: List<Torrent> = emptyList(),
    val currentSortCriteria: SortCriteria = SortCriteria.Default,
    val currentSortOrder: SortOrder = SortOrder.Default,
    val filterOptions: FilterOptions = FilterOptions(),
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val isInternetError: Boolean = false,
)

private data class InternalState(
    val filterQuery: String = "",
    val sortOptions: SortOptions = SortOptions(),
    val filterOptions: FilterOptions = FilterOptions(),
    val searchResults: List<Torrent> = emptyList(),
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val isInternetError: Boolean = false,
)

private data class SortOptions(
    val criteria: SortCriteria = SortCriteria.Default,
    val order: SortOrder = SortOrder.Default,
)

data class FilterOptions(
    val searchProviders: List<SearchProviderFilterOption> = emptyList(),
    val deadTorrents: Boolean = true,
)

data class SearchProviderFilterOption(
    val searchProviderId: SearchProviderId,
    val searchProviderName: String,
    val enabled: Boolean = false,
    val selected: Boolean = false,
)