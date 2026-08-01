package com.blackink.app.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Typography scale transcribed from the design's `.t-*` utility classes.
 *
 * Two families:
 *  - Display  -> "P22 Mackinac Pro" (serif). Bundle the .otf in composeResources/font and
 *    load via [displayFontFamily]; falls back to the platform serif until then.
 *  - Sans     -> "Inter" for body/headings (loft theme uses Inter).
 *
 * Font loading is platform-agnostic through Compose Resources; see README "Fonts".
 */
@Immutable
data class FlipTypography(
    val displayM: TextStyle,
    val displayL: TextStyle,
    val headingXl: TextStyle,
    val headingL: TextStyle,
    val headingM: TextStyle,
    val headingS: TextStyle,
    val bodyL: TextStyle,
    val bodyM: TextStyle,
    val bodyS: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

/**
 * Builds the type scale. [displayFamily] and [sansFamily] are injected so the actual font
 * resources can be provided once loaded; defaults use system fallbacks.
 */
fun flipTypography(
    displayFamily: FontFamily = FontFamily.Serif,
    sansFamily: FontFamily = FontFamily.SansSerif,
): FlipTypography {
    val evenLineHeight = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    )
    fun sans(size: Int, lh: Double, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
        fontFamily = sansFamily, fontSize = size.sp, lineHeight = (size * lh).sp,
        fontWeight = weight, letterSpacing = tracking.em, lineHeightStyle = evenLineHeight,
    )
    return FlipTypography(
        displayM = TextStyle(fontFamily = displayFamily, fontSize = 36.sp, lineHeight = (36 * 1.15).sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.015).em),
        displayL = TextStyle(fontFamily = displayFamily, fontSize = 48.sp, lineHeight = (48 * 1.1).sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em),
        headingXl = sans(28, 1.2, FontWeight.Bold, -0.01),
        headingL = sans(22, 1.25, FontWeight.Bold),
        headingM = sans(18, 1.3, FontWeight.SemiBold),
        headingS = sans(16, 1.35, FontWeight.SemiBold),
        bodyL = sans(16, 1.5, FontWeight.Normal),
        bodyM = sans(14, 1.45, FontWeight.Normal),
        bodyS = sans(12, 1.4, FontWeight.Normal),
        label = sans(13, 1.3, FontWeight.Bold),
        caption = sans(11, 1.3, FontWeight.Normal),
    )
}
