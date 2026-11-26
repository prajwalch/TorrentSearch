package com.prajwalch.torrentsearch.ui.search.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.component.ContentState
import com.prajwalch.torrentsearch.ui.component.ContentStateDefaults

@Composable
fun NoExactMatchesState(
    hiddenTorrentsCount: Int,
    onShowAllResults: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ContentState(
        modifier = modifier,
        icon = {
            Icon(
                modifier = Modifier.size(ContentStateDefaults.IconSize),
                painter = painterResource(R.drawable.ic_search_off),
                contentDescription = null,
            )
        },
        title = { Text(stringResource(R.string.search_state_no_exact_matches_title)) },
        primaryAction = {
            Button(
                onClick = onShowAllResults,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    painter = painterResource(R.drawable.ic_visibility),
                    contentDescription = null,
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(
                    pluralStringResource(
                        R.plurals.search_button_show_all_results,
                        hiddenTorrentsCount,
                        hiddenTorrentsCount,
                    )
                )
            }
        },
    )
}
