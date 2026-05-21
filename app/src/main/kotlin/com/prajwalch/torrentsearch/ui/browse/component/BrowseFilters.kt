package com.prajwalch.torrentsearch.ui.browse.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.browse.BrowseSort
import com.prajwalch.torrentsearch.ui.browse.BrowseViewFilters
import com.prajwalch.torrentsearch.ui.categoryStringResource
import com.prajwalch.torrentsearch.ui.component.RoundedDropdownMenu
import com.prajwalch.torrentsearch.ui.iconResId
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.collections.immutable.ImmutableList

@Composable
fun BrowseFilters(
    sort: BrowseSort,
    onChangeSort: (BrowseSort) -> Unit,
    category: Category,
    onChangeCategory: (Category) -> Unit,
    deadTorrents: Boolean,
    onToggleDeadTorrents: () -> Unit,
    hideViewed: Boolean,
    onToggleHideViewed: () -> Unit,
    providerOptions: ImmutableList<BrowseViewFilters.ProviderOption>,
    onToggleSearchProvider: (providerName: String) -> Unit,
    onSelectAllSearchProviders: () -> Unit,
    onDeselectAllSearchProviders: () -> Unit,
    onInvertSearchProvidersSelection: () -> Unit,
    modifier: Modifier = Modifier,
    enableSort: Boolean = true,
    enableCategory: Boolean = true,
    enableDeadTorrents: Boolean = true,
    enableHideViewed: Boolean = true,
    enableSearchProvidersFilter: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val numSelectedSearchProviders = rememberSaveable(providerOptions) {
        providerOptions.count { it.selected }
    }

    var showSearchProvidersFilter by rememberSaveable { mutableStateOf(false) }
    if (showSearchProvidersFilter) {
        FilterByProviderBottomSheet(
            onDismiss = { showSearchProvidersFilter = false },
            filterOptions = providerOptions,
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
        contentPadding = contentPadding,
    ) {
        item(key = "browse_sort", contentType = { sort }) {
            var showBrowseSortMenu by rememberSaveable(sort) { mutableStateOf(false) }

            Box {
                FilterChip(
                    selected = true,
                    onClick = { showBrowseSortMenu = true },
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                            painter = painterResource(sort.iconResId()),
                            contentDescription = null,
                        )
                    },
                    label = { Text(sort.displayName()) },
                    trailingIcon = arrowDownIcon,
                    enabled = enableSort,
                )
                BrowseSortDropdownMenu(
                    expanded = showBrowseSortMenu,
                    onDismiss = { showBrowseSortMenu = false },
                    selectedSort = sort,
                    onSortSelect = onChangeSort,
                )
            }
        }

        item(key = "browse_category", contentType = { category }) {
            val isDefaultCategorySelected = category == Category.All
            var showCategoryMenu by rememberSaveable(category) { mutableStateOf(false) }

            Box {
                FilterChip(
                    selected = !isDefaultCategorySelected,
                    onClick = { showCategoryMenu = true },
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                            painter = painterResource(category.iconResId()),
                            contentDescription = null,
                        )
                    },
                    label = { Text(text = categoryStringResource(category)) },
                    trailingIcon = arrowDownIcon,
                    enabled = enableCategory,
                )
                CategoryDropdownMenu(
                    expanded = showCategoryMenu,
                    onDismiss = { showCategoryMenu = false },
                    selectedCategory = category,
                    onCategorySelect = onChangeCategory,
                )
            }
        }

        item(key = "filters_divider") {
            VerticalDivider(modifier = Modifier.height(FilterChipDefaults.Height / 1.5f))
        }

        item(key = "dead_torrents") {
            FilterChip(
                selected = deadTorrents,
                onClick = onToggleDeadTorrents,
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                        painter = painterResource(R.drawable.ic_heart_broken),
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(R.string.search_filter_chip_dead_torrents)) },
                enabled = enableDeadTorrents,
            )
        }

        item(key = "hide_viewed") {
            FilterChip(
                selected = hideViewed,
                onClick = onToggleHideViewed,
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                        painter = painterResource(R.drawable.ic_visibility_off),
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(R.string.search_filter_chip_hide_viewed)) },
                enabled = enableHideViewed,
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
    }
}

@Composable
private fun BrowseSortDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    selectedSort: BrowseSort,
    onSortSelect: (BrowseSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    RoundedDropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        BrowseSort.entries.forEach {
            DropdownMenuItem(
                onClick = { onSortSelect(it) },
                text = { Text(it.displayName()) },
                trailingIcon = {
                    if (it == selectedSort) {
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

@DrawableRes
@Composable
private fun BrowseSort.iconResId(): Int = when (this) {
    BrowseSort.Latest -> R.drawable.ic_release_alert
    BrowseSort.Top -> R.drawable.ic_trending_up
}

@Composable
private fun BrowseSort.displayName(): String {
    val resId = when (this) {
        BrowseSort.Latest -> R.string.browse_sort_latest
        BrowseSort.Top -> R.string.browse_sort_top
    }
    return stringResource(resId)
}