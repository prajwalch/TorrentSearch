package com.prajwalch.torrentsearch.ui.search.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.search.TorrentFilter
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchProvidersFilterBottomSheet(
    onDismiss: () -> Unit,
    filterOptions: ImmutableList<TorrentFilter.SearchProviderOption>,
    onToggleSearchProvider: (providerName: String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(modifier = modifier, onDismissRequest = onDismiss) {
        BottomSheetContent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spaces.large),
            filterOptions = filterOptions,
            onToggleSearchProvider = onToggleSearchProvider,
            onSelectAll = onSelectAll,
            onDeselectAll = onDeselectAll,
            onInvertSelection = onInvertSelection,
        )
    }
}

@Composable
private fun BottomSheetContent(
    filterOptions: ImmutableList<TorrentFilter.SearchProviderOption>,
    onToggleSearchProvider: (providerName: String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchProvidersFilterTitle(modifier = Modifier.weight(1f))
            SearchProvidersFilterActions(
                onSelectAll = onSelectAll,
                onDeselectAll = onDeselectAll,
                onInvertSelection = onInvertSelection,
            )
        }

        SearchProvidersChipRow(
            modifier = Modifier.padding(vertical = MaterialTheme.spaces.large),
            filterOptions = filterOptions,
            onToggleSearchProvider = onToggleSearchProvider,
        )
    }
}

@Composable
private fun SearchProvidersFilterTitle(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(R.string.search_filter_providers_title),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun SearchProvidersFilterActions(
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButtonWithTooltip(
            onClick = onSelectAll,
            painter = painterResource(R.drawable.ic_select_all),
            contentDescription = stringResource(
                R.string.search_filter_providers_action_select_all,
            ),
        )
        IconButtonWithTooltip(
            onClick = onDeselectAll,
            painter = painterResource(R.drawable.ic_deselect_all),
            contentDescription = stringResource(
                R.string.search_filter_providers_action_deselect_all,
            ),
        )
        IconButtonWithTooltip(
            onClick = onInvertSelection,
            painter = painterResource(R.drawable.ic_flip),
            contentDescription = stringResource(
                R.string.search_filter_providers_action_invert_selection,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconButtonWithTooltip(
    onClick: () -> Unit,
    painter: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val tooltipPositionProvider =
        TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above)

    TooltipBox(
        modifier = modifier,
        positionProvider = tooltipPositionProvider,
        tooltip = { PlainTooltip { Text(text = contentDescription) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
            )
        }
    }
}

@Composable
private fun SearchProvidersChipRow(
    filterOptions: ImmutableList<TorrentFilter.SearchProviderOption>,
    onToggleSearchProvider: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        filterOptions.forEach {
            FilterChip(
                selected = it.selected,
                onClick = { onToggleSearchProvider(it.provider) },
                label = { Text(text = it.provider) },
            )
        }
    }
}