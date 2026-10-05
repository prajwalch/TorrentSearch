package com.prajwalch.torrentsearch.ui.tv.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.compose.foundation.BorderStroke
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface

import com.prajwalch.torrentsearch.ui.tv.theme.spaces
import com.prajwalch.torrentsearch.ui.tv.theme.tvCardSurface
import com.prajwalch.torrentsearch.ui.tv.theme.tvElevatedSurface
import com.prajwalch.torrentsearch.ui.tv.theme.tvFocusColor

/**
 * Focus treatment for the 10-foot UI.
 *
 * ## Why this is not left to the tv-material defaults
 *
 * `ButtonDefaults.scale()` and `ClickableSurfaceDefaults.scale()` both default
 * `focusedScale` to **1.0f**, i.e. focused controls do not scale at all. Their
 * only built-in focus cue is a soft glow, which is easy to lose against a dark
 * panel at sofa distance. On top of that the library's focused *border* and this
 * app's *selected* fill were both `colorScheme.primary`, so a selected chip and a
 * focused chip looked almost identical.
 *
 * Focus is therefore made explicit and multi-channel on every focusable:
 *
 * 1. a thick **high-contrast ring** whose colour is deliberately never a
 *    scheme role used for selection,
 * 2. a **scale bump**, so focus is also conveyed by size,
 * 3. the default glow, which now reinforces rather than carries the signal.
 *
 * Selection stays a *fill*; focus is always an *outline*. A chip that is both
 * selected and focused therefore shows a filled interior plus a bright ring and
 * reads unambiguously as "this is the active filter".
 */
object TvFocusDefaults {
    /** Rows and cards. */
    const val FocusedScaleCard: Float = 1.06f

    /**
     * Buttons and chips. Slightly smaller than [FocusedScaleCard] because these
     * sit inside dense chip rows where a large multiplier shifts neighbours.
     */
    const val FocusedScaleControl: Float = 1.05f

    /** Wide enough to survive a 4K panel being downscaled for a 1080p TV. */
    val BorderWidth: Dp = 3.dp

    /** Used for large primary actions where extra weight is warranted. */
    val BorderWidthStrong: Dp = 4.dp

    /**
     * Space reserved around focusable content inside scrolling containers.
     *
     * Scaling is a graphics-layer transform, so it does not change layout bounds
     * and `bringIntoView` will happily park a focused item flush against the
     * viewport edge - where the enlarged visual spills out of view and looks like
     * focus has left the screen. Reserving this much on each side keeps the whole
     * scaled item, ring included, inside the viewport and out of the overscan zone.
     */
    val Reserve: Dp = 12.dp
}

/** Ring shown around a focused card or row. */
@Composable
fun tvFocusBorder(strong: Boolean = false): Border = Border(
    border = BorderStroke(
        width = if (strong) TvFocusDefaults.BorderWidthStrong else TvFocusDefaults.BorderWidth,
        color = tvFocusColor(),
    ),
    shape = RoundedCornerShape(14.dp),
)

/** Ring shown around a focused button or chip. */
@Composable
fun tvControlFocusBorder(): Border = Border(
    border = BorderStroke(width = TvFocusDefaults.BorderWidth, color = tvFocusColor()),
    shape = RoundedCornerShape(12.dp),
)

/**
 * Horizontal/vertical padding that keeps a scaled focusable inside the viewport.
 *
 * Apply to every item inside a `LazyColumn`/`LazyRow`, and to the content padding
 * of those lists, so neither the enlarged body nor its ring is ever clipped.
 */
fun tvFocusReservePadding(
    horizontal: Dp = TvFocusDefaults.Reserve,
    vertical: Dp = TvFocusDefaults.Reserve,
): PaddingValues = PaddingValues(horizontal = horizontal, vertical = vertical)

/**
 * Content padding for a full-screen focusable list: overscan-safe margins plus
 * [TvFocusDefaults.Reserve] so the first and last focused items are not flush
 * against the viewport edge.
 */
@Composable
fun tvListContentPadding(): PaddingValues {
    val outer = MaterialTheme.spaces
    return PaddingValues(
        start = 48.dp + TvFocusDefaults.Reserve,
        end = 48.dp + TvFocusDefaults.Reserve,
        top = outer.medium + TvFocusDefaults.Reserve,
        bottom = outer.large + TvFocusDefaults.Reserve,
    )
}

