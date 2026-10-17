package com.prajwalch.torrentsearch.ui.tv.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.home.HomeViewModel
import com.prajwalch.torrentsearch.ui.home.ProvidersCheckState
import com.prajwalch.torrentsearch.ui.tv.component.LocalTvContentFocusRequester
import com.prajwalch.torrentsearch.ui.tv.component.TvActionButton
import com.prajwalch.torrentsearch.ui.tv.component.TvChip
import com.prajwalch.torrentsearch.ui.tv.component.TvActionButton
import com.prajwalch.torrentsearch.ui.tv.component.TvChipRow
import com.prajwalch.torrentsearch.ui.tv.component.TvTextInput
import com.prajwalch.torrentsearch.ui.tv.theme.tvCardSurface
import com.prajwalch.torrentsearch.ui.tv.component.tvFocusable
import com.prajwalch.torrentsearch.ui.tv.component.displayName
import com.prajwalch.torrentsearch.ui.tv.component.tvListContentPadding
import com.prajwalch.torrentsearch.ui.tv.theme.spaces

import org.koin.androidx.compose.koinViewModel

/**
 * The TV home screen.
 *
 * Differences from the handheld `HomeScreen`:
 *
 * - No `pullToRefresh`: the provider check is a one-shot startup probe here, and
 *   pull-to-refresh has no D-pad equivalent.
 * - The provider-check banner is no longer swipe-dismissible; it renders as a
 *   plain inline status row with an explicit action.
 * - `LazyColumn` instead of a `verticalScroll` column, so focus traversal brings
 *   items into view correctly.
 */
@Composable
fun TvHomeScreen(
    onSearch: (query: String, category: Category) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val providersCheckState by viewModel.providersCheckState.collectAsStateWithLifecycle()

    var queryField by remember { mutableStateOf(TextFieldValue("")) }
    // Focus entry point for the category row. Without an explicit target, DPAD_DOWN
    // from the search field lands on whichever chip is geometrically nearest the
    // field's centre - mid-row, e.g. "Games" - instead of the active category.
    val categoryFocusRequester = remember { FocusRequester() }

    val submit: (String) -> Unit = { raw ->
        val query = raw.trim()
        if (query.isNotEmpty()) {
            viewModel.filterSearchSuggestions("")
            onSearch(query, uiState.selectedCategory)
        }
    }

    // Screen entry point: focus lands on the search field's surface when this
    // route opens. The surface, not the inner text field, so the keyboard stays
    // closed until OK is pressed.
    val entryFocusRequester = LocalTvContentFocusRequester.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = tvListContentPadding(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.medium),
    ) {
        item(key = "search") {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TvTextInput(
                    value = queryField,
                    onValueChange = { field ->
                        queryField = field
                        viewModel.filterSearchSuggestions(field.text)
                    },
                    onSubmit = submit,
                    placeholder = stringResource(R.string.home_search_query_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            entryFocusRequester
                                ?.let { Modifier.focusRequester(it) } ?: Modifier,
                        ),
                    downFocusRequester = categoryFocusRequester,
                )
                // A disabled tv-material Button is still reachable with the D-pad, so
                // rendering one for the blank-query state leaves a dead focus stop -
                // the remote lands on a control that swallows OK. Render an inert
                // surface of identical size instead so the row does not jump but
                // focus skips it cleanly.
                if (queryField.text.isNotBlank()) {
                    TvActionButton(
                        onClick = { submit(queryField.text) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusProperties { down = categoryFocusRequester },
                    ) {
                        Text(text = stringResource(R.string.home_button_search))
                    }
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        colors = SurfaceDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.tvCardSurface,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusProperties { down = categoryFocusRequester },
                    ) {
                        Text(
                            text = stringResource(R.string.home_button_search),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(MaterialTheme.spaces.medium),
                        )
                    }
                }
            }
        }

        item(key = "categories") {
            TvChipRow(
                chips = uiState.categories.map { category ->
                    TvChip(
                        key = category.name,
                        label = category.displayName(),
                        selected = category == uiState.selectedCategory,
                        onClick = { viewModel.setCategory(category) },
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties { down = categoryFocusRequester },
                initialFocusRequester = categoryFocusRequester,
            )
        }

        when (val state = providersCheckState) {
            null -> Unit

            ProvidersCheckState.Checking -> item(key = "providers-checking") {
                ProviderStatusRow(text = stringResource(R.string.home_status_providers_checking))
            }

            ProvidersCheckState.Error -> item(key = "providers-error") {
                ProviderStatusRow(
                    text = stringResource(R.string.home_status_providers_check_failed),
                    actionLabel = stringResource(R.string.button_try_again),
                    onAction = viewModel::checkProviders,
                )
            }

            is ProvidersCheckState.Complete -> item(key = "providers-done") {
                ProviderStatusRow(
                    text = "${stringResource(R.string.home_status_providers_check_complete)} " +
                        "(${state.numUnlockedProviders}/${state.numLockedProviders})",
                    actionLabel = stringResource(R.string.button_done),
                    onAction = viewModel::finishProvidersCheck,
                )
            }
        }

        item(key = "no-providers") {
            EnableProvidersPrompt(
                visible = uiState.settings.providersInitialized == false,
                onEnable = viewModel::enableDefaultSearchProviders,
                onSkip = viewModel::skipDefaultSearchProviders,
            )
        }

        if (uiState.settings.showRecentSearches &&
            uiState.settings.providersInitialized != false
        ) {
            item(key = "recent-header") {
                Text(
                    text = stringResource(R.string.home_title_recent_searches),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = MaterialTheme.spaces.small),
                )
            }
            items(uiState.recentSearches, key = { "recent-$it" }) { recent ->
                RecentSearchRow(text = recent, onClick = { submit(recent) })
            }
        }
    }
}

@Composable
private fun ProviderStatusRow(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (actionLabel != null && onAction != null) {
            TvActionButton(onClick = onAction) { Text(text = actionLabel) }
        }
    }
}

@Composable
private fun EnableProvidersPrompt(
    visible: Boolean,
    onEnable: () -> Unit,
    onSkip: () -> Unit,
) {
    if (!visible) return
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small)) {
        Text(
            text = stringResource(R.string.home_search_providers_not_enabled_msg),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
        ) {
            TvActionButton(onClick = onEnable) {
                Text(text = stringResource(R.string.home_button_enable))
            }
            TvActionButton(onClick = onSkip) {
                Text(text = stringResource(R.string.home_button_i_will_choose_myself))
            }
        }
    }
}

@Composable
private fun RecentSearchRow(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}