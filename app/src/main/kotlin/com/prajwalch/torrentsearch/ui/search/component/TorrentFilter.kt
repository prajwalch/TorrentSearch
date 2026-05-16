package com.prajwalch.torrentsearch.ui.search.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.categoryStringResource
import com.prajwalch.torrentsearch.ui.component.RoundedDropdownMenu
import com.prajwalch.torrentsearch.ui.iconResId
import com.prajwalch.torrentsearch.ui.search.TorrentFilter
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun TorrentFilter(
    filter: TorrentFilter,
    onToggleDeadTorrents: () -> Unit,
    onToggleHideViewed: () -> Unit,
    onToggleSearchProvider: (providerName: String) -> Unit,
    onSelectAllSearchProviders: () -> Unit,
    onDeselectAllSearchProviders: () -> Unit,
    onInvertSearchProvidersSelection: () -> Unit,
    onUpdateCategory: (Category) -> Unit,
    modifier: Modifier = Modifier,
    enableDeadTorrentsFilter: Boolean = true,
    enableSearchProvidersFilter: Boolean = true,
    enableCategoryFilter: Boolean = true,
) {
    val numSelectedSearchProviders = rememberSaveable(filter.providers) {
        filter.providers.count { it.selected }
    }

    var showSearchProvidersFilter by rememberSaveable { mutableStateOf(false) }
    if (showSearchProvidersFilter) {
        SearchProvidersFilterBottomSheet(
            onDismiss = { showSearchProvidersFilter = false },
            filterOptions = filter.providers,
            onToggleSearchProvider = onToggleSearchProvider,
            onSelectAll = onSelectAllSearchProviders,
            onDeselectAll = onDeselectAllSearchProviders,
            onInvertSelection = onInvertSearchProvidersSelection,
        )
    }

    val arrowDownIcon: @Composable () -> Unit = @Composable {
        Icon(
            modifier = Modifier.size(FilterChipDefaults.IconSize),
            painter = painterResource(R.drawable.ic_keyboard_arrow_down),
            contentDescription = null,
        )
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.spaces.small,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = MaterialTheme.spaces.large),
    ) {
        item(key = "dead_torrents") {
            FilterChip(
                selected = filter.showDeadTorrents,
                onClick = onToggleDeadTorrents,
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                        painter = painterResource(R.drawable.ic_heart_broken),
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(R.string.search_filter_chip_dead_torrents)) },
                enabled = enableDeadTorrentsFilter,
            )
        }

        item(key = "hide_viewed") {
            FilterChip(
                selected = filter.hideViewed,
                onClick = onToggleHideViewed,
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                        painter = painterResource(R.drawable.ic_visibility_off),
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(R.string.search_filter_chip_hide_viewed)) },
            )
        }

        item(key = "search_providers") {
            val selected = numSelectedSearchProviders > 0
            val label = stringResource(R.string.search_filter_chip_search_providers).let {
                if (selected) "$it ($numSelectedSearchProviders)" else it
            }

            FilterChip(
                modifier = Modifier.animateItem(),
                selected = selected,
                onClick = { showSearchProvidersFilter = true },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                        painter = painterResource(R.drawable.ic_travel_explore),
                        contentDescription = null,
                    )
                },
                label = { Text(text = label) },
                trailingIcon = arrowDownIcon,
                enabled = enableSearchProvidersFilter,
            )
        }

        item(key = "category", contentType = filter.category) {
            val isDefaultCategorySelected = filter.category == Category.All
            var showCategoryOptions by rememberSaveable(filter.category) {
                mutableStateOf(false)
            }

            Box {
                FilterChip(
                    selected = !isDefaultCategorySelected,
                    onClick = { showCategoryOptions = true },
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                            painter = painterResource(filter.category.iconResId()),
                            contentDescription = null,
                        )
                    },
                    label = { Text(text = categoryStringResource(filter.category)) },
                    trailingIcon = arrowDownIcon,
                    enabled = enableCategoryFilter,
                )
                CategoryOptionsDropdownMenu(
                    expanded = showCategoryOptions,
                    onDismiss = { showCategoryOptions = false },
                    selectedCategory = filter.category,
                    onCategorySelect = onUpdateCategory,
                )
            }
        }
    }
}

@Composable
private fun CategoryOptionsDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    selectedCategory: Category,
    onCategorySelect: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    RoundedDropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        Category.entries.forEach {
            DropdownMenuItem(
                text = { Text(text = categoryStringResource(it)) },
                onClick = { onCategorySelect(it) },
                trailingIcon = {
                    if (it == selectedCategory) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                        )
                    }
                },
            )
        }
    }
}