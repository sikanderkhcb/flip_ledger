package com.blackink.app.presentation.screens.adddevice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blackink.app.presentation.components.PrimaryButton
import com.blackink.app.presentation.components.SecondaryButton
import com.blackink.app.presentation.theme.FlipTheme

/** 13 · Device Added — success confirmation with quick follow-up actions. */
@Composable
fun DeviceAddedScreen(
    deviceSummary: String,
    onGoToInventory: () -> Unit,
    onAddRepairCost: () -> Unit,
    onAddAnother: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(FlipTheme.colors.backgroundSubtle), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(FlipTheme.colors.statusReadyBg), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = FlipTheme.colors.success, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Device added", style = FlipTheme.typography.displayM, color = FlipTheme.colors.textDefault)
            Spacer(Modifier.height(8.dp))
            Text("It's now in your inventory and ready to track.", style = FlipTheme.typography.bodyL, color = FlipTheme.colors.textWeaker, textAlign = TextAlign.Center)
            if (deviceSummary.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(deviceSummary, style = FlipTheme.typography.bodyM, color = FlipTheme.colors.textWeakest, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(32.dp))
            SecondaryButton("Add repair expense", onAddRepairCost)
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Add another device", onAddAnother)
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Go to inventory", onGoToInventory)
        }
    }
}
