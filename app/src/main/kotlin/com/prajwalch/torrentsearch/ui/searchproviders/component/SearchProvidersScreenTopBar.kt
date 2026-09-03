package com.prajwalch.torrentsearch.ui.searchproviders.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.component.RoundedDropdownMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchProvidersScreenTopBar(
    onNavigateBack: () -> Unit,
    onToggleSearchBar: () -> Unit,
    onEnableAll: () -> Unit,
    onDisableAll: () -> Unit,
    onUpdateProtectionStatus: () -> Unit,
    onResetToDefault: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: @Composable (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier,
        title = { TopAppBarTitle(subtitle = subtitle) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                )
            }
        },
        actions = {
            IconButton(onClick = onToggleSearchBar) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
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
                    onEnableAll = {
                        onEnableAll()
                        showOverflowMenu = false
                    },
                    onDisableAll = {
                        onDisableAll()
                        showOverflowMenu = false
                    },
                    onUpdateProtectionStatus = {
                        onUpdateProtectionStatus()
                        showOverflowMenu = false
                    },
                    onResetToDefault = {
                        onResetToDefault()
                        showOverflowMenu = false
                    },
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun TopAppBarTitle(
    modifier: Modifier = Modifier,
    subtitle: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        Text(stringResource(R.string.search_providers_screen_title))
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
            LocalTextStyle provides MaterialTheme.typography.labelMedium,
        ) {
            subtitle?.invoke()
        }
    }
}

@Composable
private fun TopBarOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEnableAll: () -> Unit,
    onDisableAll: () -> Unit,
    onUpdateProtectionStatus: () -> Unit,
    onResetToDefault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RoundedDropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_select_all),
                    contentDescription = stringResource(
                        R.string.search_providers_action_enable_all,
                    ),
                )
            },
            text = { Text(stringResource(R.string.search_providers_action_enable_all)) },
            onClick = onEnableAll,
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_deselect_all),
                    contentDescription = stringResource(
                        R.string.search_providers_action_disable_all,
                    ),
                )
            },
            text = { Text(stringResource(R.string.search_providers_action_disable_all)) },
            onClick = onDisableAll,
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_safety_check),
                    contentDescription = stringResource(
                        R.string.search_providers_action_update_protection_status,
                    ),
                )
            },
            text = {
                Text(stringResource(R.string.search_providers_action_update_protection_status))
            },
            onClick = onUpdateProtectionStatus,
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_reset_settings),
                    contentDescription = stringResource(R.string.search_providers_action_reset),
                )
            },
            text = { Text(stringResource(R.string.search_providers_action_reset)) },
            onClick = onResetToDefault,
        )
    }
}