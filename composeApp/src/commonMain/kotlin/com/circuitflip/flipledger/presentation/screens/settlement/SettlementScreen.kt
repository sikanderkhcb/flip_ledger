package com.circuitflip.flipledger.presentation.screens.settlement

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
import com.circuitflip.flipledger.domain.model.Settlement
import com.circuitflip.flipledger.domain.util.Money
import com.circuitflip.flipledger.presentation.components.FlipCard
import com.circuitflip.flipledger.presentation.components.FlipTopBar
import com.circuitflip.flipledger.presentation.components.SectionLabel
import com.circuitflip.flipledger.presentation.rememberViewModel
import com.circuitflip.flipledger.presentation.theme.FlipTheme

/** 21 · Partner Settlement — total profit, split, amount owed, and recent activity. */
@Composable
fun SettlementScreen(onBack: () -> Unit) {
    val vm = rememberViewModel<SettlementViewModel>()
    val state by vm.state.collectAsState()
    val colors = FlipTheme.colors
    val s: Settlement? = state.settlement

    Box(Modifier.fillMaxSize().background(colors.backgroundSubtle)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            FlipTopBar(title = "Partner Settlement", onBack = onBack)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Amount owed hero
                FlipCard {
                    SectionLabel("AMOUNT OWED TO ${state.profile.partnerName.uppercase()}")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        Money.format(s?.owedCents ?: 0),
                        style = FlipTheme.typography.displayM,
                        color = colors.warning,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Settle up to keep the books clean.", style = FlipTheme.typography.bodyS, color = colors.textWeaker)
                }
                // Split breakdown
                FlipCard {
                    SplitRow("Total business profit", Money.format(s?.totalProfitCents ?: 0), colors.textDefault)
                    Spacer(Modifier.height(10.dp))
                    SplitRow("You (${state.profile.splitYou}%)", Money.format(s?.yourShareCents ?: 0), colors.textDefault)
                    Spacer(Modifier.height(10.dp))
                    SplitRow("${state.profile.partnerName} (${state.profile.splitPartner}%)", Money.format(s?.partnerShareCents ?: 0), colors.textDefault)
                }
                // Activity
                SectionLabel("RECENT SETTLEMENT ACTIVITY", Modifier.padding(top = 4.dp))
                FlipCard {
                    val activity = s?.activity.orEmpty()
                    if (activity.isEmpty()) {
                        Text("No settlement activity yet.", style = FlipTheme.typography.bodyM, color = colors.textWeaker)
                    } else {
                        activity.forEachIndexed { i, a ->
                            if (i > 0) Spacer(Modifier.height(10.dp))
                            SplitRow(a.label, Money.formatSigned(a.amountCents), if (a.amountCents >= 0) colors.success else colors.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = FlipTheme.typography.bodyM, color = FlipTheme.colors.textWeaker)
        Text(value, style = FlipTheme.typography.headingS, color = valueColor)
    }
}
