package com.circuitflip.flipledger.presentation.screens.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.domain.model.Currency
import com.circuitflip.flipledger.domain.model.WorkspaceType
import com.circuitflip.flipledger.presentation.components.ChipGroup
import com.circuitflip.flipledger.presentation.components.FieldLabel
import com.circuitflip.flipledger.presentation.components.FlipCard
import com.circuitflip.flipledger.presentation.components.FlipTextField
import com.circuitflip.flipledger.presentation.components.LinkButton
import com.circuitflip.flipledger.presentation.components.PrimaryButton
import com.circuitflip.flipledger.presentation.components.ScreenScaffold
import com.circuitflip.flipledger.presentation.components.WizardHeader
import com.circuitflip.flipledger.presentation.theme.FlipTheme

/** 04 · Setup 1 — choose Solo vs Partner workspace. */
@Composable
fun Setup1Screen(vm: SetupViewModel, onContinue: () -> Unit, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    SetupShell(step = 1, title = "Let's set up your workspace", subtitle = "Takes less than a minute.", onContinue = onContinue, onBack = onBack) {
        listOf(
            Triple(WorkspaceType.SOLO, "Solo reseller", "Just you — track your own devices and profit."),
            Triple(WorkspaceType.PARTNER, "Partner business", "Split costs, profit, and settlements with a partner."),
        ).forEach { (type, title, desc) ->
            val selected = state.workspaceType == type
            FlipCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                selected = selected,
                onClick = { vm.setWorkspace(type) },
            ) {
                Text(title, style = FlipTheme.typography.headingM, color = if (selected) FlipTheme.colors.primary else FlipTheme.colors.textDefault)
                Spacer(Modifier.height(4.dp))
                Text(desc, style = FlipTheme.typography.bodyM, color = FlipTheme.colors.textWeaker)
            }
        }
    }
}

/** 05 · Setup 2 — name the business. */
@Composable
fun Setup2Screen(vm: SetupViewModel, onContinue: () -> Unit, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    SetupShell(step = 2, title = "What should we call your business?", subtitle = "This name organizes your inventory, reports, and partner settlements.", onContinue = onContinue, onBack = onBack) {
        FlipTextField(state.businessName, vm::setBusinessName, "Business name", placeholder = "e.g. Circuit Flip Co.")
        Spacer(Modifier.height(16.dp))
        FlipCard {
            FieldLabel("Preview")
            Text(state.businessName.ifBlank { "Circuit Flip Co." }, style = FlipTheme.typography.headingL, color = FlipTheme.colors.textDefault)
        }
    }
}

/** 06 · Setup 3 — currency, partner split, category preference. */
@Composable
fun Setup3Screen(vm: SetupViewModel, onFinish: () -> Unit, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    SetupShell(
        step = 3,
        title = "A few business preferences",
        subtitle = "These preferences are saved to your workspace.",
        continueLabel = "Finish Setup",
        loading = state.loading,
        onContinue = { vm.finish(onFinish) },
        onBack = onBack,
    ) {
        FieldLabel("Currency")
        ChipGroup(Currency.entries, state.currency, { it.code }, vm::setCurrency)
        Spacer(Modifier.height(20.dp))
        if (state.workspaceType == WorkspaceType.PARTNER) {
            FlipTextField(state.partnerName, vm::setPartnerName, "Partner name", placeholder = "Partner")
            Spacer(Modifier.height(20.dp))
            FieldLabel("Default profit split (partner mode)")
            Text("You ${state.splitYou}%", style = FlipTheme.typography.headingM, color = FlipTheme.colors.textDefault)
            Slider(value = state.splitYou.toFloat(), onValueChange = { vm.setSplit((it / 5).toInt() * 5) }, valueRange = 0f..100f, steps = 19)
            Spacer(Modifier.height(20.dp))
        }
        FieldLabel("Primary resale category")
        val cats = listOf("phones" to "Phones", "laptops" to "Laptops", "tablets" to "Tablets", "gaming" to "Gaming", "mixed" to "Mixed")
        ChipGroup(cats, cats.firstOrNull { it.first == state.categoryPref }, { it.second }, { vm.setCategoryPref(it.first) })
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = FlipTheme.typography.bodyM, color = FlipTheme.colors.error)
        }
    }
}

/** Shared shell for the three setup steps: wizard header + scrollable body + footer buttons. */
@Composable
private fun SetupShell(
    step: Int,
    title: String,
    subtitle: String,
    continueLabel: String = "Continue",
    loading: Boolean = false,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    body: @Composable () -> Unit,
) {
    ScreenScaffold {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
            Spacer(Modifier.height(16.dp))
            WizardHeader(step = step, totalSteps = 3, title = title, subtitle = subtitle)
            Spacer(Modifier.height(24.dp))
            body()
        }
        Column(Modifier.padding(24.dp)) {
            PrimaryButton(continueLabel, onContinue, loading = loading)
            if (step > 1) {
                Spacer(Modifier.height(4.dp))
                LinkButton("Back", onBack, Modifier.fillMaxWidth())
            }
        }
    }
}
