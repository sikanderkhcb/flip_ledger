package com.circuitflip.flipledger.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.presentation.theme.FlipTheme

/**
 * The four primary destinations of the app's bottom navigation bar, matching the design's
 * `TabBar` (Home · Inventory · Sales · More).
 */
enum class BottomTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    INVENTORY("Inventory", Icons.Rounded.Inventory2),
    SALES("Sales", Icons.Rounded.ReceiptLong),
    MORE("More", Icons.Rounded.MoreHoriz),
}

/**
 * Persistent bottom navigation bar shown on the primary screens. The [active] tab is tinted
 * with the primary color; tapping another tab invokes [onSelect].
 */
@Composable
fun FlipBottomBar(active: BottomTab, onSelect: (BottomTab) -> Unit) {
    val colors = FlipTheme.colors
    Column(Modifier.fillMaxWidth().background(colors.backgroundDefault)) {
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(colors.borderDefault))
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomTab.entries.forEach { tab ->
                val selected = tab == active
                val tint = if (selected) colors.primary else colors.textWeakest
                Column(
                    modifier = Modifier.weight(1f).clickable { onSelect(tab) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = tint)
                    Spacer(Modifier.height(4.dp))
                    Text(tab.label, style = FlipTheme.typography.caption, color = tint)
                }
            }
        }
    }
}
