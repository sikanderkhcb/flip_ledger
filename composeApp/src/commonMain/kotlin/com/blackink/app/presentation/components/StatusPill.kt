package com.blackink.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blackink.app.domain.model.DeviceStatus
import com.blackink.app.presentation.theme.FlipTheme

/** Colored status badge whose palette maps to [DeviceStatus] exactly per the design. */
@Composable
fun StatusPill(status: DeviceStatus, modifier: Modifier = Modifier) {
    val colors = FlipTheme.colors
    val (bg: Color, fg: Color) = when (status) {
        DeviceStatus.PURCHASED -> colors.backgroundMuted to colors.textWeaker
        DeviceStatus.REPAIR -> colors.statusRepairBg to colors.statusRepairFg
        DeviceStatus.READY -> colors.statusReadyBg to colors.statusReadyFg
        DeviceStatus.LISTED -> colors.statusListedBg to colors.statusListedFg
        DeviceStatus.SOLD -> colors.backgroundMuted to colors.textWeaker
    }
    Text(
        text = status.label,
        style = FlipTheme.typography.caption,
        color = fg,
        modifier = modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
