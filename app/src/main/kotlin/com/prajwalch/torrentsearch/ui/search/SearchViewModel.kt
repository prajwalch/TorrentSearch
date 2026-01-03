package com.prajwalch.torrentsearch.ui.search

import android.util.Log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.BookmarksRepository
import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.SearchTorrentsUseCase
import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.SearchResults
import com.prajwalch.torrentsearch.domain.models.SortCriteria
import com.prajwalch.torrentsearch.domain.models.SortOptions
import com.prajwalch.torrentsearch.domain.models.SortOrder
import com.prajwalch.torrentsearch.domain.models.Torrent
import com.prajwalch.torrentsearch.network.ConnectivityChecker
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
import kotlinx.coroutines.flow.conflate
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
    private val bookmarksRepository: BookmarksRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val connectivityChecker: ConnectivityChecker,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    // Let app crash if these two are not present.
    private val searchQuery = savedStateHandle.get<String>("query")!!
    private val searchCategory = savedStateHandle.get<String>("category")?.let(Category::valueOf)!!

    /** The internal or mutable UI state. */
    private val _uiState = MutableStateFlow(
        SearchUiState(
            searchQuery = searchQuery,
            searchCategory = searchCategory,
        )
    )

    /**
     * Query used to filter search results.
     *
     * This is the copy of filter query which is maintained and given by the UI.
     */
    private val filterQuery = MutableStateFlow("")

    /** The UI state. */
    val uiState = combine(
        _uiState,
        filterQuery,
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

            performNewSearch()
        }
    }

    /** Produces UI state from the given internal state and other parameters. */
    private suspend fun createUiState(
        currentUiState: SearchUiState,
        filterQuery: String,
        nsfwModeEnabled: Boolean,
    ): SearchUiState {
        Log.i(TAG, "Creating UI state")

        if (currentUiState.isLoading) {
            return currentUiState
        }

        if (currentUiState.isInternetError) {
            return currentUiState
        }

        val enabledSearchProvidersName = currentUiState.filterOptions.searchProviders.mapNotNull {
            if (it.selected) it.searchProviderName else null
        }
        val sortComparator = createSortComparator(
            criteria = currentUiState.sortOptions.criteria,
            order = currentUiState.sortOptions.order,
        )
        val filteredSearchResults = currentUiState
            .searchResults
            .asSequence()
            .filter {
                currentUiState.filterOptions.searchProviders.isEmpty()
                        || it.providerName in enabledSearchProvidersName
            }
            .filter { nsfwModeEnabled || !it.isNSFW() }
            .filter { currentUiState.filterOptions.deadTorrents || !it.isDead() }
            .filter { filterQuery.isBlank() || it.name.contains(filterQuery, ignoreCase = true) }
            .sortedWith(comparator = sortComparator)
            .toImmutableList()

        val isSearchResultsEmpty = currentUiState.searchResults.isEmpty()
        val resultsNotFound = isSearchResultsEmpty && !currentUiState.isSearching
        val resultsFilteredOut = !isSearchResultsEmpty && filteredSearchResults.isEmpty()

        return currentUiState.copy(
            searchResults = filteredSearchResults,
            resultsNotFound = resultsNotFound,
            resultsFilteredOut = resultsFilteredOut,
        )
    }

    /**
     * Refreshes only the search results without changing or resetting options
     * currently set by the user to default.
     */
    fun refreshSearchResults() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            if (!connectivityChecker.isInternetAvailable()) {
                _uiState.update { it.copy(isRefreshing = false) }
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
            _uiState.update {
                it.copy(isLoading = true, isInternetError = false)
            }
            performNewSearch()
        }
    }

    /** Shows only those search results that contains the given query. */
    fun filterSearchResults(query: String) {
        val query = if (query.isNotBlank()) query.trim() else query
        filterQuery.value = query
    }

    /** Updates the sort criteria. */
    fun updateSortCriteria(criteria: SortCriteria) {
        _uiState.update {
            it.copy(sortOptions = it.sortOptions.copy(criteria = criteria))
        }
    }

    /** Updates the sort order. */
    fun updateSortOrder(order: SortOrder) {
        _uiState.update {
            it.copy(sortOptions = it.sortOptions.copy(order = order))
        }
    }

    /**
     * Shows or hides search results which are fetched from the search
     * provider whose ID matches with the given one.
     */
    fun toggleSearchProviderResults(searchProviderName: String) {
        val filterOptions = with(_uiState.value.filterOptions) {
            val searchProviders = this.searchProviders
                .map {
                    if (it.searchProviderName == searchProviderName) {
                        it.copy(selected = !it.selected)
                    } else {
                        it
                    }
                }
                .toImmutableList()

            this.copy(searchProviders = searchProviders)
        }
        _uiState.update { it.copy(filterOptions = filterOptions) }
    }

    /** Shows or hides dead torrents. */
    fun toggleDeadTorrents() {
        val filterOptions = with(_uiState.value.filterOptions) {
            this.copy(deadTorrents = !this.deadTorrents)
        }
        _uiState.update {
            it.copy(filterOptions = filterOptions)
        }
    }

    /** Bookmarks the given [Torrent]. */
    fun bookmarkTorrent(torrent: Torrent) {
        viewModelScope.launch {
            bookmarksRepository.bookmarkTorrent(torrent = torrent)
        }
    }

    /** Performs a new search by resetting every state. */
    private suspend fun performNewSearch() {
        Log.i(TAG, "performNewSearch() called")

        if (searchQuery.isBlank()) {
            _uiState.update {
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
            _uiState.update { it.copy(isLoading = false, isInternetError = true) }
            return
        }

        val defaultSortOptions = settingsRepository.defaultSortOptions.first()
        _uiState.update { it.copy(sortOptions = defaultSortOptions) }

        search(query = searchQuery, category = searchCategory)
    }

    /** Performs a new search. */
    private suspend fun search(query: String, category: Category) {
        searchTorrentsUseCase(query = query, category = category)
            .onStart { onSearchStart() }
            .onCompletion { onSearchCompletion(cause = it) }
            .conflate()
            .collect { onSearchResultsReceived(searchResults = it) }
    }

    /** Invoked when search is about to begin. */
    private fun onSearchStart() {
        _uiState.update {
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
        _uiState.update {
            // It doesn't make sense to create search provider filter option
            // when only single search provider is enabled.
            val filterOptions = if (searchProvidersFilterOption.size > 1) {
                it.filterOptions.copy(searchProviders = searchProvidersFilterOption)
            } else {
                it.filterOptions
            }

            it.copy(
                filterOptions = filterOptions,
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
            .distinctBy { it.providerName }
            .map { it.providerName }
            .sorted()
            .map { SearchProviderFilterOption(searchProviderName = it, selected = true) }
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
        val filterOptions = with(_uiState.value.filterOptions) {
            val searchProviders = this.searchProviders
                .map { it.copy(enabled = true) }
                .toImmutableList()

            this.copy(searchProviders = searchProviders)
        }
        _uiState.update {
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
    val resultsNotFound: Boolean = false,
    val resultsFilteredOut: Boolean = false,
)

data class FilterOptions(
    val searchProviders: ImmutableList<SearchProviderFilterOption> = persistentListOf(),
    val deadTorrents: Boolean = true,
)

data class SearchProviderFilterOption(
    val searchProviderName: String,
    val enabled: Boolean = false,
    val selected: Boolean = false,
)