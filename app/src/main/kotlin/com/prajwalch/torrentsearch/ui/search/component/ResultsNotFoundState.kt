package com.prajwalch.torrentsearch.ui.search.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.categoryStringResource
import com.prajwalch.torrentsearch.ui.component.ContentState
import com.prajwalch.torrentsearch.ui.component.ContentStateDefaults

//@Composable
//fun ResultsNotFoundState(query: String, modifier: Modifier = Modifier) {
//    ContentState(
//        modifier = modifier,
//        icon = {
//            Icon(
//                modifier = Modifier.size(ContentStateDefaults.IconSize),
//                painter = painterResource(R.drawable.ic_results_not_found),
//                contentDescription = null,
//            )
//        },
//        title = { Text(stringResource(R.string.search_no_results_found_format, query)) },
//    )
//}

@Composable
fun ResultsNotFoundState(
    onNavigateToProviders: () -> Unit,
    onTryAgain: () -> Unit,
    query: String,
    category: Category,
    modifier: Modifier = Modifier,
) {
    ContentState(
        modifier = modifier,
        icon = {
            Icon(
                modifier = Modifier.size(ContentStateDefaults.IconSize),
                painter = painterResource(R.drawable.ic_results_not_found),
                contentDescription = null,
            )
        },
        title = { Text(stringResource(R.string.search_state_results_not_found_title)) },
        description = {
            Text(
                text = stringResource(
                    R.string.search_state_results_not_found_description,
                    query,
                    categoryStringResource(category),
                ),
                textAlign = TextAlign.Center,
            )
        },
        primaryAction = {
            Button(
                onClick = onNavigateToProviders,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = null,
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.search_button_go_to_providers))
            }
        },
        secondaryAction = {
            OutlinedButton(
                onClick = onTryAgain,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = null,
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.button_try_again))
            }
        }
    )
}