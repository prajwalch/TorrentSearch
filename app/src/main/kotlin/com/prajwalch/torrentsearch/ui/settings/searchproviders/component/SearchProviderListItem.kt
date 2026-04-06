package com.prajwalch.torrentsearch.ui.settings.searchproviders.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.SearchProviderType
import com.prajwalch.torrentsearch.ui.component.BadgesRow
import com.prajwalch.torrentsearch.ui.component.CategoryBadge
import com.prajwalch.torrentsearch.ui.component.TorznabBadge
import com.prajwalch.torrentsearch.ui.component.UnsafeBadge
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun SearchProviderListItem(
    name: String,
    url: String,
    category: Category,
    type: SearchProviderType,
    safetyStatus: SearchProviderSafetyStatus,
    enabled: Boolean,
    onEnable: (Boolean) -> Unit,
    onEditConfig: () -> Unit,
    onDeleteConfig: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showUnsafeReason by rememberSaveable { mutableStateOf<Int?>(null) }
    showUnsafeReason?.let { reasonResId ->
        SearchProviderUnsafeDetailsDialog(
            onDismissRequest = { showUnsafeReason = null },
            providerName = name,
            url = url,
            unsafeReason = stringResource(reasonResId),
        )
    }

    var showTorznabContextMenu by rememberSaveable { mutableStateOf(false) }

    // Long click handler for showing Torznab context menu.
    val longClickHandler: (() -> Unit)? = when (type) {
        // Disable it for builtin providers.
        SearchProviderType.Builtin -> null
        SearchProviderType.Torznab -> ({ showTorznabContextMenu = true })
    }
    val clickableModifier = Modifier.combinedClickable(
        interactionSource = null,
        indication = LocalIndication.current,
        onClick = { onEnable(!enabled) },
        onLongClick = longClickHandler,
    )

    Box(modifier = modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier.then(clickableModifier),
            headlineContent = { Text(text = name) },
            supportingContent = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        space = MaterialTheme.spaces.extraSmall,
                        alignment = Alignment.CenterVertically,
                    ),
                ) {
                    SearchProviderUrl(url = url)
                    BadgesRow {
                        CategoryBadge(category)
                        if (type == SearchProviderType.Torznab) TorznabBadge()
                        if (safetyStatus.isUnsafe()) UnsafeBadge()
                    }
                }
            },
            trailingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val isBuiltinProvider = (type == SearchProviderType.Builtin)
                    val isUnsafe = safetyStatus is SearchProviderSafetyStatus.Unsafe

                    if (isBuiltinProvider && isUnsafe) {
                        QuestionMarkButton(onClick = { showUnsafeReason = safetyStatus.reason })
                    }
                    Switch(checked = enabled, onCheckedChange = onEnable)
                }
            },
        )

        TorznabContextMenu(
            expanded = showTorznabContextMenu,
            onDismiss = { showTorznabContextMenu = false },
            onEditConfiguration = {
                onEditConfig()
                showTorznabContextMenu = false
            },
            onDelete = {
                onDeleteConfig()
                showTorznabContextMenu = false
            },
        )
    }
}

@Composable
private fun QuestionMarkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalIconButton(modifier = modifier, onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_question_mark),
            contentDescription = null,
        )
    }
}