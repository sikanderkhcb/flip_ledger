package com.circuitflip.flipledger.presentation.screens.devicedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.domain.util.Money
import com.circuitflip.flipledger.presentation.components.FlipCard
import com.circuitflip.flipledger.presentation.components.FlipTopBar
import com.circuitflip.flipledger.presentation.components.PrimaryButton
import com.circuitflip.flipledger.presentation.components.SecondaryButton
import com.circuitflip.flipledger.presentation.components.StatusPill
import com.circuitflip.flipledger.presentation.rememberViewModel
import com.circuitflip.flipledger.presentation.theme.FlipTheme

/** 14 · Device Detail — hero stats, overview, costs breakdown, timeline, status action, add cost, sell. */
@Composable
fun DeviceDetailScreen(deviceId: String, onBack: () -> Unit, onAddCost: () -> Unit, onStartSale: () -> Unit) {
    val vm = rememberViewModel<DeviceDetailViewModel>(key = deviceId)
    LaunchedEffect(deviceId) { vm.load(deviceId) }
    val state by vm.state.collectAsState()
    val submitting by vm.submitting.collectAsState()
    val colors = FlipTheme.colors
    val device = state.device ?: return

    Box(Modifier.fillMaxSize().background(colors.backgroundSubtle)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            FlipTopBar(title = device.model, onBack = onBack, trailing = { StatusPill(device.status) })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
                // Hero stat row
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeroStat("INVESTED", Money.format(device.investedCents), Modifier.weight(1f))
                    HeroStat("EXPECTED PROFIT", "+" + Money.format(state.expectedProfitCents), Modifier.weight(1f), colors.success)
                    HeroStat("DAYS HELD", device.daysHeld.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(20.dp))

                // Overview
                Text("Overview", style = FlipTheme.typography.headingM, color = colors.textDefault)
                Spacer(Modifier.height(8.dp))
                FlipCard {
                    listOf(
                        "Category" to device.category.label,
                        "Condition" to (device.condition?.label ?: "—"),
                        "Storage" to device.storage,
                        "Identifier" to device.identifier,
                        "Purchased" to "${device.purchaseDate} · ${device.source?.label ?: "—"}",
                    ).forEachIndexed { i, (l, v) ->
                        DetailRow(l, v); if (i < 4) HorizontalDivider(color = colors.borderDefault)
                    }
                }
                Spacer(Modifier.height(20.dp))

                // Costs
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Costs", style = FlipTheme.typography.headingM, color = colors.textDefault)
                    Text("+ Add cost", style = FlipTheme.typography.bodyM, color = colors.info, modifier = Modifier.clickable(onClick = onAddCost).padding(4.dp))
                }
                Spacer(Modifier.height(8.dp))
                FlipCard {
                    DetailRow("Purchase price", Money.format(device.purchasePriceCents))
                    device.costs.forEach { c -> HorizontalDivider(color = colors.borderDefault); DetailRow(c.type.label, Money.format(c.amountCents)) }
                }
                Spacer(Modifier.height(20.dp))

                // Timeline
                Text("Timeline", style = FlipTheme.typography.headingM, color = colors.textDefault)
                Spacer(Modifier.height(8.dp))
                FlipCard {
                    val timeline = buildList {
                        add(device.purchaseDate to "Added to inventory")
                        if (device.costs.isNotEmpty()) add("Recently" to "${device.costs.size} cost${if (device.costs.size > 1) "s" else ""} logged")
                        add("Today" to "Status: ${device.status.label}")
                    }
                    timeline.forEach { (date, event) ->
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Text(date, style = FlipTheme.typography.caption, color = colors.textWeakest, modifier = Modifier.width(72.dp))
                            Text("— $event", style = FlipTheme.typography.bodyM, color = colors.textDefault)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Column(Modifier.padding(20.dp)) {
                PrimaryButton(vm.primaryActionLabel(device.status), onClick = { if (vm.onPrimaryAction(device.status)) onStartSale() }, loading = submitting)
                Spacer(Modifier.height(10.dp))
                SecondaryButton("Add cost", onAddCost)
                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = FlipTheme.typography.bodyS, color = colors.error)
                }
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier, valueColor: androidx.compose.ui.graphics.Color? = null) {
    FlipCard(modifier = modifier, padding = 12.dp) {
        Text(label, style = FlipTheme.typography.caption, color = FlipTheme.colors.textWeakest, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Text(value, style = FlipTheme.typography.headingM, color = valueColor ?: FlipTheme.colors.textDefault)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = FlipTheme.typography.bodyM, color = FlipTheme.colors.textWeaker)
        Text(value, style = FlipTheme.typography.headingS, color = FlipTheme.colors.textDefault)
    }
}
