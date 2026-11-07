package com.prajwalch.torrentsearch.ui.tv.theme

import android.os.Build

import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

import com.prajwalch.torrentsearch.ui.theme.backgroundDark
import com.prajwalch.torrentsearch.ui.theme.backgroundLight
import com.prajwalch.torrentsearch.ui.theme.errorContainerDark
import com.prajwalch.torrentsearch.ui.theme.errorContainerLight
import com.prajwalch.torrentsearch.ui.theme.errorDark
import com.prajwalch.torrentsearch.ui.theme.errorLight
import com.prajwalch.torrentsearch.ui.theme.inverseOnSurfaceDark
import com.prajwalch.torrentsearch.ui.theme.inverseOnSurfaceLight
import com.prajwalch.torrentsearch.ui.theme.inversePrimaryDark
import com.prajwalch.torrentsearch.ui.theme.inversePrimaryLight
import com.prajwalch.torrentsearch.ui.theme.inverseSurfaceDark
import com.prajwalch.torrentsearch.ui.theme.inverseSurfaceLight
import com.prajwalch.torrentsearch.ui.theme.onBackgroundDark
import com.prajwalch.torrentsearch.ui.theme.onBackgroundLight
import com.prajwalch.torrentsearch.ui.theme.onErrorContainerDark
import com.prajwalch.torrentsearch.ui.theme.onErrorContainerLight
import com.prajwalch.torrentsearch.ui.theme.onErrorDark
import com.prajwalch.torrentsearch.ui.theme.onErrorLight
import com.prajwalch.torrentsearch.ui.theme.onPrimaryContainerDark
import com.prajwalch.torrentsearch.ui.theme.onPrimaryContainerLight
import com.prajwalch.torrentsearch.ui.theme.onPrimaryDark
import com.prajwalch.torrentsearch.ui.theme.onPrimaryLight
import com.prajwalch.torrentsearch.ui.theme.onSecondaryContainerDark
import com.prajwalch.torrentsearch.ui.theme.onSecondaryContainerLight
import com.prajwalch.torrentsearch.ui.theme.onSecondaryDark
import com.prajwalch.torrentsearch.ui.theme.onSecondaryLight
import com.prajwalch.torrentsearch.ui.theme.onSurfaceDark
import com.prajwalch.torrentsearch.ui.theme.onSurfaceLight
import com.prajwalch.torrentsearch.ui.theme.onSurfaceVariantDark
import com.prajwalch.torrentsearch.ui.theme.onSurfaceVariantLight
import com.prajwalch.torrentsearch.ui.theme.onTertiaryContainerDark
import com.prajwalch.torrentsearch.ui.theme.onTertiaryContainerLight
import com.prajwalch.torrentsearch.ui.theme.onTertiaryDark
import com.prajwalch.torrentsearch.ui.theme.onTertiaryLight
import com.prajwalch.torrentsearch.ui.theme.primaryContainerDark
import com.prajwalch.torrentsearch.ui.theme.primaryContainerLight
import com.prajwalch.torrentsearch.ui.theme.primaryDark
import com.prajwalch.torrentsearch.ui.theme.primaryLight
import com.prajwalch.torrentsearch.ui.theme.scrimDark
import com.prajwalch.torrentsearch.ui.theme.scrimLight
import com.prajwalch.torrentsearch.ui.theme.secondaryContainerDark
import com.prajwalch.torrentsearch.ui.theme.secondaryContainerLight
import com.prajwalch.torrentsearch.ui.theme.secondaryDark
import com.prajwalch.torrentsearch.ui.theme.secondaryLight
import com.prajwalch.torrentsearch.ui.theme.surfaceDark
import com.prajwalch.torrentsearch.ui.theme.surfaceLight
import com.prajwalch.torrentsearch.ui.theme.surfaceVariantDark
import com.prajwalch.torrentsearch.ui.theme.surfaceVariantLight
import com.prajwalch.torrentsearch.ui.theme.tertiaryContainerDark
import com.prajwalch.torrentsearch.ui.theme.tertiaryContainerLight
import com.prajwalch.torrentsearch.ui.theme.tertiaryDark
import com.prajwalch.torrentsearch.ui.theme.tertiaryLight

/**
 * TV colour schemes, mirroring the handheld `ui/theme/Theme.kt` palettes so the
 * brand is identical across form factors.
 *
 * Only the component layer differs: `androidx.tv.material3` instead of
 * `androidx.compose.material3`, because the two libraries ship separate
 * `MaterialTheme` objects that must not be interleaved.
 *
 * Note `androidx.tv.material3.ColorScheme` exposes a deliberately smaller role
 * set than `androidx.compose.material3` - there is no `surfaceContainer*`
 * family, no `outline`/`outlineVariant` and no `surfaceBright`/`surfaceDim`.
 * Card-style surfaces therefore go through [tvCardSurface] and [tvElevatedSurface]
 * below rather than the container roles.
 */
private val TvDarkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    inversePrimary = inversePrimaryDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    scrim = scrimDark,
)

private val TvLightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    inversePrimary = inversePrimaryLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    scrim = scrimLight,
)

