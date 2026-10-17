package com.prajwalch.torrentsearch.ui.tv.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.ui.extension.toRelativeTimeSpanString
import com.prajwalch.torrentsearch.ui.tv.theme.spaces
import androidx.compose.ui.res.stringResource
import com.prajwalch.torrentsearch.ui.tv.theme.tvCardSurface
import com.prajwalch.torrentsearch.ui.tv.theme.tvElevatedSurface

/**
 * Overscan-safe content padding.
 *
 * Android TV panels are not guaranteed to display the full signal, and Google TV
 * shells frequently reserve the outer few percent for the launcher chrome. The
 * Android TV design guidelines call for interactive elements to sit at least
 * 5-8% inside the edges, so every screen body uses this instead of raw padding.
 */
@Composable
fun tvOverscanPadding(horizontal: androidx.compose.ui.unit.Dp = 48.dp): PaddingValues =
    PaddingValues(
        start = horizontal,
        end = horizontal,
        top = 24.dp,
        bottom = 24.dp,
    )

/**
 * A single focusable torrent row.
 *
 * Flat card rather than the handheld `ListItem`: on a 10-foot surface a filled
 * surface with a clear focus border is far easier to track than a list row, and
 * it gives the focus scale somewhere visible to act on. The whole row is one
 * focus target - the handheld version nests trailing `IconButton`s which become
 * focus traps on a D-pad.
 */
@Composable
fun TvTorrentCard(
    torrent: Torrent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The focus ring is a foundation `Modifier.border` (via tvFocusable) rather than
    // tv-material's `border` role: that role did not render on these cards, so focus
    // on a results row was signalled by nothing but a faint fill change - the "I
    // cannot tell what I have picked" symptom. tvFocusable also supplies the scale
    // and binds DPAD_CENTER to onClick.
    Surface(
        shape = MaterialTheme.shapes.large,
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.tvCardSurface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable(shape = MaterialTheme.shapes.large, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spaces.medium,
                vertical = MaterialTheme.spaces.small,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = torrent.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = torrent.providerName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                torrent.category?.let { category ->
                    Text(
                        text = "· ${category.displayName()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (torrent.isNSFW) {
                    TvBadge(
                        text = stringResource(R.string.nsfw),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                if (torrent.isDead) {
                    TvBadge(
                        text = stringResource(R.string.torrent_badge_dead),
                        containerColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TorrentMetaText(torrent.size)
                TorrentMetaText(torrent.seeders?.let { "↑ $it" })
                TorrentMetaText(torrent.peers?.let { "↓ $it" })
                TorrentMetaText(torrent.uploadDate?.toRelativeTimeSpanString())
            }
        }
    }
}

@Composable
private fun TorrentMetaText(value: String?) {
    if (value == null) return
    Text(
        text = value,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Small pill used for NSFW / Dead markers. Not focusable - labels only. */
@Composable
fun TvBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Box(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

/** Resolves a [Category] to its localized label. */
@Composable
fun Category.displayName(): String = stringResource(
    when (this) {
        Category.All -> R.string.category_all
        Category.Anime -> R.string.category_anime
        Category.Apps -> R.string.category_apps
        Category.Books -> R.string.category_books
        Category.Games -> R.string.category_games
        Category.Movies -> R.string.category_movies
        Category.Music -> R.string.category_music
        Category.Porn -> R.string.category_porn
        Category.Series -> R.string.category_series
        Category.Other -> R.string.category_other
    },
)

/** Full-screen centred progress indicator. */
@Composable
fun TvLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Full-screen message with an optional single action.
 *
 * Replaces the handheld `ContentState` icon blocks; the icon is dropped because
 * a 80dp glyph at TV scale reads as decoration rather than information.
 */
@Composable
fun TvMessageState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    // Entry point for screen focus: attached to the action button, which is the
    // only focusable this state offers.
    focusRequester: FocusRequester? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(MaterialTheme.spaces.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small, Alignment.CenterVertically),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            TvActionButton(
                onClick = onAction,
                strong = true,
                modifier = if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                },
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

/**
 * Screen header.
 *
 * The action area gets its own horizontal scroll rather than being sized to its
 * content. Some screens have four long actions (Search providers) and three on
 * Bookmarks; without this they squeeze the title to zero width and it renders as
 * "Bo...". Giving each side a weight keeps the title legible and lets a crowded
 * action set scroll instead of crushing the header.
 */
@Composable
fun TvTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.medium),
    ) {
        if (onBack != null) {
            TvActionButton(onClick = onBack) {
                Text(text = stringResource(R.string.action_back))
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 200.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier
                .weight(1.3f)
                // Padding *inside* the scroll (after horizontalScroll in the chain)
                // so it acts as content padding: the viewport still clips to its own
                // bounds, and a focused action parked against the start or end edge
                // would otherwise have its enlarged body and ring cut off.
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = TvFocusDefaults.Reserve),
            horizontalArrangement = Arrangement.spacedBy(TvFocusDefaults.Reserve),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions()
        }
    }
}

/** 1dp spacer helper used between focusable rows to stop focus rings touching. */
@Composable
fun TvGap(height: androidx.compose.ui.unit.Dp = 12.dp) {
    Box(modifier = Modifier.size(width = 0.dp, height = height))
}