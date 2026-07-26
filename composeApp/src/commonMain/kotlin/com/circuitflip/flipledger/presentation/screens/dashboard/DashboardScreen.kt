package com.circuitflip.flipledger.presentation.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.domain.model.AttentionItem
import com.circuitflip.flipledger.domain.model.Sale
import com.circuitflip.flipledger.domain.model.FREE_DEVICE_LIMIT
import com.circuitflip.flipledger.domain.model.SubscriptionAccess
import com.circuitflip.flipledger.domain.model.CategoryCount
import com.circuitflip.flipledger.domain.model.CategoryBars
import com.circuitflip.flipledger.domain.util.Money
import com.circuitflip.flipledger.domain.util.toPercentLabel
import com.circuitflip.flipledger.presentation.components.FlipCard
import com.circuitflip.flipledger.presentation.components.IconBlob
import com.circuitflip.flipledger.presentation.components.StatCard
import com.circuitflip.flipledger.presentation.rememberViewModel
import com.circuitflip.flipledger.presentation.theme.FlipTheme

/** 07 · Dashboard — greeting, hero profit card, metric grid, attention items, recent sales, FAB. */
@Composable
fun DashboardScreen(
    subscriptionAccess: SubscriptionAccess,
    onAddDevice: () -> Unit,
    onSeeAllSales: () -> Unit,
    onOpenDevice: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSubscription: () -> Unit,
) {
    val vm = rememberViewModel<DashboardViewModel>()
    val state by vm.state.collectAsState()
    val colors = FlipTheme.colors
    val greeting = remember {
        val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Box(Modifier.fillMaxSize().background(colors.backgroundSubtle)) {
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = colors.primary) }
            return@Box
        }
        val m = state.metrics!!
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 0.dp, 20.dp, 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (state.profile.ownerName.isBlank()) greeting else "$greeting, ${state.profile.ownerName}",
                        style = FlipTheme.typography.headingXl, color = colors.textDefault, modifier = Modifier.weight(1f),
                    )
                }
            }
            if (
                !subscriptionAccess.isUnlimited &&
                subscriptionAccess.lifetimeDevicesCreated >= FREE_DEVICE_LIMIT - 2
            ) {
                item {
                    FlipCard(onClick = onOpenSubscription) {
                        Text(
                            "${subscriptionAccess.lifetimeDevicesCreated.coerceAtMost(FREE_DEVICE_LIMIT)} of $FREE_DEVICE_LIMIT free device flips used",
                            style = FlipTheme.typography.headingS,
                            color = colors.textDefault,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (subscriptionAccess.remainingFreeDevices == 0) {
                                "Upgrade to add another device."
                            } else {
                                "${subscriptionAccess.remainingFreeDevices} free device ${if (subscriptionAccess.remainingFreeDevices == 1) "flip" else "flips"} remaining."
                            },
                            style = FlipTheme.typography.bodyM,
                            color = colors.textWeaker,
                        )
                    }
                }
            }
            // Hero month-profit card
            item {
                FlipCard {
                    Text("MONTH NET PROFIT", style = FlipTheme.typography.caption, color = colors.textWeakest)
                    Spacer(Modifier.height(6.dp))
                    Text(Money.format(m.monthNetProfitCents), style = FlipTheme.typography.displayM, color = colors.textDefault)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val up = m.profitDeltaPercent >= 0
                        val deltaColor = if (up) colors.success else colors.error
                        Icon(
                            if (up) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = null, tint = deltaColor, modifier = Modifier.size(16.dp),
                        )
                        val sign = if (m.profitDeltaPercent > 0) "+" else ""
                        Text(" $sign${m.profitDeltaPercent}% vs last month", style = FlipTheme.typography.bodyM, color = deltaColor)
                    }
                }
            }
            // Metric grid (2x2)
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard("Inventory Value", Money.format(m.inventoryValueCents), Modifier.weight(1f))
                    StatCard("Avg Margin", m.avgMarginFraction.toPercentLabel(), Modifier.weight(1f))
                }
            }
            if (state.categoryCounts.isNotEmpty()) {
                item { CategoryPieCard(state.categoryCounts) }
                item { BoughtSoldBarCard(state.categoryBars) }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard("Aging Devices", m.agingCount.toString(), Modifier.weight(1f), caption = if (m.agingCount > 0) "Attention needed" else null, captionColor = colors.warning)
                    StatCard("Sales / mo", m.salesThisMonth.toString(), Modifier.weight(1f))
                }
            }
            // Attention needed
            state.error?.let { message ->
                item {
                    FlipCard {
                        Text(message, style = FlipTheme.typography.bodyM, color = colors.error)
                    }
                }
            }
            if (state.attention.isNotEmpty()) {
                item { Text("Attention needed", style = FlipTheme.typography.headingM, color = colors.textDefault) }
                items(state.attention) { attn -> AttentionRow(attn) { attn.deviceId?.let(onOpenDevice) } }
            }
            // Recent sales
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent sales", style = FlipTheme.typography.headingM, color = colors.textDefault)
                    Text("See all", style = FlipTheme.typography.bodyM, color = colors.info, modifier = Modifier.clickable(onClick = onSeeAllSales))
                }
            }
            if (state.recentSales.isEmpty()) {
                item {
                    FlipCard {
                        Text("No sales yet.", style = FlipTheme.typography.bodyM, color = colors.textWeaker)
                    }
                }
            } else {
                items(state.recentSales) { sale -> RecentSaleRow(sale) }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }

        FloatingActionButton(
            onClick = onAddDevice,
            containerColor = colors.primary,
            contentColor = colors.textInverse,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
        ) { Icon(Icons.Rounded.Add, contentDescription = "Add Device") }
    }
}

