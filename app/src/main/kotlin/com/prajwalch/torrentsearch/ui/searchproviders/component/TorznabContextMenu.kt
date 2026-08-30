package com.prajwalch.torrentsearch.ui.searchproviders.component

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.component.RoundedDropdownMenu
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun TorznabContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEditConfiguration: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RoundedDropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(x = MaterialTheme.spaces.large, y = 0.dp),
    ) {
        DropdownMenuItem(
            onClick = onEditConfiguration,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                )
            },
            text = {
                Text(text = stringResource(R.string.search_providers_list_action_edit))
            },
        )
        DropdownMenuItem(
            onClick = onDelete,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                )
            },
            text = {
                Text(text = stringResource(R.string.search_providers_list_action_delete))
            },
        )
    }
}