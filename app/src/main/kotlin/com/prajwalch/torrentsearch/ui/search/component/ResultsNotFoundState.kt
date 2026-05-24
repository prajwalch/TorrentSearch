package com.prajwalch.torrentsearch.ui.search.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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
import com.prajwalch.torrentsearch.ui.component.TryAgainButton

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
        action = { TryAgainButton(onClick = onTryAgain) }
    )
}