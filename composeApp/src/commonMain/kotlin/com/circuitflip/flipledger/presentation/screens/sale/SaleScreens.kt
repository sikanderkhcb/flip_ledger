package com.circuitflip.flipledger.presentation.screens.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.domain.model.SalesChannel
import com.circuitflip.flipledger.domain.util.Money
import com.circuitflip.flipledger.domain.util.ProfitCalculator
import com.circuitflip.flipledger.domain.util.toPercentLabel
import com.circuitflip.flipledger.presentation.components.ChipGroup
import com.circuitflip.flipledger.presentation.components.FieldLabel
import com.circuitflip.flipledger.presentation.components.FlipCard
import com.circuitflip.flipledger.presentation.components.FlipTextField
import com.circuitflip.flipledger.presentation.components.LinkButton
import com.circuitflip.flipledger.presentation.components.PrimaryButton
import com.circuitflip.flipledger.presentation.components.ScreenScaffold
import com.circuitflip.flipledger.presentation.components.WizardHeader
import com.circuitflip.flipledger.presentation.theme.FlipTheme

/** 16 · Sale Step 1 — sale price, date, channel. */
@Composable
fun Sale1Screen(vm: SaleViewModel, onContinue: () -> Unit, onBack: () -> Unit) {
    val s by vm.state.collectAsState()
    SaleShell(1, "Record the sale", s.device?.let { "Invested ${Money.format(it.investedCents)}" }, onContinue, onBack, showBack = false) {
        FlipTextField(s.draft.price, vm::setPrice, "Sale price", placeholder = "0", keyboardType = KeyboardType.Decimal, currencyPrefix = true)
        Spacer(Modifier.height(16.dp))
        FlipTextField(s.draft.date, vm::setDate, "Sale date", placeholder = "YYYY-MM-DD")
        Spacer(Modifier.height(20.dp))
        FieldLabel("Sales channel")
        ChipGroup(SalesChannel.entries, s.draft.channel, { it.label }, vm::setChannel)
    }
}

/** 17 · Sale Step 2 — final selling costs (5 fee fields). */
@Composable
fun Sale2Screen(vm: SaleViewModel, onContinue: () -> Unit, onBack: () -> Unit) {
    val s by vm.state.collectAsState()
    SaleShell(2, "Any final selling costs?", "Adding these gives you your true net profit.", onContinue, onBack) {
        FlipTextField(s.draft.platformFee, vm::setPlatformFee, "Platform fee", placeholder = "0", keyboardType = KeyboardType.Decimal, currencyPrefix = true)
        Spacer(Modifier.height(16.dp))
        FlipTextField(s.draft.paymentFee, vm::setPaymentFee, "Payment processing fee", placeholder = "0", keyboardType = KeyboardType.Decimal, currencyPrefix = true)
        Spacer(Modifier.height(16.dp))
        FlipTextField(s.draft.shipping, vm::setShipping, "Shipping", placeholder = "0", keyboardType = KeyboardType.Decimal, currencyPrefix = true)
        Spacer(Modifier.height(16.dp))
        FlipTextField(s.draft.packaging, vm::setPackaging, "Packaging", placeholder = "0", keyboardType = KeyboardType.Decimal, currencyPrefix = true)
        Spacer(Modifier.height(16.dp))
        FlipTextField(s.draft.otherFee, vm::setOtherFee, "Other", placeholder = "0", keyboardType = KeyboardType.Decimal, currencyPrefix = true)
    }
}

/** 18 · Sale Step 3 — profit preview with full breakdown. */
@Composable
fun Sale3Screen(vm: SaleViewModel, onComplete: () -> Unit, onBack: () -> Unit) {
    val s by vm.state.collectAsState()
    val submitting by vm.submitting.collectAsState()
    val colors = FlipTheme.colors
    val device = s.device
    SaleShell(3, "Here's your result", null, continueLabel = "Complete Sale", loading = submitting, onContinue = { vm.complete(); onComplete() }, onBack = onBack) {
        FlipCard {
            Text("NET PROFIT", style = FlipTheme.typography.caption, color = colors.textWeakest)
            Spacer(Modifier.height(6.dp))
            Text(Money.format(s.previewNetProfitCents), style = FlipTheme.typography.displayM, color = if (s.previewNetProfitCents >= 0) colors.success else colors.error)
            Text("Margin ${s.previewMargin.toPercentLabel()}", style = FlipTheme.typography.bodyM, color = colors.textWeaker)
        }
        Spacer(Modifier.height(16.dp))
        if (device != null) {
            val breakdown = listOf(
                Triple("Sale revenue", Money.format(Money.parseToCents(s.draft.price)), colors.textDefault),
                Triple("Purchase cost", "-" + Money.format(device.purchasePriceCents), colors.error),
                Triple("Extra costs", "-" + Money.format(ProfitCalculator.extraCostsCents(device.costs)), colors.error),
                Triple("Fees", "-" + Money.format(ProfitCalculator.feesCents(s.draft)), colors.error),
                Triple("Net profit", Money.format(s.previewNetProfitCents), colors.success),
            )
            FlipCard {
                breakdown.forEachIndexed { i, (label, value, color) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, style = FlipTheme.typography.bodyM, color = colors.textWeaker)
                        Text(value, style = FlipTheme.typography.headingS, color = color)
                    }
                    if (i < breakdown.lastIndex) HorizontalDivider(color = colors.borderDefault)
                }
            }
        }
        s.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = FlipTheme.typography.bodyS, color = colors.error)
        }
    }
}

@Composable
private fun SaleShell(
    step: Int, title: String, subtitle: String?, onContinue: () -> Unit, onBack: () -> Unit,
    continueLabel: String = "Continue", showBack: Boolean = true, loading: Boolean = false,
    body: @Composable () -> Unit,
) {
    ScreenScaffold {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
            Spacer(Modifier.height(16.dp))
            WizardHeader(step, 3, title, subtitle)
            Spacer(Modifier.height(24.dp))
            body()
        }
        Column(Modifier.padding(24.dp)) {
            PrimaryButton(continueLabel, onContinue, loading = loading)
            if (showBack) { Spacer(Modifier.height(4.dp)); LinkButton("Back", onBack, Modifier.fillMaxWidth()) }
        }
    }
}
