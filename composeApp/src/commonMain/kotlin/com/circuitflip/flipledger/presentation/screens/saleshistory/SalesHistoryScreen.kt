package com.circuitflip.flipledger.presentation.screens.saleshistory

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.domain.model.Sale
import com.circuitflip.flipledger.domain.util.Money
import com.circuitflip.flipledger.domain.util.toPercentLabel
import com.circuitflip.flipledger.presentation.components.FlipCard
import com.circuitflip.flipledger.presentation.components.FlipTopBar
import com.circuitflip.flipledger.presentation.components.StatCard
import com.circuitflip.flipledger.presentation.rememberViewModel
import com.circuitflip.flipledger.presentation.theme.FlipTheme

/** 20 · Sales History — month summary stats + a list of completed sales. */
@Composable
fun SalesHistoryScreen(onBack: (() -> Unit)?, onOpenSettlement: () -> Unit) {
    val vm = rememberViewModel<SalesHistoryViewModel>()
    val state by vm.state.collectAsState()
    val colors = FlipTheme.colors

    Box(Modifier.fillMaxSize().background(colors.backgroundSubtle)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            if (onBack != null) {
                FlipTopBar(title = "Sales", onBack = onBack)
            } else {
                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Sales", style = FlipTheme.typography.headingXl, color = colors.textDefault)
                }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 0.dp, 20.dp, 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            label = "NET PROFIT",
                            value = Money.format(state.netProfitCents),
                            caption = "This month",
                            captionColor = colors.success,
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            label = "AVG MARGIN",
                            value = state.avgMargin.toPercentLabel(),
                            caption = "Across ${state.summarySalesCount} sales this month",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                state.error?.let { message ->
                    item {
                        Text(message, style = FlipTheme.typography.bodyM, color = colors.error)
                    }
                }
                item {
                    FlipCard(onClick = onOpenSettlement) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Partner settlement", style = FlipTheme.typography.headingS, color = colors.textDefault)
                                Text("See who owes what this period", style = FlipTheme.typography.bodyS, color = colors.textWeaker)
                            }
                            Text("View", style = FlipTheme.typography.bodyM, color = colors.info)
                        }
                    }
                }
                if (state.sales.isEmpty()) {
                    item {
                        Text(
                            "No sales yet. Your completed sales will appear here.",
                            style = FlipTheme.typography.bodyM,
                            color = colors.textWeaker,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        )
                    }
                } else {
                    items(state.sales, key = { it.id }) { sale -> SaleHistoryRow(sale) }
                }
            }
        }
    }
}

@Composable
private fun SaleHistoryRow(sale: Sale) {
    val colors = FlipTheme.colors
    FlipCard(padding = 16.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(sale.model, style = FlipTheme.typography.headingS, color = colors.textDefault)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${sale.channel?.label ?: "—"} · ${sale.soldDate} · ${sale.daysHeld}d held",
                    style = FlipTheme.typography.bodyS,
                    color = colors.textWeaker,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Money.formatSigned(sale.netProfitCents),
                    style = FlipTheme.typography.headingS,
                    color = if (sale.netProfitCents >= 0) colors.success else colors.error,
                )
                Text(sale.margin.toPercentLabel() + " margin", style = FlipTheme.typography.caption, color = colors.textWeakest, textAlign = TextAlign.End)
            }
        }
    }
}
