package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prajwalch.torrentsearch.R

@Composable
fun NoInternetConnectionMessage(onRetry: () -> Unit, modifier: Modifier = Modifier.Companion) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.msg_no_internet_connection),
            fontWeight = FontWeight.Companion.Bold
        )
        Spacer(modifier = Modifier.Companion.height(10.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.button_retry)) }
    }
}

@Composable
fun ResultsNotFoundMessage(modifier: Modifier = Modifier.Companion) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier = Modifier.Companion.size(58.dp),
            painter = painterResource(R.drawable.ic_sad),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.Companion.height(8.dp))
        Text(
            text = stringResource(R.string.msg_no_results_found),
            fontWeight = FontWeight.Companion.Bold,
        )
    }
}

@Composable
fun EmptySearchPlaceholder(modifier: Modifier = Modifier.Companion) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Companion.Center,
    ) {
        Column(
            modifier = Modifier.Companion
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.Companion.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                modifier = Modifier.Companion.fillMaxWidth(),
                text = stringResource(R.string.msg_page_empty),
                fontWeight = FontWeight.Companion.SemiBold,
                textAlign = TextAlign.Companion.Center,
            )
            Text(
                modifier = Modifier.Companion.fillMaxWidth(),
                text = stringResource(R.string.msg_start_searching),
                fontWeight = FontWeight.Companion.Normal,
                textAlign = TextAlign.Companion.Center,
            )
        }
    }
}