/**
 * `androidx.tv.material3` ships no `dynamicDarkColorScheme`, so Material You
 * colours are read from `androidx.compose.material3` and mapped onto the TV role
 * set. This keeps the user's `enableDynamicTheme` setting meaningful on TV.
 */
@Composable
private fun dynamicTvColorScheme(darkTheme: Boolean): ColorScheme {
    val context = LocalContext.current
    val mobile = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    return darkColorScheme(
        primary = mobile.primary,
        onPrimary = mobile.onPrimary,
        primaryContainer = mobile.primaryContainer,
        onPrimaryContainer = mobile.onPrimaryContainer,
        inversePrimary = mobile.inversePrimary,
        secondary = mobile.secondary,
        onSecondary = mobile.onSecondary,
        secondaryContainer = mobile.secondaryContainer,
        onSecondaryContainer = mobile.onSecondaryContainer,
        tertiary = mobile.tertiary,
        onTertiary = mobile.onTertiary,
        tertiaryContainer = mobile.tertiaryContainer,
        onTertiaryContainer = mobile.onTertiaryContainer,
        background = mobile.background,
        onBackground = mobile.onBackground,
        surface = mobile.surface,
        onSurface = mobile.onSurface,
        surfaceVariant = mobile.surfaceVariant,
        onSurfaceVariant = mobile.onSurfaceVariant,
        inverseSurface = mobile.inverseSurface,
        inverseOnSurface = mobile.inverseOnSurface,
        error = mobile.error,
        onError = mobile.onError,
        errorContainer = mobile.errorContainer,
        onErrorContainer = mobile.onErrorContainer,
        scrim = mobile.scrim,
    )
}

/** Forces true-black surfaces, useful on OLED panels. */
private fun ColorScheme.pureBlack() = copy(surface = Color.Black, background = Color.Black)

/**
 * The 10-foot theme.
 *
 * @param darkTheme resolved from the user's persisted `DarkTheme` setting.
 * @param dynamicColor Material You wallpaper colours; honoured on Android 12+.
 * @param pureBlack OLED-friendly true-black surfaces.
 */
@Composable
fun TvTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicTvColorScheme(darkTheme)

        darkTheme -> TvDarkColorScheme
        else -> TvLightColorScheme
    }

    val colorScheme = if (darkTheme && pureBlack) baseColorScheme.pureBlack() else baseColorScheme

    CompositionLocalProvider(LocalIsTvDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TvTypography,
            content = content,
        )
    }
}

/** True when the TV theme resolved to the dark scheme. */
val LocalIsTvDarkTheme = androidx.compose.runtime.staticCompositionLocalOf { true }

@Composable
internal fun isTvDarkTheme(): Boolean = LocalIsTvDarkTheme.current

/**
 * Supplies an `androidx.compose.material3.MaterialTheme` whose colours mirror the
 * TV scheme.
 *
 * Wrap any `androidx.compose.material3` primitive (notably `TextField`, which
 * `androidx.tv.material3` does not provide) in this so it does not fall back to
 * the library default light scheme.
 */
@Composable
fun TvMaterial3Bridge(content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.toMobile(darkTheme = isTvDarkTheme()),
        content = content,
    )
}

/**
 * Background for a focusable card.
 *
 * Stands in for the missing `surfaceContainer` role: `surfaceVariant` is the
 * nearest TV-available step above `surface` and still reads as a distinct layer
 * at 10-foot.
 */
val ColorScheme.tvCardSurface: Color get() = surfaceVariant

/**
 * Background for overlays (dialogs, drawers) that must sit above a screen.
 *
 * `surfaceVariant` again, lightened toward `onSurfaceVariant` so the layering
 * still reads on a panel with a bright wallpaper-derived dynamic scheme.
 */
val ColorScheme.tvElevatedSurface: Color
    get() = androidx.compose.ui.graphics.lerp(surfaceVariant, onSurfaceVariant, 0.08f)

/**
 * The colour of the focus ring.
 *
 * Deliberately not a scheme role. `colorScheme.primary` is used for *selection*
 * (a filled chip, a selected category), so using it for focus as well made a
 * focused control and a selected control nearly indistinguishable - the failure
 * mode where a user cannot tell what they just picked.
 *
 * Near-white on dark panels and near-black on light ones gives the maximum
 * luminance delta against *every* background in either scheme, including the
 * `primary` fill of a selected control. That keeps the ring readable whether or
 * not the focused item happens to be the selected one.
 */
@Composable
fun tvFocusColor(): Color =
    if (isTvDarkTheme()) Color(0xFFFFFFFF) else Color(0xFF0A0A0A)

/**
 * Spacing for the 10-foot UI.
 *
 * The handheld scale is deliberately not reused: dp values tuned for a 6" screen
 * read as cramped from ~2.5 m away, and the extra room also keeps focus targets
 * from crowding each other at 4K.
 */
data class TvSpace(
    val extraSmall: Dp = 8.dp,
    val small: Dp = 12.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
)

val MaterialTheme.spaces: TvSpace
    @Composable @ReadOnlyComposable get() = TvSpace()