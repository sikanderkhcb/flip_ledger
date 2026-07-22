package com.circuitflip.flipledger.presentation.screens.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.domain.model.SubscriptionPlan
import com.circuitflip.flipledger.presentation.components.PrimaryButton
import com.circuitflip.flipledger.presentation.components.FlipTopBar
import com.circuitflip.flipledger.presentation.theme.FlipTheme
import com.circuitflip.flipledger.presentation.theme.Radius

/**
 * 23 · Subscription — the three plan tiers from the reference (Free / Solo Pro / Partner Pro).
 * Plans are static content, so they live here rather than in a ViewModel.
 */
@Composable
fun SubscriptionScreen(onBack: () -> Unit, onStartTrial: () -> Unit) {
    val colors = FlipTheme.colors
    val plans = listOf(
        SubscriptionPlan("Free", "$0", listOf("Up to 10 active devices", "Basic profit tracking")),
        SubscriptionPlan("Solo Pro", "$9/mo", listOf("Unlimited devices", "Full profit tracking", "CSV export"), highlighted = true),
        SubscriptionPlan("Partner Pro", "$19/mo", listOf("Everything in Solo Pro", "Partner settlements", "Evidence vault")),
    )

    Box(Modifier.fillMaxSize().background(colors.backgroundSubtle)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            FlipTopBar(title = "Subscription", onBack = onBack)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Upgrade your reseller workflow", style = FlipTheme.typography.headingXl, color = colors.textDefault)
                Text("Pick a plan that fits how you flip.", style = FlipTheme.typography.bodyM, color = colors.textWeaker)
                Spacer(Modifier.height(4.dp))
                plans.forEach { plan -> PlanCard(plan) }
                Spacer(Modifier.height(4.dp))
                PrimaryButton(text = "Start Free Trial", onClick = onStartTrial)
            }
        }
    }
}

@Composable
private fun PlanCard(plan: SubscriptionPlan) {
    val colors = FlipTheme.colors
    val borderColor = if (plan.highlighted) colors.primary else colors.borderDefault
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(colors.backgroundDefault)
            .border(BorderStroke(if (plan.highlighted) 2.dp else 1.dp, borderColor), RoundedCornerShape(Radius.card))
            .padding(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(plan.name, style = FlipTheme.typography.headingM, color = colors.textDefault)
            Text(plan.price, style = FlipTheme.typography.headingM, color = if (plan.highlighted) colors.primary else colors.textDefault)
        }
        Spacer(Modifier.height(12.dp))
        plan.features.forEach { feature ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(999.dp)).background(colors.success))
                Text(feature, style = FlipTheme.typography.bodyM, color = colors.textWeaker, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}
