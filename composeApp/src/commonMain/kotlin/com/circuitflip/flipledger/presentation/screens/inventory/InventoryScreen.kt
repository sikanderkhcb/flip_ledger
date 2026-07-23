package com.circuitflip.flipledger.presentation.screens.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.domain.model.DeviceStatus
import com.circuitflip.flipledger.domain.util.Money
import com.circuitflip.flipledger.presentation.components.DeviceRow
import com.circuitflip.flipledger.presentation.components.SelectableChip
import com.circuitflip.flipledger.presentation.rememberViewModel
import com.circuitflip.flipledger.presentation.theme.FlipTheme

/** 08 · Inventory — search, status filter chips, device list, invested total, FAB. */
@Composable
fun InventoryScreen(onAddDevice: () -> Unit, onOpenDevice: (String) -> Unit) {
    val vm = rememberViewModel<InventoryViewModel>()
    val state by vm.state.collectAsState()
    val colors = FlipTheme.colors

    Box(Modifier.fillMaxSize().background(colors.backgroundSubtle)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Inventory", style = FlipTheme.typography.headingXl, color = colors.textDefault)
                Text("Invested ${Money.format(state.totalInvestedCents)}", style = FlipTheme.typography.bodyM, color = colors.textWeaker)
            }
            Spacer(Modifier.height(16.dp))
            // Search box
            Row(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)).background(colors.backgroundDefault)
                    .border(1.dp, colors.borderDefault, RoundedCornerShape(12.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textWeakest)
                Spacer(Modifier.height(0.dp))
                Box(Modifier.padding(start = 10.dp).fillMaxWidth()) {
                    if (state.query.isEmpty()) Text("Search inventory", style = FlipTheme.typography.bodyL, color = colors.textWeakest)
                    BasicTextField(
                        value = state.query, onValueChange = vm::onQuery, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = FlipTheme.typography.bodyL.copy(color = colors.textDefault),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // Status filter chips
            LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf<Pair<String, DeviceStatus?>>("All" to null) + DeviceStatus.entries.filter { it != DeviceStatus.SOLD }.map { it.label to it }
                items(filters) { (label, status) ->
                    SelectableChip(label = label, selected = state.statusFilter == status, onClick = { vm.onFilter(status) })
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 0.dp, 20.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.error?.let { message ->
                    item {
                        Text(message, style = FlipTheme.typography.bodyM, color = colors.error)
                    }
                }
                if (state.devices.isEmpty()) {
                    item {
                        Text(
                            if (state.query.isNotBlank() || state.statusFilter != null) {
                                "No devices match your search."
                            } else {
                                "No devices yet. Tap + to add your first device."
                            },
                            style = FlipTheme.typography.bodyM,
                            color = colors.textWeaker,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        )
                    }
                } else {
                    items(state.devices, key = { it.id }) { device ->
                        DeviceRow(device = device, onClick = { onOpenDevice(device.id) })
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAddDevice, containerColor = colors.primary, contentColor = colors.textInverse,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
        ) { Icon(Icons.Rounded.Add, contentDescription = "Add Device") }
    }
}
