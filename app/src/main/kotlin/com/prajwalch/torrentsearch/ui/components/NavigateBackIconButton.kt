package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.prajwalch.torrentsearch.R

@Composable
fun NavigateBackIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes contentDescriptionId: Int? = null,
) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = contentDescriptionId?.let { stringResource(it) },
        )
    }
}