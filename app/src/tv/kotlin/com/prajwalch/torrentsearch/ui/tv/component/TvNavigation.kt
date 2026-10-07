package com.prajwalch.torrentsearch.ui.tv.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

import com.prajwalch.torrentsearch.ui.tv.theme.tvCardSurface
import com.prajwalch.torrentsearch.ui.tv.theme.tvElevatedSurface

/**
 * The TV app's top-level destinations.
 *
 * The handheld app reaches most of these from small top-bar icons, which a D-pad
 * makes genuinely hard to find. TV therefore gets a permanently visible menu
 * rail on the left: pressing LEFT from any screen lands on it, and every
 * destination is one OK press away.
 */
enum class TvDestination(val label: String) {
    Search("Search"),
    Browse("Browse"),
    Bookmarks("Bookmarks"),
    History("History"),
    Providers("Providers"),
    Settings("Settings"),
}

private val RailWidth = 260.dp

/**
 * Focus target for a screen's entry point.
 *
 * `TvScaffold` provides one; the screen attaches it to its first focusable
 * element (the top of its chip row, its first list row, its message action).
 * When a screen becomes current, focus moves there instead of staying on the
 * menu rail, so the screen immediately shows what the D-pad is driving.
 */
val LocalTvContentFocusRequester = staticCompositionLocalOf<FocusRequester?> { null }

/**
 * Screen chrome: a persistent left menu rail plus the screen body.
 *
 * The rail is always composed rather than hidden behind a drawer button, which is
 * what keeps navigation reachable without a "menu" affordance the user has to
 * discover. Focus moves between rail and body with LEFT/RIGHT.
 */
@Composable
fun TvScaffold(
    destination: TvDestination,
    onNavigate: (TvDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val contentFocusRequester = remember { FocusRequester() }
    val railFocusRequesters = remember { TvDestination.entries.map { FocusRequester() } }

    // Runs whenever this route enters composition - a rail pick, a push onto the
    // stack, or a pop back. The screen gets the frame to attach its entry
    // focusable; if it has none (an empty state with no action), focus stays in
    // the rail rather than disappearing.
    LaunchedEffect(Unit) {
        val reachedContent = runCatching { contentFocusRequester.requestFocus() }.isSuccess
        if (!reachedContent) {
            TvDestination.entries.indexOf(destination)
                .takeIf { it >= 0 }
                ?.let { runCatching { railFocusRequesters[it].requestFocus() } }
        }
    }

    CompositionLocalProvider(LocalTvContentFocusRequester provides contentFocusRequester) {
        Row(modifier = modifier.fillMaxSize()) {
            TvNavigationRail(
                current = destination,
                onNavigate = onNavigate,
                focusRequesters = railFocusRequesters,
                modifier = Modifier.width(RailWidth).fillMaxHeight(),
            )
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) { content() }
        }
    }
}

/** The persistent left-hand menu. */
@Composable
fun TvNavigationRail(
    current: TvDestination,
    onNavigate: (TvDestination) -> Unit,
    modifier: Modifier = Modifier,
    focusRequesters: List<FocusRequester> = emptyList(),
) {
    // Focus normally lives in the screen body; the rail only holds focus while it
    // is being used. `focusRequesters` exists for the fallback path in
    // [TvScaffold] - a screen with nothing focusable leaves focus on the active
    // menu entry, which is the one the user just picked.
    //
    // Selection is the fill, so the active section stays obvious even when the
    // ring is elsewhere.
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = TvFocusDefaults.Reserve),
        verticalArrangement = Arrangement.spacedBy(TvFocusDefaults.Reserve),
    ) {
        Text(
            text = "Torrent Search",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TvFocusDefaults.Reserve),
            verticalArrangement = Arrangement.spacedBy(TvFocusDefaults.Reserve),
        ) {
            TvDestination.entries.forEachIndexed { index, entry ->
                val entryModifier = focusRequesters.getOrNull(index)
                    ?.let { Modifier.focusRequester(it) }
                    ?: Modifier
                TvMenuItem(
                    label = entry.label,
                    active = entry == current,
                    onClick = { onNavigate(entry) },
                    modifier = entryModifier,
                )
            }
        }
    }
}

/**
 * One menu entry.
 *
 * Selection is the fill, focus is the ring - same contract as the category chips,
 * so the menu and the filters read the same way. The active destination is filled
 * so it is obvious where OK will take you.
 */
@Composable
fun TvMenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    Surface(
        onClick = onClick,
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.tvCardSurface
            },
            contentColor = if (active) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            focusedContainerColor = if (active) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.tvElevatedSurface
            },
            focusedContentColor = if (active) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            disabledContainerColor = MaterialTheme.colorScheme.tvCardSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            pressedContainerColor = MaterialTheme.colorScheme.tvElevatedSurface,
            pressedContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier
            .fillMaxWidth()
            .tvFocusRing(shape = RoundedCornerShape(10.dp)),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        )
    }
}

/**
 * A settings row: title on the left, current value on the right, whole row is one
 * focus target that activates on OK.
 */
@Composable
fun TvSettingRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.tvCardSurface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.tvElevatedSurface,
            focusedContentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.tvCardSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            pressedContainerColor = MaterialTheme.colorScheme.tvElevatedSurface,
            pressedContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier
            .fillMaxWidth()
            .tvFocusRing(shape = RoundedCornerShape(10.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

/** Non-interactive section header. */
@Composable
fun TvSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp),
    )
}