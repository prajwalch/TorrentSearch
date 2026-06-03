package com.prajwalch.torrentsearch.ui.settings.searchproviders.component

import android.content.res.Configuration

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.CloudflareProtectionStatus
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.SearchProviderType
import com.prajwalch.torrentsearch.ui.component.BadgeRow
import com.prajwalch.torrentsearch.ui.component.CategoryBadge
import com.prajwalch.torrentsearch.ui.component.TextUrl
import com.prajwalch.torrentsearch.ui.component.TorznabBadge
import com.prajwalch.torrentsearch.ui.component.UnsafeBadge
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun SearchProviderListItem(
    name: String,
    url: String,
    supportedCategories: Set<Category>,
    type: SearchProviderType,
    safetyStatus: SearchProviderSafetyStatus,
    protectionStatus: CloudflareProtectionStatus,
    enabled: Boolean,
    onEnable: (Boolean) -> Unit,
    onUnlockProtection: () -> Unit,
    onEditConfig: () -> Unit,
    onDeleteConfig: () -> Unit,
    onShowUnsafeReason: (resId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
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
    val containerColor = MaterialTheme.colorScheme.surfaceContainer

    Box(modifier = modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier
                .clip(shape = MaterialTheme.shapes.large)
                .then(clickableModifier),
            headlineContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.extraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(name)
                    SearchProviderProtectionStatus(protectionStatus)
                }
            },
            supportingContent = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        space = MaterialTheme.spaces.extraSmall,
                        alignment = Alignment.CenterVertically,
                    ),
                ) {
                    SearchProviderUrl(url = url)
                    SupportedCategories(
                        categories = supportedCategories,
                        containerColor = containerColor,
                    )
                    BadgeRow {
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
                        QuestionMarkButton(onClick = { onShowUnsafeReason(safetyStatus.reason) })
                    }

                    AnimatedContent(protectionStatus) { protectionStatus ->
                        if (protectionStatus == CloudflareProtectionStatus.Locked) {
                            IconButton(onClick = onUnlockProtection) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lock_open),
                                    contentDescription = null,
                                )
                            }
                        } else {
                            Switch(checked = enabled, onCheckedChange = onEnable)
                        }
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = containerColor),
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
private fun SearchProviderProtectionStatus(
    protectionStatus: CloudflareProtectionStatus,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(modifier = modifier, targetState = protectionStatus) { status ->
        when (status) {
            CloudflareProtectionStatus.Locked -> {
                Icon(
                    painter = painterResource(R.drawable.ic_shield_lock),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }

            CloudflareProtectionStatus.Unlocked -> {
                Icon(
                    painter = painterResource(R.drawable.ic_shield_checked),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }

            else -> {}
        }
    }
}

@Composable
private fun SearchProviderUrl(url: String, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val isHttps = url.startsWith("https://")

    if (isHttps) {
        TextUrl(
            modifier = modifier,
            text = url.removePrefix("https://"),
            onClick = { uriHandler.openUri(url) },
        )
    } else {
        Text(
            modifier = modifier,
            text = url,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
        )
    }
}

@Composable
private fun SupportedCategories(
    categories: Set<Category>,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    val hasOverflowed = categories.size > 5
    val fadeOutWidth = 32.dp
    val fadeOutGradient = Brush.horizontalGradient(
        colors = listOf(Color.Transparent, containerColor),
    )

    // TODO: For maximum flexibility, use WindowSizeClass.
    val currentOrientation = LocalConfiguration.current.orientation
    val isPortraitMode = currentOrientation == Configuration.ORIENTATION_PORTRAIT
    val showFadeOut = if (isPortraitMode) hasOverflowed else false

    Box(modifier = modifier.height(IntrinsicSize.Max)) {
        BadgeRow(Modifier.horizontalScroll(rememberScrollState())) {
            categories.forEach { CategoryBadge(it) }

            if (showFadeOut) {
                Spacer(Modifier.width(fadeOutWidth))
            }
        }

        if (showFadeOut) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(fadeOutWidth)
                    .fillMaxHeight()
                    .background(fadeOutGradient),
            )
        }
    }
}

@Composable
private fun QuestionMarkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.error,
) {
    IconButton(
        modifier = modifier,
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = contentColor,
        ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_question_mark),
            contentDescription = null,
        )
    }
}

@Preview
@Composable
private fun SearchProviderListItemPreview() {
    SearchProviderListItem(
        name = "ThePirateBay",
        url = "https://thepiratebay.org",
        supportedCategories = Category.entries.toSet(),
        type = SearchProviderType.Builtin,
        safetyStatus = SearchProviderSafetyStatus.Unsafe(R.string.tpb_unsafe_reason),
        protectionStatus = CloudflareProtectionStatus.UnProtected,
        enabled = true,
        onEnable = {},
        onUnlockProtection = {},
        onEditConfig = {},
        onDeleteConfig = {},
        onShowUnsafeReason = {},
    )
}