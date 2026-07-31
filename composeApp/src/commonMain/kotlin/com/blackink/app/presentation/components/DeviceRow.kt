package com.blackink.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TabletMac
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blackink.app.domain.model.Device
import com.blackink.app.domain.model.DeviceCategory
import com.blackink.app.domain.util.Money
import com.blackink.app.presentation.theme.FlipTheme

/** Inventory list row: category icon, model + meta, invested value, status pill. */
@Composable
fun DeviceRow(device: Device, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FlipTheme.colors
    FlipCard(modifier = modifier, padding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBlob(color = categoryBlob(device.category), size = 44.dp) {
                Icon(categoryIcon(device.category), contentDescription = null, tint = colors.textDefault)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(device.model, style = FlipTheme.typography.headingS, color = colors.textDefault)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${device.daysHeld}d held · ${device.storage}",
                    style = FlipTheme.typography.caption,
                    color = if (device.isAging) colors.warning else colors.textWeakest,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(device.investedCents), style = FlipTheme.typography.headingS, color = colors.textDefault)
                Spacer(Modifier.height(4.dp))
                StatusPill(device.status)
            }
        }
    }
}

@Composable
private fun categoryBlob(category: DeviceCategory) = when (category) {
    DeviceCategory.PHONE -> FlipTheme.colors.accentLilac
    DeviceCategory.LAPTOP -> FlipTheme.colors.accentLoyaltyBlue
    DeviceCategory.TABLET -> FlipTheme.colors.accentAmberLight
    DeviceCategory.GAMING -> FlipTheme.colors.accentPink
    DeviceCategory.ACCESSORY -> FlipTheme.colors.accentCream
}

fun categoryIcon(category: DeviceCategory) = when (category) {
    DeviceCategory.PHONE -> Icons.Rounded.PhoneIphone
    DeviceCategory.LAPTOP -> Icons.Rounded.LaptopMac
    DeviceCategory.TABLET -> Icons.Rounded.TabletMac
    DeviceCategory.GAMING -> Icons.Rounded.SportsEsports
    DeviceCategory.ACCESSORY -> Icons.Rounded.Headphones
}
