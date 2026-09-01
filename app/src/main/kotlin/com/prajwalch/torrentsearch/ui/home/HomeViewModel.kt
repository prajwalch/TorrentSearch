package com.prajwalch.torrentsearch.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.SearchProvidersManager
import com.prajwalch.torrentsearch.domain.model.Category

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

data class HomeUiState(
    val searchSuggestions: List<String> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val categories: List<Category> = Category.entries,
    val selectedCategory: Category = Category.All,
    val settings: HomeRelevantSettings = HomeRelevantSettings(),
)

data class HomeRelevantSettings(
    val searchHistoryEnabled: Boolean = true,
    val searchProvidersInitialized: Boolean? = null,
)

/**
 * The ViewModel which handles the business logic of home screen.
 */
@KoinViewModel
class HomeViewModel(
    searchHistoryRepository: SearchHistoryRepository,
    settingsRepository: SettingsRepository,
    private val searchProvidersManager: SearchProvidersManager,
) : ViewModel() {
    /**
     * The internal source for the current search query used only for
     * filtering search suggestions.
     *
     * UI maintains the query by itself but notifies the ViewModel whenever the
     * query changes. We then update this flow with copy of the query.
     */
    private val searchQuery = MutableStateFlow("")

    /**
     * The primary asynchronous stream of search suggestions which is shown
     * on the expanded search bar.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchSuggestions: Flow<List<String>> =
        combine(
            searchQuery,
            settingsRepository.showSearchHistory,
            ::Pair,
        ).flatMapLatest { (query, showSearchSuggestions) ->
            when {
                // Avoid fetching histories when not needed.
                !showSearchSuggestions -> flowOf(emptyList())
                query.isBlank() -> searchHistoryRepository.getAllSearchHistories()
                else -> searchHistoryRepository.getSearchHistoriesByTerm(query)
            }
        }.map { histories ->
            histories.map { it.query }
        }

    /**
     * The primary asynchronous stream of selectable [Category].
     */
    private val selectableCategories: Flow<List<Category>> =
        settingsRepository.enableNSFWMode.map { nsfwModeEnabled ->
            Category.entries.filter { nsfwModeEnabled || !it.isNSFW }
        }

    /**
     * The internal, primary mutable source for the currently selected
     * category. The flow is updated on demand from the UI.
     */
    private val selectedCategory = MutableStateFlow(Category.All)

    private val homeRelevantSettings: Flow<HomeRelevantSettings> =
        combine(
            settingsRepository.saveSearchHistory,
            settingsRepository.searchProvidersInitialized,
        ) { searchHistoryEnabled, searchProvidersInitialized ->
            HomeRelevantSettings(
                searchHistoryEnabled = searchHistoryEnabled,
                searchProvidersInitialized = searchProvidersInitialized,
            )
        }

    /**
     * The primary read-only UI state.
     */
    val uiState: StateFlow<HomeUiState> =
        combine(
            searchSuggestions,
            searchHistoryRepository.getRecentSearches(),
            selectableCategories,
            selectedCategory,
            homeRelevantSettings,
        ) {
                searchSuggestions,
                recentSearches,
                selectableCategories,
                selectedCategory,
                homeRelevantSettings,
            ->
            val selectedCategory = when {
                selectedCategory in selectableCategories -> selectedCategory
                else -> Category.All
            }

            HomeUiState(
                searchSuggestions = searchSuggestions,
                recentSearches = recentSearches,
                categories = selectableCategories,
                selectedCategory = selectedCategory,
                settings = homeRelevantSettings,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = HomeUiState(),
        )

    /**
     * Sets the currently selected category to given one.
     */
    fun setCategory(category: Category) {
        selectedCategory.value = category
    }

    /**
     * Filters search suggestions by the given query.
     */
    fun filterSearchSuggestions(query: String) {
        searchQuery.value = query
    }

    fun enableDefaultSearchProviders() {
        viewModelScope.launch {
            searchProvidersManager.enableDefaultSearchProviders()
        }
    }

    fun skipDefaultSearchProviders() {
        viewModelScope.launch {
            searchProvidersManager.skipDefaultSearchProviders()
        }
    }
}