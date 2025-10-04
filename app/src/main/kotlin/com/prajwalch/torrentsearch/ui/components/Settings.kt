package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun SettingsSectionTitle(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    style: TextStyle = MaterialTheme.typography.titleSmall,
) {
    Text(
        modifier = modifier.padding(MaterialTheme.spaces.large),
        text = stringResource(title),
        color = color,
        style = style,
    )
}

@Composable
fun SettingsListItem(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    @StringRes headline: Int,
    modifier: Modifier = Modifier,
    supportingContent: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(modifier),
        leadingContent = {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(headline),
            )
        },
        headlineContent = { Text(text = stringResource(headline)) },
        supportingContent = supportingContent?.let { { Text(text = it) } },
        trailingContent = trailingContent,
    )
}

@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit,
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    confirmButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.settings_dialog_button_cancel))
            }
        },
        confirmButton = confirmButton ?: {},
        title = { Text(text = stringResource(title)) },
        text = content,
    )
}