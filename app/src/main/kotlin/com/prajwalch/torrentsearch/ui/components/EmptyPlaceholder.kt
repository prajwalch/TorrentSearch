package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun EmptyPlaceholder(
    @StringRes headlineTextId: Int,
    modifier: Modifier = Modifier,
    @DrawableRes overlineIconId: Int? = null,
    @StringRes supportingTextId: Int? = null,
    actions: @Composable (ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.spaces.large,
            alignment = Alignment.CenterVertically,
        )
    ) {
        overlineIconId?.let {
            Icon(
                modifier = Modifier.size(80.dp),
                painter = painterResource(it),
                contentDescription = null,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = MaterialTheme.spaces.small,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            Text(
                text = stringResource(headlineTextId),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
            )

            supportingTextId?.let {
                Text(text = stringResource(it))
            }
        }

        actions?.let {
            it()
        }
    }
}