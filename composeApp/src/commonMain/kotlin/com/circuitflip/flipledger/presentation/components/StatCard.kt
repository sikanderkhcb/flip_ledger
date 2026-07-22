package com.circuitflip.flipledger.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.presentation.theme.FlipTheme

/** Small metric card for the dashboard grid (label + big value + optional caption). */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    captionColor: Color? = null,
) {
    FlipCard(modifier = modifier, padding = 16.dp) {
        Text(label, style = FlipTheme.typography.caption, color = FlipTheme.colors.textWeakest)
        Spacer(Modifier.height(6.dp))
        Text(value, style = FlipTheme.typography.headingL, color = FlipTheme.colors.textDefault)
        if (caption != null) {
            Spacer(Modifier.height(4.dp))
            Text(caption, style = FlipTheme.typography.caption, color = captionColor ?: FlipTheme.colors.textWeakest)
        }
    }
}
