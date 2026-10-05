package com.prajwalch.torrentsearch.ui.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

import com.prajwalch.torrentsearch.ui.tv.theme.tvCardSurface
import com.prajwalch.torrentsearch.ui.tv.theme.tvElevatedSurface

/**
 * Colour sets for a selectable chip.
 *
 * Two non-obvious constraints drive this:
 *
 * 1. **Every role must be passed.** `ClickableSurfaceDefaults.colors()` takes eight
 *    colour roles and the `focused*` ones default to a light tone, so a focused chip
 *    washes out to near-white regardless of `containerColor`.
 * 2. **Selection must not reuse the focus colour.** The focus ring is near-white in
 *    the dark scheme and `primary` is a near-white light green (#A3D398), so a
 *    selected chip and a focused chip were indistinguishable. Selection therefore
 *    uses `primaryContainer`, a dark green (#265022) with light green text.
 *
 * The result is that the two states are orthogonal and composable:
 *
 * | state | fill | ring | scale |
 * | --- | --- | --- | --- |
 * | unselected, unfocused | card surface | – | 1.0 |
 * | unselected, focused | lifted surface | white 3dp | 1.05 |
 * | selected, unfocused | dark green | – | 1.0 |
 * | selected, focused | dark green | white 3dp | 1.05 |
 */
@Composable
private fun unselectedChipColors(): ClickableSurfaceColors =
    ClickableSurfaceDefaults.colors(
        containerColor = MaterialTheme.colorScheme.tvCardSurface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.tvElevatedSurface,
        focusedContentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = MaterialTheme.colorScheme.tvCardSurface,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        pressedContainerColor = MaterialTheme.colorScheme.tvElevatedSurface,
        pressedContentColor = MaterialTheme.colorScheme.onSurface,
    )

@Composable
private fun selectedChipColors(): ClickableSurfaceColors =
    ClickableSurfaceDefaults.colors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        focusedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
        disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        pressedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        pressedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )

/**
 * A selectable chip for category / sort / filter rows.
 *
 * Built on a focusable TV `Surface` rather than `FilterChip`: at 4K from a sofa a
 * filled chip with a thick ring is far more legible than the hairline outline a
 * `FilterChip` uses.
 */
@Composable
fun TvSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = if (selected) selectedChipColors() else unselectedChipColors(),
        // Our own ring rather than tv-material's border/scale roles: those default
        // focusedScale to 1.0f and did not render consistently. See tvFocusRing.
        modifier = modifier.tvFocusRing(
            shape = RoundedCornerShape(10.dp),
            scale = TvFocusDefaults.FocusedScaleControl,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}

/**
 * Horizontal row of chips.
 *
 * Uses standard `LazyRow` - the `TvLazyRow`/`TvLazyColumn` composables were
 * deprecated in `tv-foundation` 1.0.0-alpha11 and removed in alpha12; standard
 * Compose Foundation lazy layouts have carried the focus behaviour since 1.7.0.
 *
 * @param initialFocusRequester when non-null, focus lands on the *selected* chip
 *   (or the first one). Without it, focus enters the row on whichever chip is
 *   geometrically nearest the element above - mid-row, e.g. "Games" instead of the
 *   active category.
 */
@Composable
fun TvChipRow(
    chips: List<TvChip>,
    modifier: Modifier = Modifier,
    initialFocusRequester: FocusRequester? = null,
) {
    val listState = rememberLazyListState()

    LazyRow(
        modifier = modifier,
        state = listState,
        // Reserve room at both ends. The focus scale is a graphics transform, so a
        // focused chip parked at the row edge would have its enlarged body and ring
        // clipped, which reads as focus leaving the screen.
        contentPadding = PaddingValues(horizontal = TvFocusDefaults.Reserve),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            count = chips.size,
            key = { index -> chips[index].key },
        ) { index ->
            val chip = chips[index]
            val isEntryPoint = initialFocusRequester != null &&
                (chip.selected || (chips.none { it.selected } && index == 0))

            TvSelectableChip(
                label = chip.label,
                selected = chip.selected,
                onClick = chip.onClick,
                modifier = if (isEntryPoint) {
                    Modifier.focusRequester(initialFocusRequester)
                } else {
                    Modifier
                },
            )
        }
    }
}

data class TvChip(
    val key: String,
    val label: String,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)