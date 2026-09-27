package com.prajwalch.torrentsearch.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.data.repository.ViewedTorrentRepository
import com.prajwalch.torrentsearch.domain.TorrentQueryService
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.SearchProviderError
import com.prajwalch.torrentsearch.domain.model.SearchProviderResult
import com.prajwalch.torrentsearch.domain.model.SortCriteria
import com.prajwalch.torrentsearch.domain.model.SortOptions
import com.prajwalch.torrentsearch.domain.model.SortOrder
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.filterIfAll
import com.prajwalch.torrentsearch.domain.model.sortedWithComparator
import com.prajwalch.torrentsearch.filter.TorrentFilters
import com.prajwalch.torrentsearch.network.ConnectivityChecker
import com.prajwalch.torrentsearch.util.createSortComparator

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

data class SearchUiState(
    val searchParams: SearchParams = SearchParams(),
    val searchState: SearchState = SearchState.Loading,
    val torrents: PersistentList<Torrent> = persistentListOf(),
    val errors: PersistentList<ErrorItem> = persistentListOf(),
    val sortOptions: SortOptions = SortOptions(),
    val torrentFilter: TorrentFilter = TorrentFilter(),
    val viewedTorrentIds: Set<String> = emptySet(),
)

data class SearchParams(
    val query: String = "",
    val category: Category = Category.All,
)

sealed interface SearchState {
    data object Loading : SearchState
    data object InternetError : SearchState
    data object ResultsNotFound : SearchState

    sealed interface ResultsAvailable : SearchState {
        data object Complete : ResultsAvailable
        data object Searching : ResultsAvailable
        data object Refreshing : ResultsAvailable
    }
}

data class ErrorItem(
    val state: State = State.Active,
    val providerError: SearchProviderError,
) {
    sealed interface State {
        data object Active : State
        data object Retrying : State
        data object Resolved : State
    }
}

data class TorrentFilter(
    val providers: ImmutableList<SearchProviderOption> = persistentListOf(),
    val showDeadTorrents: Boolean = true,
    val category: Category = Category.All,
    val hideViewed: Boolean = false,
) {
    data class SearchProviderOption(
        val provider: String,
        val selected: Boolean = false,
    )
}

/**
 * A ViewModel that handles the business logic of search screen.
 */
