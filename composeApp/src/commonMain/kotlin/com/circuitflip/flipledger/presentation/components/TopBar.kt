package com.circuitflip.flipledger.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.presentation.theme.FlipTheme
import com.circuitflip.flipledger.presentation.theme.Spacing

/** Lightweight top bar: back chevron + centered/leading title + optional trailing slot. */
@Composable
fun FlipTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(56.dp).padding(horizontal = Spacing.x400),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = FlipTheme.colors.textDefault,
                modifier = Modifier.size(24.dp).clickable(onClick = onBack),
            )
            Spacer(Modifier.width(Spacing.x300))
        }
        Text(title, style = FlipTheme.typography.headingL, color = FlipTheme.colors.textDefault, modifier = Modifier.weight(1f))
        if (trailing != null) trailing()
    }
}
