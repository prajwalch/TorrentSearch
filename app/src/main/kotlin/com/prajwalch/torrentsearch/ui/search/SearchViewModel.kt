package com.prajwalch.torrentsearch.ui.search

import android.util.Log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.SearchResults
import com.prajwalch.torrentsearch.models.SortCriteria
import com.prajwalch.torrentsearch.models.SortOrder
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.ConnectivityChecker
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.usecases.SearchTorrentsUseCase
import com.prajwalch.torrentsearch.utils.createSortComparator

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/** ViewModel that handles the business logic of SearchScreen. */
@HiltViewModel
class SearchViewModel @Inject constructor(
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
        initialValue = SearchUiState(isLoading = true),
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

            loadResults()
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
    ): SearchUiState {
        val (
            filterQuery,
            sortOptions,
            filterOptions,
            searchResults,
            isLoading,
            isSearching,
            isRefreshing,
            isInternetError,
        ) = internalState
        Log.i(TAG, "Creating UI state")

        if (isLoading) {
            return SearchUiState(isLoading = true)
        }

        if (isInternetError) {
            return SearchUiState(isLoading = false, isInternetError = true)
        }

        val enabledSearchProvidersId = filterOptions.searchProviders.mapNotNull {
            if (it.enabled && it.selected) it.searchProviderId else null
        }
        val sortComparator = createSortComparator(
            criteria = sortOptions.criteria,
            order = sortOptions.order,
        )
        val filteredSearchResults = searchResults
            .asSequence()
            .filter { nsfwModeEnabled || !it.isNSFW() }
            .filter { filterOptions.deadTorrents || !it.isDead() }
            .filter {
                enabledSearchProvidersId.isEmpty() || it.providerId in enabledSearchProvidersId
            }
            .filter { filterQuery.isBlank() || it.name.contains(filterQuery, ignoreCase = true) }
            .sortedWith(comparator = sortComparator)
            .toImmutableList()

        return SearchUiState(
            searchQuery = searchQuery,
            searchCategory = searchCategory,
            searchResults = filteredSearchResults,
            sortOptions = sortOptions,
            filterOptions = filterOptions,
            isLoading = false,
            isSearching = isSearching,
            isRefreshing = isRefreshing,
            isInternetError = false,
        )
    }

    /**
     * Refreshes only the search results without changing or resetting options
     * currently set by the user to default.
     */
    fun refreshSearchResults() {
        viewModelScope.launch {
            internalState.update { it.copy(isRefreshing = true) }

            if (!connectivityChecker.isInternetAvailable()) {
                internalState.update { it.copy(isRefreshing = false) }
                return@launch
            }

            search(query = searchQuery, category = searchCategory)
        }
    }

    /**
     * Reloads everything by resetting options currently set by the user
     * to default and performing a new search.
     */
    fun reload() {
        viewModelScope.launch {
            internalState.update { it.copy(isLoading = true) }
            loadResults()
        }
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
        val filterOptions = with(internalState.value.filterOptions) {
            val searchProviders = this.searchProviders
                .map {
                    if (it.searchProviderId == searchProviderId) {
                        it.copy(selected = !it.selected)
                    } else {
                        it
                    }
                }
                .toImmutableList()

            this.copy(searchProviders = searchProviders)
        }
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

    /** Performs the search job. */
    private suspend fun loadResults() {
        Log.i(TAG, "loadResults() called")

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

        val defaultSortOptions = getDefaultSortOptions()
        internalState.update { it.copy(sortOptions = defaultSortOptions) }

        search(query = searchQuery, category = searchCategory)
    }

    /** Performs a new search. */
    private suspend fun search(query: String, category: Category) {
        searchTorrentsUseCase(query = query, category = category)
            .onStart { onSearchStart() }
            .onCompletion { onSearchCompletion(cause = it) }
            .collect { onSearchResultsReceived(searchResults = it) }
    }

    /** Invoked when search is about to begin. */
    private fun onSearchStart() {
        internalState.update {
            it.copy(
                isLoading = false,
                isSearching = true,
                isRefreshing = false,
            )
        }
    }

    /** Invoked when search results are received. */
    private fun onSearchResultsReceived(searchResults: SearchResults) {
        Log.i(TAG, "onSearchResultsReceived() called")

        // TODO: Collect failures/errors as well and generate search results
        //       summary which can be used to display on UI or for debugging
        //       purpose.
        val searchResults = searchResults.successes

        if (searchResults.isEmpty()) {
            Log.i(TAG, "Received empty results. Returning")
            return
        }
        Log.i(TAG, "Received ${searchResults.size} results")

        val searchProvidersFilterOption = createSearchProvidersFilterOption(
            searchResults = searchResults,
        )
        internalState.update {
            it.copy(
                filterOptions = it.filterOptions.copy(
                    searchProviders = searchProvidersFilterOption,
                ),
                searchResults = searchResults,
            )
        }
    }

    /** Creates search providers filter option from the given search results. */
    private fun createSearchProvidersFilterOption(
        searchResults: ImmutableList<Torrent>,
    ): ImmutableList<SearchProviderFilterOption> {
        return searchResults
            .asSequence()
            .distinctBy { it.providerId }
            .sortedBy { it.providerName }
            .map { Pair(it.providerId, it.providerName) }
            .map { (searchProviderId, searchProviderName) ->
                SearchProviderFilterOption(
                    searchProviderId = searchProviderId,
                    searchProviderName = searchProviderName,
                    selected = true,
                )
            }
            .toImmutableList()
    }

    /** Invoked when search completes or cancelled. */
    private fun onSearchCompletion(cause: Throwable?) {
        Log.i(TAG, "onSearchCompletion() called")

        if (cause is CancellationException) {
            Log.w(TAG, "Search is cancelled")
            return
        }

        Log.i(TAG, "Search completed", cause)
        val filterOptions = with(internalState.value.filterOptions) {
            val searchProviders = this.searchProviders
                .map { it.copy(enabled = true) }
                .toImmutableList()

            this.copy(searchProviders = searchProviders)
        }
        internalState.update {
            it.copy(filterOptions = filterOptions, isSearching = false)
        }
    }

    private companion object {
        private const val TAG = "SearchViewModel"
    }
}

data class SearchUiState(
    val searchQuery: String = "",
    val searchCategory: Category = Category.All,
    val searchResults: ImmutableList<Torrent> = persistentListOf(),
    val sortOptions: SortOptions = SortOptions(),
    val filterOptions: FilterOptions = FilterOptions(),
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val isRefreshing: Boolean = false,
    val isInternetError: Boolean = false,
)

private data class InternalState(
    val filterQuery: String = "",
    val sortOptions: SortOptions = SortOptions(),
    val filterOptions: FilterOptions = FilterOptions(),
    val searchResults: ImmutableList<Torrent> = persistentListOf(),
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val isRefreshing: Boolean = false,
    val isInternetError: Boolean = false,
)

data class SortOptions(
    val criteria: SortCriteria = SortCriteria.Default,
    val order: SortOrder = SortOrder.Default,
)

data class FilterOptions(
    val searchProviders: ImmutableList<SearchProviderFilterOption> = persistentListOf(),
    val deadTorrents: Boolean = true,
)

data class SearchProviderFilterOption(
    val searchProviderId: SearchProviderId,
    val searchProviderName: String,
    val enabled: Boolean = false,
    val selected: Boolean = false,
)