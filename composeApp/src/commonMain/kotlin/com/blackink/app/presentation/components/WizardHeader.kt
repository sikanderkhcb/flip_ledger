package com.blackink.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.blackink.app.presentation.theme.FlipTheme
import com.blackink.app.presentation.theme.Spacing

/**
 * Reusable multi-step wizard header: "STEP x OF y" eyebrow, a segmented progress bar,
 * a serif display title, and an optional subtitle. Used by Add Device (4 steps),
 * Setup (3 steps), and the Sale flow (3 steps).
 */
@Composable
fun WizardHeader(
    step: Int,
    totalSteps: Int,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = FlipTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Text("STEP $step OF $totalSteps", style = FlipTheme.typography.caption, color = colors.textWeakest)
        Spacer(Modifier.height(Spacing.x200))
        Row(Modifier.fillMaxWidth()) {
            repeat(totalSteps) { index ->
                val filled = index < step
                Row(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .padding(end = if (index < totalSteps - 1) 4.dp else 0.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (filled) colors.primary else colors.backgroundMuted),
                ) {}
            }
        }
        Spacer(Modifier.height(Spacing.x600))
        Text(title, style = FlipTheme.typography.displayM, color = colors.textDefault)
        if (subtitle != null) {
            Spacer(Modifier.height(Spacing.x300))
            Text(subtitle, style = FlipTheme.typography.bodyL, color = colors.textWeaker)
        }
    }
}
