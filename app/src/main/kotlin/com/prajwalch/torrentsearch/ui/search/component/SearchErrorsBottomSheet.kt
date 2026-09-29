package com.prajwalch.torrentsearch.ui.search.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.Alignment
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
import com.prajwalch.torrentsearch.ui.search.ErrorItem
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchErrorsBottomSheet(
    onDismiss: () -> Unit,
    errorItems: ImmutableList<ErrorItem>,
    onRetryError: (ErrorItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
    ) {
        BottomSheetContent(
            modifier = Modifier.fillMaxSize(),
            errorItems = errorItems,
            onRetryError = onRetryError,
        )
    }
}

@Composable
private fun BottomSheetContent(
    errorItems: ImmutableList<ErrorItem>,
    onRetryError: (ErrorItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small)) {
            Text(
                modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large),
                text = stringResource(R.string.search_errors_bottom_sheet_title),
                style = MaterialTheme.typography.titleLarge,
            )
            HorizontalDivider()
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
            contentPadding = PaddingValues(MaterialTheme.spaces.large),
        ) {
            items(
                items = errorItems,
                key = { it.providerError.providerId },
                contentType = { it.providerError.kind },
            ) {
                ErrorItemCard(
                    modifier = Modifier.animateItem(),
                    errorItem = it,
                    onRetryError = { onRetryError(it) },
                )
            }
        }

        Column {
            HorizontalDivider()
            BottomInfo(modifier = Modifier.padding(MaterialTheme.spaces.large)) {
                Text(text = stringResource(R.string.search_info_troubleshoot_help))
            }
        }
    }
}

@Composable
private fun ErrorItemCard(
    errorItem: ErrorItem,
    onRetryError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showStackTrace by rememberSaveable { mutableStateOf(false) }
    val chevronIconRotation by animateFloatAsState(if (showStackTrace) 180f else 0f)
    val enableRetryButton = errorItem.providerError.isRetryable &&
            errorItem.state == ErrorItem.State.Active

    Card(modifier = modifier, shape = MaterialTheme.shapes.large) {
        ListItem(
            modifier = Modifier.clickable { showStackTrace = !showStackTrace },
            leadingContent = {
                Crossfade(errorItem) { targetErrorItem ->
                    when (targetErrorItem.state) {
                        ErrorItem.State.Active -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_error),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }

                        ErrorItem.State.Retrying -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                trackColor = MaterialTheme.colorScheme.primaryContainer,
                                strokeWidth = 2.0.dp,
                            )
                        }

                        ErrorItem.State.Resolved -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_circle),
                                contentDescription = null,
                            )
                        }
                    }
                }
            },
            headlineContent = { Text(errorItem.providerError.providerName) },
            supportingContent = {
                Crossfade(errorItem) { targetErrorItem ->
                    Text(
                        text = targetErrorItem.supportingText(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (targetErrorItem.state == ErrorItem.State.Active) {
                            MaterialTheme.colorScheme.error
                        } else {
                            LocalContentColor.current
                        },
                    )
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRetryError, enabled = enableRetryButton) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = null,
                        )
                    }

                    Icon(
                        modifier = Modifier.rotate(chevronIconRotation),
                        painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                        contentDescription = null,
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = CardDefaults.cardColors().containerColor,
                leadingIconColor = MaterialTheme.colorScheme.primary,
            ),
        )

        AnimatedVisibility(showStackTrace) {
            StackTraceSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spaces.large),
                stackTrace = errorItem.providerError.cause?.stackTraceToString(),
            )
        }
    }
}

@Composable
private fun ErrorItem.supportingText(): String {
    val resId = when (this.state) {
        ErrorItem.State.Active -> when (this.providerError.kind) {
            SearchProviderError.Kind.Crash -> R.string.search_error_kind_crash
            SearchProviderError.Kind.CloudflareChallenge -> {
                R.string.search_error_kind_cloudflare_challenge
            }
        }

        ErrorItem.State.Retrying -> R.string.search_error_status_retrying
        ErrorItem.State.Resolved -> R.string.search_error_status_resolved
    }

    return stringResource(resId)
}

@Composable
private fun StackTraceSection(stackTrace: String?, modifier: Modifier = Modifier) {
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

        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
        ) {
            StackTraceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                stackTrace = stackTrace
                    ?: stringResource(R.string.search_message_no_stack_trace),
            )
        }
    }
}

@Preview
@Composable
private fun ErrorItemCardPreview() {
    TorrentSearchTheme {
        ErrorItemCard(
            errorItem = ErrorItem(
                state = ErrorItem.State.Active,
                providerError = SearchProviderError(
                    providerId = "exampleprovider",
                    providerName = "TokyoToshokan",
                    providerUrl = "https://example.com",
                    kind = SearchProviderError.Kind.CloudflareChallenge,
                    cause = null,
                )
            ),
            onRetryError = {},
        )
    }
}