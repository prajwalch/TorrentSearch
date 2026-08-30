package com.prajwalch.torrentsearch.ui.bookmarks.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.SortCriteria
import com.prajwalch.torrentsearch.domain.model.SortOptions
import com.prajwalch.torrentsearch.domain.model.SortOrder
import com.prajwalch.torrentsearch.ui.component.CollapsibleSearchBar
import com.prajwalch.torrentsearch.ui.component.RoundedDropdownMenu
import com.prajwalch.torrentsearch.ui.component.SortDropdownMenu
import com.prajwalch.torrentsearch.ui.component.rememberCollapsibleSearchBarState
import com.prajwalch.torrentsearch.ui.theme.spaces

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreenTopBar(
    onNavigateBack: () -> Unit,
    bookmarksCount: Int,
    onFilterBookmarks: (String) -> Unit,
    sortOptions: SortOptions,
    onChangeSortCriteria: (SortCriteria) -> Unit,
    onChangeSortOrder: (SortOrder) -> Unit,
    onDeleteAllBookmarks: () -> Unit,
    onImportBookmarks: () -> Unit,
    onExportBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val searchBarState = rememberCollapsibleSearchBarState(visibleOnInitial = false)
    var showSortMenu by rememberSaveable(sortOptions) { mutableStateOf(false) }
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }

    val topBarTitle: @Composable () -> Unit = @Composable {
        CollapsibleSearchBar(
            state = searchBarState,
            onQueryChange = onFilterBookmarks,
            placeholder = { Text(text = stringResource(R.string.bookmarks_search_query_hint)) },
        )

        if (!searchBarState.isVisible) {
            Column(verticalArrangement = Arrangement.Center) {
                Text(text = stringResource(R.string.bookmarks_screen_title))
                // Subtitle.
                AnimatedVisibility(visible = bookmarksCount > 0) {
                    BookmarksCount(
                        totalBookmarksCount = bookmarksCount,
                        currentBookmarksCount = bookmarksCount,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }

    TopAppBar(
        modifier = modifier,
        title = topBarTitle,
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                )
            }
        },
        actions = {
            val isBookmarksEmpty = bookmarksCount == 0
            val enableBookmarkActions = !isBookmarksEmpty || !searchBarState.isTextBlank

            if (!searchBarState.isVisible) {
                IconButton(
                    onClick = { searchBarState.showSearchBar() },
                    enabled = enableBookmarkActions,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                    )
                }
                IconButton(
                    onClick = { showSortMenu = true },
                    enabled = enableBookmarkActions,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sort),
                        contentDescription = stringResource(R.string.action_sort),
                    )
                }
                SortDropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    currentCriteria = sortOptions.criteria,
                    onChangeCriteria = onChangeSortCriteria,
                    currentOrder = sortOptions.order,
                    onChangeOrder = onChangeSortOrder,
                )
            }

            Box {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = null,
                    )
                }
                TopBarOverflowMenu(
                    expanded = showOverflowMenu,
                    onDismiss = { showOverflowMenu = false },
                    onImportBookmarks = onImportBookmarks,
                    onExportBookmarks = onExportBookmarks,
                    onDeleteAllBookmarks = onDeleteAllBookmarks,
                    onNavigateToSettings = onNavigateToSettings,
                    enableDeleteAllAction = enableBookmarkActions,
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun TopBarOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onImportBookmarks: () -> Unit,
    onExportBookmarks: () -> Unit,
    onDeleteAllBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    enableDeleteAllAction: Boolean = true,
) {
    fun actionWithDismiss(action: () -> Unit): () -> Unit = {
        action()
        onDismiss()
    }

    RoundedDropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bookmarks_action_import)) },
            onClick = actionWithDismiss(onImportBookmarks),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_download),
                    contentDescription = stringResource(R.string.bookmarks_action_import),
                )
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bookmarks_action_export)) },
            onClick = actionWithDismiss(onExportBookmarks),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_upload),
                    contentDescription = stringResource(R.string.bookmarks_action_export),
                )
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bookmarks_action_delete_all)) },
            onClick = actionWithDismiss(onDeleteAllBookmarks),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_sweep),
                    contentDescription = stringResource(R.string.bookmarks_action_delete_all),
                )
            },
            enabled = enableDeleteAllAction,
            colors = MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.error,
                leadingIconColor = MaterialTheme.colorScheme.error,
            ),
        )

        Spacer(Modifier.height(MaterialTheme.spaces.small))
        HorizontalDivider(Modifier.padding(MenuDefaults.DropdownMenuItemContentPadding))
        Spacer(Modifier.height(MaterialTheme.spaces.small))

        DropdownMenuItem(
            text = { Text(stringResource(R.string.bookmarks_action_settings)) },
            onClick = actionWithDismiss(onNavigateToSettings),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.bookmarks_action_settings),
                )
            },
        )
    }
}