/**
 * The focus ring: scale + outline, drawn with foundation APIs.
 *
 * This deliberately does *not* use `ButtonDefaults.border()` or
 * `ClickableSurfaceDefaults.border(focusedBorder = ...)`. In tv-material 1.1.0 those
 * roles did not reliably render - the focused Search button and the focused result
 * cards showed no ring at all, leaving focus indicated only by a faint fill change.
 * Foundation `Modifier.border` renders deterministically, so every focusable in the
 * app goes through here instead.
 *
 * Returns the input unchanged when [enabled] is false, so disabled controls also drop
 * out of the focus ring rather than becoming dead stops.
 */
@Composable
fun Modifier.tvFocusRing(
    shape: Shape = RoundedCornerShape(14.dp),
    scale: Float = TvFocusDefaults.FocusedScaleCard,
    borderWidth: Dp = TvFocusDefaults.BorderWidth,
    enabled: Boolean = true,
): Modifier {
    if (!enabled) return this

    var isFocused by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) scale else 1f,
        label = "tvFocusRingScale",
    )

    return this
        .onFocusChanged { isFocused = it.isFocused }
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .border(
            width = borderWidth,
            color = if (isFocused) tvFocusColor() else Color.Transparent,
            shape = shape,
        )
}

/**
 * Makes an arbitrary container D-pad focusable with the standard TV treatment.
 *
 * `Modifier.clickable` makes the node focusable and binds DPAD_CENTER/ENTER to
 * [onClick]; no separate `focusable()` is needed.
 *
 * @param enabled when false the element drops out of focus traversal entirely,
 *   which is how unavailable actions are removed from the focus ring rather than
 *   left as dead stops.
 */
@Composable
fun Modifier.tvFocusable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    scale: Float = TvFocusDefaults.FocusedScaleCard,
    onClick: () -> Unit,
): Modifier {
    if (!enabled) return this

    return this
        .tvFocusRing(shape = shape, scale = scale, enabled = enabled)
        .clickable(onClick = onClick)
}

/** Border shape used for focusable cards and list rows. */
val TvCardShape: Shape = RoundedCornerShape(16.dp)

/** Border shape used for chips and small controls. */
val TvControlShape: Shape = RoundedCornerShape(10.dp)

/**
 * A row of focusable controls that keeps [TvFocusDefaults.Reserve] around each
 * item so the focus ring and the scaled body are never clipped by the parent.
 */
@Composable
fun TvFocusableRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.padding(tvFocusReservePadding()),
        horizontalArrangement = horizontalArrangement,
        content = content,
    )
}

/**
 * The app's button, with the standard TV focus treatment applied.
 *
 * Every actionable control in the TV UI goes through this rather than
 * `androidx.tv.material3.Button` directly. tv-material's `ButtonDefaults.scale()`
 * defaults `focusedScale` to 1.0f, so a bare focused button only shifts its glow -
 * easy to miss from a sofa. This adds a ring and a scale bump.
 */
@Composable
fun TvActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    strong: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(if (strong) 14.dp else 12.dp)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.tvElevatedSurface,
            focusedContentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.tvCardSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            pressedContainerColor = MaterialTheme.colorScheme.tvElevatedSurface,
            pressedContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier.tvFocusRing(
            shape = shape,
            scale = TvFocusDefaults.FocusedScaleControl,
            borderWidth = if (strong) {
                TvFocusDefaults.BorderWidthStrong
            } else {
                TvFocusDefaults.BorderWidth
            },
            enabled = enabled,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

/** Convenience for a clickable TV surface with the standard focus treatment. */
@Composable
fun TvFocusableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    shape: Shape = TvCardShape,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = if (selected) {
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
        } else {
            ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.tvCardSurface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.tvElevatedSurface,
                focusedContentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.tvCardSurface,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                pressedContainerColor = MaterialTheme.colorScheme.tvElevatedSurface,
                pressedContentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
        scale = ClickableSurfaceDefaults.scale(focusedScale = TvFocusDefaults.FocusedScaleCard),
        border = ClickableSurfaceDefaults.border(focusedBorder = tvFocusBorder()),
        modifier = modifier,
    ) {
        content()
    }
}