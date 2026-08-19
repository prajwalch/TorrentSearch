package com.prajwalch.torrentsearch.ui.search.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.SearchProviderError
import com.prajwalch.torrentsearch.ui.component.BottomInfo
import com.prajwalch.torrentsearch.ui.component.StackTraceCard
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchErrorsBottomSheet(
    onDismiss: () -> Unit,
    errors: ImmutableList<SearchProviderError>,
    modifier: Modifier = Modifier,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large),
                text = stringResource(R.string.search_errors_bottom_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spaces.small))
            HorizontalDivider()
            SearchProviderErrorList(
                modifier = Modifier.weight(1f),
                errors = errors,
                contentPadding = PaddingValues(MaterialTheme.spaces.large),
            )
            HorizontalDivider()
            BottomInfo(modifier = Modifier.padding(MaterialTheme.spaces.large)) {
                Text(text = stringResource(R.string.search_info_troubleshoot_help))
            }
        }
    }
}

@Composable
private fun SearchProviderErrorList(
    errors: ImmutableList<SearchProviderError>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.spaces.small,
        ),
        contentPadding = contentPadding,
    ) {
        items(items = errors) {
            SearchProviderErrorCard(
                modifier = Modifier.animateItem(),
                error = it,
            )
        }
    }
}

@Composable
private fun SearchProviderErrorCard(error: SearchProviderError, modifier: Modifier = Modifier) {
    var showStackTrace by rememberSaveable { mutableStateOf(false) }
    val chevronIconRotation by animateFloatAsState(if (showStackTrace) 180f else 0f)

    Card(modifier = modifier, shape = MaterialTheme.shapes.large) {
        ListItem(
            modifier = Modifier.clickable { showStackTrace = !showStackTrace },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_error),
                    contentDescription = null,
                )
            },
            headlineContent = { Text(error.providerName) },
            supportingContent = {
                Text(
                    text = error.message ?: stringResource(R.string.search_unexpected_error),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            trailingContent = {
                Icon(
                    modifier = Modifier.rotate(chevronIconRotation),
                    painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                    contentDescription = null,
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = CardDefaults.cardColors().containerColor,
                leadingIconColor = MaterialTheme.colorScheme.error,
                supportingColor = MaterialTheme.colorScheme.error,
            ),
        )

        AnimatedVisibility(showStackTrace) {
            StackTraceSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spaces.large),
                stackTrace = error.cause?.stackTraceToString(),
//                onCopyStackTrace = {},
            )
        }
    }
}

@Composable
private fun StackTraceSection(
    stackTrace: String?,
//    onCopyStackTrace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_data_object),
                contentDescription = null,
            )
            Text(stringResource(R.string.search_title_stack_trace))
        }

//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(IntrinsicSize.Max)
//        ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
        ) {
            StackTraceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                stackTrace = stackTrace
                    ?: stringResource(R.string.search_message_no_stack_trace),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }

//            FilledTonalIconButton(
//                modifier = Modifier
//                    .align(Alignment.TopEnd)
//                    .padding(MaterialTheme.spaces.small),
//                onClick = onCopyStackTrace,
//                enabled = stackTrace != null,
//            ) {
//                Icon(
//                    painter = painterResource(R.drawable.ic_copy),
//                    contentDescription = null,
//                )
//            }
//        }
    }
}

@Preview
@Composable
private fun SearchProviderErrorCardPreview() {
    TorrentSearchTheme {
        SearchProviderErrorCard(
            error = SearchProviderError(
                providerName = "TokyoToshokan",
                providerUrl = "https://example.com",
                null, null,
            ),
        )
    }
}