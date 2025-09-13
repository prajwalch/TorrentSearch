package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun EmptyPlaceholder(
    @StringRes headlineId: Int,
    modifier: Modifier = Modifier,
    @StringRes supportingTextId: Int? = null,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = MaterialTheme.spaces.small,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            Text(
                text = stringResource(headlineId),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            supportingTextId?.let {
                Text(
                    text = stringResource(it),
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}