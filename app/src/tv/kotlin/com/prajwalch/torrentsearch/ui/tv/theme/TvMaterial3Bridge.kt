package com.prajwalch.torrentsearch.ui.tv.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.tv.material3.ColorScheme as TvColorScheme
import androidx.compose.material3.ColorScheme as MobileColorScheme

/**
 * Maps a TV colour scheme onto the `androidx.compose.material3` scheme.
 *
 * `androidx.tv.material3` deliberately has no `TextField`/`BasicTextField`, dialog
 * host or navigation host. Those few primitives must therefore come from
 * `androidx.compose.material3`, which resolves its colours from its own
 * `MaterialTheme` - a different object that is never configured in the TV tree.
 * Without this bridge every mobile primitive renders with the library's default
 * *light* scheme (a white search field on a dark TV UI).
 *
 * `androidx.tv.material3.ColorScheme` has a smaller role set - no
 * `surfaceContainer*`, `surfaceBright`/`surfaceDim`, `outline`/`outlineVariant` -
 * so the missing roles are derived here rather than left at their defaults.
 */
fun TvColorScheme.toMobile(darkTheme: Boolean): MobileColorScheme {
    val container = surfaceVariant

    return if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceBright = surfaceBrightFallback(),
            surfaceDim = surface,
            surfaceContainerLowest = surface,
            surfaceContainerLow = surfaceVariant,
            surfaceContainer = surfaceVariant,
            surfaceContainerHigh = surfaceVariant,
            surfaceContainerHighest = surfaceVariant,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = onSurfaceVariant,
            outlineVariant = surfaceVariant,
            scrim = scrim,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceBright = surfaceBrightFallback(),
            surfaceDim = surface,
            surfaceContainerLowest = surface,
            surfaceContainerLow = surfaceVariant,
            surfaceContainer = surfaceVariant,
            surfaceContainerHigh = surfaceVariant,
            surfaceContainerHighest = surfaceVariant,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = onSurfaceVariant,
            outlineVariant = surfaceVariant,
            scrim = scrim,
        )
    }
}

/** `surfaceBright` has no TV role; one step above `surface` is close enough. */
private fun TvColorScheme.surfaceBrightFallback() = surfaceVariant