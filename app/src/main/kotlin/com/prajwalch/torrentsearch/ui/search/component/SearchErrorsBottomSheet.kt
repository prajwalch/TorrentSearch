package com.prajwalch.torrentsearch.ui.search.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.SearchProviderError
import com.prajwalch.torrentsearch.ui.component.BottomInfo
import com.prajwalch.torrentsearch.ui.component.StackTraceCard
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

//@Preview
//@Composable
//private fun SearchErrorsBottomSheetPreview() {
//    val failures = persistentListOf(
//        SearchProviderError(
//            providerName = "ThePirateBay",
//            searchUrl = "https://example.com",
//            null, null,
//        ),
//        SearchProviderError(
//            providerName = "TheRarBg",
//            searchUrl = "https://example.com",
//            null, null,
//        ),
//        SearchProviderError(
//            providerName = "TorrentDownloads",
//            searchUrl = "https://example.com",
//            null, null,
//        ),
//        SearchProviderError(
//            providerName = "TokyoToshokan",
//            searchUrl = "https://example.com",
//            null, null,
//        ),
//    )
//
//    TorrentSearchTheme(darkTheme = true) {
//        SearchErrorsBottomSheet(
//            onDismiss = {},
//            errors = failures,
//        )
//    }
//}

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
            SearchProviderErrorListItem(
                modifier = Modifier.animateItem(),
                error = it,
            )
        }
    }
}

@Composable
private fun SearchProviderErrorListItem(
    error: SearchProviderError,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
    ) {
        var showStackTrace by rememberSaveable { mutableStateOf(false) }

        val exceptionMessage = error.message ?: stringResource(R.string.search_unexpected_error)
        val listItemColors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            supportingColor = MaterialTheme.colorScheme.error,
        )

        ListItem(
            modifier = Modifier
                .clip(shape = MaterialTheme.shapes.medium)
                .clickable { showStackTrace = !showStackTrace },
            headlineContent = { Text(text = error.providerName) },
            supportingContent = { Text(text = exceptionMessage) },
            trailingContent = {
                AnimatedContent(targetState = showStackTrace) { stackTraceVisible ->
                    val iconId = if (stackTraceVisible) {
                        R.drawable.ic_keyboard_arrow_up
                    } else {
                        R.drawable.ic_keyboard_arrow_down
                    }
                    Icon(
                        painter = painterResource(iconId),
                        contentDescription = null,
                    )
                }
            },
            colors = listItemColors,
        )

        AnimatedVisibility(visible = showStackTrace) {
            StackTraceCard(
                modifier = Modifier.height(360.dp),
                stackTrace = error.cause?.stackTraceToString() ?: "",
            )
        }
    }
}