@KoinViewModel
class SearchViewModel(
    private val torrentQueryService: TorrentQueryService,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val viewedTorrentRepository: ViewedTorrentRepository,
    private val connectivityChecker: ConnectivityChecker,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /**
     * The search params set by the user.
     *
     * Search params are final and not allowed to modify or update for
     * requesting a new search with new params.
     */
    private val searchParams = getSearchParams()

    /**
     * The search results loader.
     *
     * It produces raw and unprocessed results, which are directly feed into
     * the processor for post-processing such as filtering and sorting.
     */
    private val resultsLoader = SearchResultsLoader(
        scope = viewModelScope,
        torrentQueryService = torrentQueryService,
        connectivityChecker = connectivityChecker,
    )

    /**
     * The torrents' processor.
     *
     * It pulls torrents from the given flow, does some post-processing
     * and produces a result containing processed torrents, and different
     * filter configurations which are directly send to UI.
     */
    private val torrentsProcessor = TorrentsProcessor(
        torrents = resultsLoader.torrents,
        settingsRepository = settingsRepository,
        viewedTorrentIds = viewedTorrentRepository.getAllViewedIds(),
        searchCategory = searchParams.category,
    )

    /**
     * The primary, read-only UI state.
     */
    val uiState: StateFlow<SearchUiState> =
        combine(
            resultsLoader.searchState,
            resultsLoader.errors,
            torrentsProcessor.result,
            viewedTorrentRepository.getAllViewedIds(),
        ) { searchState, errors, processResult, viewedTorrentIds ->
            SearchUiState(
                searchParams = searchParams,
                searchState = searchState,
                torrents = processResult.torrents,
                errors = errors.values.toPersistentList(),
                sortOptions = processResult.sortOptions,
                torrentFilter = processResult.filter,
                viewedTorrentIds = viewedTorrentIds,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = SearchUiState(searchState = SearchState.Loading),
        )

    init {
        saveSearchQuery()
        // Initiate search.
        viewModelScope.launch {
            val defaultSortOptions = settingsRepository.defaultSortOptions.first()
            torrentsProcessor.updateSortCriteria(defaultSortOptions.criteria)
            torrentsProcessor.updateSortOrder(defaultSortOptions.order)

            search()
        }
    }

    @Throws(IllegalStateException::class)
    private fun getSearchParams(): SearchParams {
        val query = savedStateHandle.get<String>("query")
            // Accepting the keyboard suggestion will add extra trailing space.
            // By removing it here, the query remains clean for both history save
            // and search task.
            ?.trim()
        // TODO: Support empty search query.
            ?: error("Empty search query isn't supported")
        val category = savedStateHandle.get<Category>("category") ?: Category.All

        return SearchParams(query, category)
    }

    private fun saveSearchQuery() = viewModelScope.launch {
        if (settingsRepository.saveSearchHistory.first()) {
            searchHistoryRepository.createNewSearchHistory(query = searchParams.query)
        }
    }

    fun search() {
        resultsLoader.search(searchParams.query, searchParams.category)
    }

    fun stopSearch() {
        resultsLoader.stopSearch()
    }

    fun refreshSearchResults() {
        resultsLoader.refresh(searchParams.query, searchParams.category)
    }

    fun retryError(errorItem: ErrorItem) {
        resultsLoader.retryError(errorItem, searchParams.query, searchParams.category)
    }

    fun filterSearchResultsByName(query: String) {
        torrentsProcessor.updateFilterQuery(query)
    }

    fun updateSortCriteria(criteria: SortCriteria) {
        torrentsProcessor.updateSortCriteria(criteria)
    }

    fun updateSortOrder(order: SortOrder) {
        torrentsProcessor.updateSortOrder(order)
    }

    fun toggleSearchProviderResults(providerName: String) {
        torrentsProcessor.toggleSearchProviderResults(providerName)
    }

    fun selectAllSearchProviders() {
        torrentsProcessor.updateExcludedSearchProviders(emptySet())
    }

    fun deselectAllSearchProviders() {
        uiState
            .value
            .torrentFilter
            .providers
            .map { it.provider }
            .toSet()
            .let(torrentsProcessor::updateExcludedSearchProviders)
    }

    fun invertSearchProvidersSelection() {
        uiState
            .value
            .torrentFilter
            .providers
            .filter { it.selected }
            .map { it.provider }
            .toSet()
            .let(torrentsProcessor::updateExcludedSearchProviders)
    }

    fun toggleDeadTorrents() {
        torrentsProcessor.toggleShowDeadTorrents()
    }

    fun updateCategoryFilter(category: Category) {
        torrentsProcessor.updateCategory(category)
    }

    fun toggleHideViewedTorrents() {
        torrentsProcessor.toggleHideViewed()
    }

    fun markAsViewed(infoHash: String) {
        viewModelScope.launch {
            viewedTorrentRepository.markAsViewed(infoHash)
        }
    }
}

/**
 * Manages and handles the search results fetching task.
 *
 * It's the first stage in the search pipeline which is responsible for
 * maintaining and producing search state and results. As the name suggests,
 * it doesn't perform any pre-processing on the results.
 *
 * @param scope The [CoroutineScope] in which the search is performed.
 * @param torrentQueryService The service which queries the torrents.
 * @param connectivityChecker The helper class for checking network connection.
 */
private class SearchResultsLoader(
    private val scope: CoroutineScope,
    private val torrentQueryService: TorrentQueryService,
    private val connectivityChecker: ConnectivityChecker,
) {
    /**
     * The internal, mutable state of [SearchState].
     * This flow is updated constantly during the search lifecycle.
     */
    private val _searchState = MutableStateFlow<SearchState>(SearchState.Loading)

    /**
     * The public, read-only state of the [SearchState].
     */
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _torrents = MutableStateFlow(persistentListOf<Torrent>())
    val torrents: StateFlow<PersistentList<Torrent>> = _torrents.asStateFlow()

    private val _errors = MutableStateFlow(persistentMapOf<String, ErrorItem>())
    val errors: StateFlow<PersistentMap<String, ErrorItem>> = _errors.asStateFlow()

    /**
     * The ongoing background search job.
     *
     * Before starting a new lifecycle current job is explicitly canceled
     * whether it has completed or not to prevent unnecessary usage of resource.
     */
    private var searchJob: Job? = null

    /**
     * Initiates a new search for the given query and category.
     */
    fun search(query: String, category: Category) {
        searchJob?.cancel()
        searchJob = scope.launch {
            _searchState.value = SearchState.Loading

            if (!connectivityChecker.isInternetAvailable()) {
                _searchState.value = SearchState.InternetError
                return@launch
            }

            executeSearch(query = query, category = category)
        }
    }

    /**
     * Stops the ongoing search.
     */
    fun stopSearch() {
        searchJob?.cancel()
        searchJob = null
    }

    /**
     * Refreshes the current search results by refetching new results.
     */
    fun refresh(query: String, category: Category) {
        searchJob?.cancel()
        searchJob = scope.launch {
            _searchState.value = SearchState.ResultsAvailable.Refreshing

            if (!connectivityChecker.isInternetAvailable()) {
                _searchState.value = SearchState.ResultsAvailable.Complete
                return@launch
            }

            executeSearch(query = query, category = category)
        }
    }


    /**
     * Executes a new search for the given query and category.
     */
    private suspend fun executeSearch(query: String, category: Category) {
        torrentQueryService.searchTorrents(query = query, category = category)
            .conflate()
            .onCompletion {
                _searchState.value = if (_torrents.value.isEmpty()) {
                    SearchState.ResultsNotFound
                } else {
                    SearchState.ResultsAvailable.Complete
                }
            }
            .collect { searchResults ->
                // Unfold
                _torrents.value = searchResults.torrents
                _errors.value = searchResults.errors
                    .map { ErrorItem(providerError = it) }
                    .associateBy { it.providerError.providerId }
                    .toPersistentMap()

                _searchState.value = SearchState.ResultsAvailable.Searching
            }
    }

    fun retryError(errorItem: ErrorItem, query: String, category: Category) {
        scope.launch {
            val providerId = errorItem.providerError.providerId

            _errors.update {
                it.putting(
                    key = providerId,
                    value = errorItem.copy(state = ErrorItem.State.Retrying),
                )
            }

            val result = torrentQueryService.searchTorrents(
                providerId = providerId,
                query = query,
                category = category,
            )

            when (result) {
                is SearchProviderResult.Error -> {
                    // Mark the given error as active.
                    _errors.update {
                        // Since the given error item is not modified,
                        // we can directly use that instead of making a new copy.
                        it.putting(key = providerId, value = errorItem)
                    }
                }

                is SearchProviderResult.Success<ImmutableList<Torrent>> -> {
                    // Push torrents
                    val newTorrents = result.value
                    _torrents.update { it.addingAll(newTorrents) }

                    // Mark the given error as resolved.
                    _errors.update {
                        it.putting(
                            key = providerId,
                            value = errorItem.copy(state = ErrorItem.State.Resolved),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Setups and manages the execution of different transformation
 * operations on the [torrents].
 *
 * It's the second stage in the search pipeline, responsible for performing
 * post-processing on the [torrents].
 *
 * @param torrents The flow that emits the torrents.
 * @param settingsRepository The repository from where user-define filter options are fetched.
 * @param viewedTorrentIds The flow that emits the viewed torrent IDs.
 * @param searchCategory The search [Category].
 */
private class TorrentsProcessor(
    torrents: Flow<PersistentList<Torrent>>,
    settingsRepository: SettingsRepository,
    searchCategory: Category = Category.All,
    private val viewedTorrentIds: Flow<Set<String>>,
) {
    data class ProcessResult(
        val torrents: PersistentList<Torrent>,
        val filter: TorrentFilter,
        val sortOptions: SortOptions,
    )

    /**
     * A snapshot of the current configuration of [TorrentFilter].
     */
    private data class TorrentFilterConfig(
        val query: String = "",
        val excludedProviders: Set<String> = emptySet(),
        val showDeadTorrents: Boolean = true,
        val category: Category = Category.All,
        val hideViewed: Boolean = false,
    )

    /**
     * The internal, mutable state of [TorrentFilterConfig].
     */
    private val filterConfig = MutableStateFlow(TorrentFilterConfig(category = searchCategory))

    /**
     * The internal, mutable state of sort options.
     */
    private val sortOptions = MutableStateFlow(SortOptions())

    /**
     * The flow that emits the [ProcessResult].
     */
    val result: Flow<ProcessResult> =
        combine(
            torrents,
            filterConfig,
            sortOptions,
            settingsRepository.enableNSFWMode,
            ::processTorrents,
        ).flowOn(Dispatchers.Default)

    /**
     * Processes the given [torrents] using given configurations and
     * returns a [ProcessResult].
     */
    private suspend fun processTorrents(
        torrents: PersistentList<Torrent>,
        filterConfig: TorrentFilterConfig,
        sortOptions: SortOptions,
        nsfwModeEnabled: Boolean,
    ): ProcessResult {
        val sortComparator = createSortComparator(
            criteria = sortOptions.criteria,
            order = sortOptions.order,
        )
        val viewedTorrentIds = if (filterConfig.hideViewed) {
            getCurrentViewedTorrentIds()
        } else {
            emptySet()
        }

        val processedTorrents = torrents.filterIfAll {
            add(TorrentFilters.notExcludedProvider(filterConfig.excludedProviders))

            if (!nsfwModeEnabled) add(TorrentFilters.isSfw())
            if (!filterConfig.showDeadTorrents) add(TorrentFilters.isAlive())
            if (filterConfig.hideViewed) add(TorrentFilters.notViewed(viewedTorrentIds))
            if (filterConfig.query.isNotBlank())
                add(TorrentFilters.matchesQuery(filterConfig.query))
            if (filterConfig.category != Category.All)
                add(TorrentFilters.matchesCategory(filterConfig.category))
        }.sortedWithComparator(sortComparator)
        val torrentFilter = createTorrentFilter(filterConfig, torrents)

        return ProcessResult(
            torrents = processedTorrents,
            filter = torrentFilter,
            sortOptions = sortOptions,
        )
    }

    private suspend fun getCurrentViewedTorrentIds(): Set<String> {
        return viewedTorrentIds.firstOrNull().orEmpty()
    }

    /**
     * Creates a [TorrentFilter] from the given snapshot of filter config.
     */
    private fun createTorrentFilter(
        filterConfig: TorrentFilterConfig,
        torrents: ImmutableList<Torrent>,
    ): TorrentFilter {
        val providerFilters = torrents
            .map { it.providerName }
            .distinct()
            .map {
                TorrentFilter.SearchProviderOption(
                    provider = it,
                    selected = it !in filterConfig.excludedProviders,
                )
            }

        return TorrentFilter(
            providers = providerFilters.toImmutableList(),
            showDeadTorrents = filterConfig.showDeadTorrents,
            category = filterConfig.category,
            hideViewed = filterConfig.hideViewed,
        )
    }

    /**
     * Shows only those search results that contains the given query.
     */
    fun updateFilterQuery(query: String) {
        filterConfig.update { it.copy(query = query.trim()) }
    }

    /**
     * Shows or hides search results associated with the given provider name.
     */
    fun toggleSearchProviderResults(providerName: String) {
        filterConfig.update {
            val newExclusions = if (providerName in it.excludedProviders) {
                // Remove from exclusion list.
                it.excludedProviders - providerName
            } else {
                // Exclude it.
                it.excludedProviders + providerName
            }
            it.copy(excludedProviders = newExclusions)
        }
    }

    /**
     * Updates the current search providers exclusion list with the given one.
     */
    fun updateExcludedSearchProviders(providers: Set<String>) {
        filterConfig.update { it.copy(excludedProviders = providers) }
    }

    /**
     * Shows or hides dead torrents from search results.
     */
    fun toggleShowDeadTorrents() {
        filterConfig.update { it.copy(showDeadTorrents = !it.showDeadTorrents) }
    }

    /**
     * Updates the current category with the given one.
     */
    fun updateCategory(category: Category) {
        filterConfig.update { it.copy(category = category) }
    }

    /**
     * Toggles the hide viewed filter.
     */
    fun toggleHideViewed() {
        filterConfig.update { it.copy(hideViewed = !it.hideViewed) }
    }

    /**
     * Updates the current sort criteria with the given one.
     */
    fun updateSortCriteria(criteria: SortCriteria) {
        sortOptions.update { it.copy(criteria = criteria) }
    }

    /**
     * Updates the current sort order with the given one.
     */
    fun updateSortOrder(order: SortOrder) {
        sortOptions.update { it.copy(order = order) }
    }
}