@Composable
private fun CategoryPieCard(items: List<CategoryCount>) {
    val colors = FlipTheme.colors
    val palette = listOf(colors.primary, colors.success, colors.info, colors.warning, colors.accentAmberLight)
    FlipCard {
        Text("Devices by category", style = FlipTheme.typography.headingM, color = colors.textDefault)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val total = items.sumOf { it.count }.coerceAtLeast(1)
            Canvas(Modifier.size(150.dp)) {
                var start = -90f
                items.forEachIndexed { index, item ->
                    val sweep = item.count.toFloat() / total * 360f
                    drawArc(palette[index % palette.size], start, sweep, true)
                    start += sweep
                }
                drawCircle(colors.backgroundDefault, radius = size.minDimension * .22f)
            }
            Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { index, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(palette[index % palette.size]))
                        Spacer(Modifier.width(8.dp))
                        Text("${item.label} · ${item.count}", style = FlipTheme.typography.caption, color = colors.textWeaker)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoughtSoldBarCard(items: List<CategoryBars>) {
    val colors = FlipTheme.colors
    val maxValue = items.maxOfOrNull { maxOf(it.bought, it.sold) }?.coerceAtLeast(1) ?: 1
    FlipCard {
        Text("Bought vs sold", style = FlipTheme.typography.headingM, color = colors.textDefault)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(colors.info, "Bought")
            LegendDot(colors.success, "Sold")
        }
        Spacer(Modifier.height(12.dp))
        items.forEach { item ->
            Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Text(item.label, style = FlipTheme.typography.caption, color = colors.textWeaker)
                Row(Modifier.fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.weight(item.bought.coerceAtLeast(1).toFloat() / maxValue).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(colors.info))
                    Box(Modifier.weight(item.sold.coerceAtLeast(0).toFloat() / maxValue + .01f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(colors.success))
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, style = FlipTheme.typography.caption, color = FlipTheme.colors.textWeaker)
    }
}

@Composable
private fun AttentionRow(item: AttentionItem, onClick: () -> Unit) {
    FlipCard(onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBlob(color = FlipTheme.colors.accentAmberLight, size = 40.dp)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = FlipTheme.typography.headingS, color = FlipTheme.colors.textDefault)
                Text(item.subtitle, style = FlipTheme.typography.caption, color = FlipTheme.colors.textWeakest, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RecentSaleRow(sale: Sale) {
    FlipCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(sale.model, style = FlipTheme.typography.headingS, color = FlipTheme.colors.textDefault)
                Text("${sale.channel?.label ?: "—"} · ${sale.soldDate}", style = FlipTheme.typography.caption, color = FlipTheme.colors.textWeakest)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.formatSigned(sale.netProfitCents), style = FlipTheme.typography.headingS, color = FlipTheme.colors.success)
                Text(sale.margin.toPercentLabel(), style = FlipTheme.typography.caption, color = FlipTheme.colors.textWeakest)
            }
        }
    }
}
