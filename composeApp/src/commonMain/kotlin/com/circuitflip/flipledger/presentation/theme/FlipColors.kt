package com.circuitflip.flipledger.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Full semantic color set for FlipLedger, mirroring the reference design's CSS custom
 * properties. Two concrete instances exist: [LoftColors] (light) and [DarkColors]
 * (rp-new-dark). Screens read colors via [LocalFlipColors] so switching themes is a
 * single state flip.
 */
@Immutable
data class FlipColors(
    val primary: Color,
    val primaryDark: Color,
    val primaryHighlight: Color,
    val secondary: Color,
    val secondaryDark: Color,
    val secondaryHighlight: Color,
    // Backgrounds / surfaces
    val backgroundDefault: Color,   // cards / surfaces
    val backgroundSubtle: Color,    // page canvas
    val backgroundMuted: Color,     // chips / muted fills
    // Text
    val textDefault: Color,
    val textWeaker: Color,
    val textWeakest: Color,
    val textInverse: Color,
    // Borders
    val borderDefault: Color,
    val borderStrong: Color,
    // Status
    val success: Color,
    val error: Color,
    val warning: Color,
    val info: Color,
    // Status pill backgrounds (device statuses)
    val statusRepairBg: Color,
    val statusRepairFg: Color,
    val statusReadyBg: Color,
    val statusReadyFg: Color,
    val statusListedBg: Color,
    val statusListedFg: Color,
    // Loft brand accents (used for illustration blobs / category cards)
    val accentLilac: Color,
    val accentAmber: Color,
    val accentAmberLight: Color,
    val accentPink: Color,
    val accentCream: Color,
    val accentLoyaltyBlue: Color,
    val isDark: Boolean,
)

/** Light "loft" theme — the app's default look. */
val LoftColors = FlipColors(
    primary = Color(0xFF282829),
    primaryDark = Color(0xFF000000),
    primaryHighlight = Color(0xFFF9F7F7),
    secondary = Color(0xFFF9F7F7),
    secondaryDark = Color(0xFFF0E9E0),
    secondaryHighlight = Color(0xFFF7F4F4),
    backgroundDefault = Color(0xFFFFFFFF),
    backgroundSubtle = Color(0xFFF9F7F7),
    backgroundMuted = Color(0xFFE9EAEB),
    textDefault = Color(0xFF282829),
    textWeaker = Color(0xFF4A4A4B),
    textWeakest = Color(0xFF707689),
    textInverse = Color(0xFFFFFFFF),
    borderDefault = Color(0xFFE9EAEB),
    borderStrong = Color(0xFFC7CCD0),
    success = Color(0xFF4CAF6E),
    error = Color(0xFFD01A1F),
    warning = Color(0xFFE8A020),
    info = Color(0xFF0076CC),
    statusRepairBg = Color(0xFFFEF3DC),
    statusRepairFg = Color(0xFFA06A12),
    statusReadyBg = Color(0xFFE5F4E8),
    statusReadyFg = Color(0xFF4CAF6E),
    statusListedBg = Color(0xFFE3F2FC),
    statusListedFg = Color(0xFF0076CC),
    accentLilac = Color(0xFFB1B1F3),
    accentAmber = Color(0xFFF7B153),
    accentAmberLight = Color(0xFFFEF3DC),
    accentPink = Color(0xFFFAB1F3),
    accentCream = Color(0xFFF0E9E0),
    accentLoyaltyBlue = Color(0xFFB3D8F0),
    isDark = false,
)

/** Dark "rp-new-dark" theme. */
val DarkColors = FlipColors(
    primary = Color(0xFF77C6D0),
    primaryDark = Color(0xFF9BE1EA),
    primaryHighlight = Color(0xFF1B314C),
    secondary = Color(0xFFDD7755),
    secondaryDark = Color(0xFFF1C6B8),
    secondaryHighlight = Color(0xFF3D2C38),
    backgroundDefault = Color(0xFF0F1925),
    backgroundSubtle = Color(0xFF162435),
    backgroundMuted = Color(0xFF203145),
    textDefault = Color(0xFFEDEFF2),
    textWeaker = Color(0xFFB3BDC7),
    textWeakest = Color(0xFF7E8E9F),
    textInverse = Color(0xFF0F1925),
    borderDefault = Color(0xFF303B47),
    borderStrong = Color(0xFF4B5760),
    success = Color(0xFF3DC389),
    error = Color(0xFFF56B7E),
    warning = Color(0xFFF0B429),
    info = Color(0xFF55B2F2),
    statusRepairBg = Color(0xFF3A2A10),
    statusRepairFg = Color(0xFFFFD175),
    statusReadyBg = Color(0xFF163A2F),
    statusReadyFg = Color(0xFF6FDCA8),
    statusListedBg = Color(0xFF12304A),
    statusListedFg = Color(0xFF7EC6F5),
    accentLilac = Color(0xFF8F8FCC),
    accentAmber = Color(0xFFF7B153),
    accentAmberLight = Color(0xFF3A2A10),
    accentPink = Color(0xFFB264A8),
    accentCream = Color(0xFF203145),
    accentLoyaltyBlue = Color(0xFF2A4A66),
    isDark = true,
)
