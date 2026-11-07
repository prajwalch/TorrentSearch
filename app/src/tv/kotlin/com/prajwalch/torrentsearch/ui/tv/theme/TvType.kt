package com.prajwalch.torrentsearch.ui.tv.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography

/**
 * 10-foot type scale.
 *
 * The handheld theme only overrides `bodyLarge` at 16sp, which is roughly half
 * the comfortable reading size for a 55" panel viewed from a sofa. These values
 * are scaled up (body 18sp, titles 30-40sp) rather than relying on the user's
 * system font scale, which is unreliable on TV.
 */
private fun tvStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

val TvTypography = Typography(
    displayLarge = tvStyle(57, 64, FontWeight.Normal),
    displayMedium = tvStyle(45, 52),
    displaySmall = tvStyle(36, 44),

    headlineLarge = tvStyle(40, 48, FontWeight.Medium),
    headlineMedium = tvStyle(34, 42, FontWeight.Medium),
    headlineSmall = tvStyle(30, 38, FontWeight.Medium),

    titleLarge = tvStyle(26, 34, FontWeight.Medium, 0.15),
    titleMedium = tvStyle(22, 28, FontWeight.Medium, 0.15),
    titleSmall = tvStyle(19, 26, FontWeight.Medium, 0.1),

    bodyLarge = tvStyle(18, 26, letterSpacing = 0.5),
    bodyMedium = tvStyle(17, 24, letterSpacing = 0.25),
    bodySmall = tvStyle(15, 20, letterSpacing = 0.4),

    labelLarge = tvStyle(17, 22, FontWeight.Medium, 0.1),
    labelMedium = tvStyle(15, 20, FontWeight.Medium, 0.5),
    labelSmall = tvStyle(13, 18, FontWeight.Medium, 0.5),
)
