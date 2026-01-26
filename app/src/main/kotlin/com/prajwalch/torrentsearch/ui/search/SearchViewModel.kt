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
import com.prajwalch.torrentsearch.domain.models.SearchException
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import java.io.OutputStream
import java.io.PrintWriter

import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class SearchUiState(
    val searchQuery: String = "",
    val searchCategory: Category = Category.All,
    val searchResults: ImmutableList<Torrent> = persistentListOf(),
    val searchFailures: ImmutableList<SearchException> = persistentListOf(),
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
    val selected: Boolean = false,
)

/** ViewModel that handles the business logic of SearchScreen. */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchTorrentsUseCase: SearchTorrentsUseCase,
    private val bookmarksRepository: BookmarksRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val connectivityChecker: ConnectivityChecker,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /** Current search query. */
    private val searchQuery = savedStateHandle.get<String>("query")!!

    /**
     * Current search category.
     *
     * If the category is not given, default category is fetched from the settings.
     */
    private lateinit var searchCategory: Category

    /** The internal or mutable UI state. */
    private val _uiState = MutableStateFlow(SearchUiState(searchQuery = searchQuery))

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

    /** Currently on-going search. */
    private var searchJob: Job? = null

    init {
        Log.d(TAG, "init")

        // 1. Maintain search history
        saveSearchQueryToHistory()
        // 2. Perform search
        searchJob = initializeCategoryAndSearch()
    }

    private fun saveSearchQueryToHistory() = viewModelScope.launch {
        if (!settingsRepository.saveSearchHistory.first()) {
            return@launch
        }
        // TODO: Do this in repository.
        //
        // Trim the query to prevent same query (e.g. 'one' and 'one ')
        // from end upping into the database.
        val query = searchQuery.trim()
        searchHistoryRepository.createNewSearchHistory(query = query)
    }

    private fun initializeCategoryAndSearch() = viewModelScope.launch {
        val category = savedStateHandle.get<Category>("category")
        // If the category is not given fallback to default one.
            ?: settingsRepository.defaultCategory.firstOrNull()
            ?: Category.All

        searchCategory = category
        _uiState.update { it.copy(searchCategory = category) }

        performSearch()
    }

    /** Produces UI state from the given internal state and other parameters. */
    private suspend fun createUiState(
        internalUiState: SearchUiState,
        filterQuery: String,
        nsfwModeEnabled: Boolean,
    ): SearchUiState {
        if (internalUiState.isLoading || internalUiState.isInternetError) {
            return internalUiState
        }

        val enabledSearchProvidersName = internalUiState.filterOptions.searchProviders.mapNotNull {
            if (it.selected) it.searchProviderName else null
        }
        val sortComparator = createSortComparator(
            criteria = internalUiState.sortOptions.criteria,
            order = internalUiState.sortOptions.order,
        )
        val filteredSearchResults = internalUiState
            .searchResults
            .asSequence()
            .filter {
                internalUiState.filterOptions.searchProviders.isEmpty()
                        || it.providerName in enabledSearchProvidersName
            }
            .filter { nsfwModeEnabled || !it.isNSFW() }
            .filter { internalUiState.filterOptions.deadTorrents || !it.isDead() }
            .filter { filterQuery.isBlank() || it.name.contains(filterQuery, ignoreCase = true) }
            .sortedWith(comparator = sortComparator)
            .toImmutableList()

        val isSearchResultsEmpty = internalUiState.searchResults.isEmpty()
        val resultsNotFound = isSearchResultsEmpty && !internalUiState.isSearching
        val resultsFilteredOut = !isSearchResultsEmpty && filteredSearchResults.isEmpty()

        return internalUiState.copy(
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
        searchJob?.cancel()
        searchJob = performSearch(refreshOnly = true)
    }

    /**
     * Reloads everything by resetting options currently set by the user
     * to default and performing a new search.
     */
    fun reload() {
        searchJob?.cancel()
        searchJob = performSearch()
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

    /** Exports the current search failures to given output stream. */
    fun exportSearchFailures(outputStream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            val printWriter = PrintWriter(outputStream.bufferedWriter(Charsets.UTF_8))

            printWriter.use {
                for (exception in uiState.value.searchFailures) {
                    it.println("----------${exception.searchProviderName}----------")
                    exception.printStackTrace(it)
                    it.println("----------${exception.searchProviderName}----------")
                    it.println()
                }
            }
        }
    }

    private fun performSearch(refreshOnly: Boolean = false) = viewModelScope.launch {
        if (refreshOnly) {
            _uiState.update { it.copy(isRefreshing = true) }
        } else {
            _uiState.update { it.copy(isLoading = true, isInternetError = false) }
        }

        if (!connectivityChecker.isInternetAvailable()) {
            Log.d(TAG, "Internet connection not available")

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isInternetError = true,
                )
            }
            return@launch
        }

        if (!refreshOnly) {
            val defaultSortOptions = settingsRepository.defaultSortOptions.first()
            _uiState.update { it.copy(sortOptions = defaultSortOptions) }
        }

        searchTorrentsUseCase(query = searchQuery, category = searchCategory)
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
        Log.d(TAG, "onSearchResultsReceived")

        val searchFailures = searchResults.failures
        val searchResults = searchResults.successes

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
                searchFailures = searchFailures,
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
        Log.i(TAG, "Search completed")

        _uiState.update { it.copy(isSearching = false) }
    }

    private companion object {
        private const val TAG = "SearchViewModel"
    }
}