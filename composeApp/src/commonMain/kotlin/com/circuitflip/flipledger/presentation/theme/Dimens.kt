package com.circuitflip.flipledger.presentation.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing, radius, and elevation tokens (4px base scale) transcribed from the design system.
 * Named to match the source `--rp-space-*` / `--rp-radius-*` tokens.
 */
object Spacing {
    val x0 = 0.dp
    val x050 = 2.dp
    val x100 = 4.dp
    val x150 = 6.dp
    val x200 = 8.dp
    val x300 = 12.dp
    val x400 = 16.dp
    val x500 = 20.dp
    val x600 = 24.dp
    val x800 = 32.dp
    val x1000 = 40.dp
    val x1200 = 48.dp
    val x1600 = 64.dp
}

object Radius {
    val sm = 6.dp
    val input = 8.dp     // --rp-radius-input
    val button = 12.dp   // --rp-radius-button (loft: rounded, not pill)
    val card = 16.dp     // --rp-radius-card
    val lg = 24.dp
    val round = 999.dp
}

object Elevation {
    val card = 1.dp      // shadow-1
    val raised = 4.dp    // shadow-2
    val overlay = 12.dp  // shadow-3
}
