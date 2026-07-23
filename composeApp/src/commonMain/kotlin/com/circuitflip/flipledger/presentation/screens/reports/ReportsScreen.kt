package com.circuitflip.flipledger.presentation.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.presentation.components.FlipCard
import com.circuitflip.flipledger.presentation.components.FlipTopBar
import com.circuitflip.flipledger.presentation.components.SecondaryButton
import com.circuitflip.flipledger.presentation.components.SectionLabel
import com.circuitflip.flipledger.presentation.rememberViewModel
import com.circuitflip.flipledger.presentation.theme.FlipTheme

/** 22 · Reports — period summary metrics with CSV export affordances. */
@Composable
fun ReportsScreen(onBack: () -> Unit) {
    val vm = rememberViewModel<ReportsViewModel>()
    val metrics by vm.metrics.collectAsState()
    val error by vm.error.collectAsState()
    val colors = FlipTheme.colors
    val exportCsv = rememberCsvExporter()

    Box(Modifier.fillMaxSize().background(colors.backgroundSubtle)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            FlipTopBar(title = "Reports", onBack = onBack)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(vm.periodLabel, style = FlipTheme.typography.headingM, color = colors.textDefault)
                    Text("Current month", style = FlipTheme.typography.bodyM, color = colors.textWeaker)
                }
                SectionLabel("SUMMARY")
                error?.let {
                    Text(it, style = FlipTheme.typography.bodyM, color = colors.error)
                }
                FlipCard {
                    if (metrics.isEmpty()) {
                        Text("No data for this period yet.", style = FlipTheme.typography.bodyM, color = colors.textWeaker)
                    } else {
                        metrics.forEachIndexed { i, m ->
                            if (i > 0) Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(m.label, style = FlipTheme.typography.bodyM, color = colors.textWeaker)
                                Text(m.displayValue, style = FlipTheme.typography.headingS, color = colors.textDefault)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                SecondaryButton(text = "Export CSV", onClick = { exportCsv("flipledger-report.csv", vm.buildCsv()) })
                Text(
                    "Download history exports every sale and cost for the selected period as a CSV file.",
                    style = FlipTheme.typography.bodyS,
                    color = colors.textWeakest,
                )
            }
        }
    }
}
