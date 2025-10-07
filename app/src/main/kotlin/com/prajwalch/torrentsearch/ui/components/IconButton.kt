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
fun ArrowBackIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes contentDescription: Int? = null,
) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = contentDescription?.let { stringResource(it) },
        )
    }
}

@Composable
fun DeleteForeverIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes contentDescription: Int? = null,
) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_delete_forever),
            contentDescription = contentDescription?.let { stringResource(it) },
        )
    }
}

@Composable
fun SearchIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
        )
    }
}

@Composable
fun SortIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_sort),
            contentDescription = stringResource(R.string.action_sort),
        )
    }
}

@Composable
fun SettingsIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = null,
        )
    }
}