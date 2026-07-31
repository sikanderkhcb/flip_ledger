package com.blackink.app.presentation.screens.subscription

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.blackink.app.domain.model.FREE_DEVICE_LIMIT
import com.blackink.app.domain.model.SubscriptionPlan
import com.blackink.app.domain.model.SubscriptionStatus
import com.blackink.app.domain.model.SubscriptionTier
import com.blackink.app.presentation.components.FlipTopBar
import com.blackink.app.presentation.components.PrimaryButton
import com.blackink.app.presentation.components.SecondaryButton
import com.blackink.app.presentation.components.UiErrorEffect
import com.blackink.app.presentation.theme.FlipTheme
import com.blackink.app.presentation.theme.Radius

@Composable
fun SubscriptionScreen(
    vm: SubscriptionViewModel,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val colors = FlipTheme.colors
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(state.externalUrl) {
        state.externalUrl?.let { url ->
            uriHandler.openUri(url)
            vm.consumeExternalUrl()
        }
    }
    val plans = listOf(
        SubscriptionTier.FREE to SubscriptionPlan(
            "Free",
            "$0",
            listOf(
                "$FREE_DEVICE_LIMIT lifetime device records",
                "Basic profit tracking",
            ),
        ),
        SubscriptionTier.SOLO to SubscriptionPlan(
            "Solo",
            "$10/mo",
            listOf(
                "Unlimited lifetime device records",
                "Full profit tracking",
                "CSV export",
            ),
        ),
    )
    val shouldManage = state.access.status in setOf(
        SubscriptionStatus.ACTIVE,
        SubscriptionStatus.TRIALING,
        SubscriptionStatus.PAST_DUE,
        SubscriptionStatus.UNPAID,
        SubscriptionStatus.PAUSED,
    )
    val displayedTier = if (state.access.isUnlimited) {
        state.access.tier
    } else {
        SubscriptionTier.FREE
    }

    Box(Modifier.fillMaxSize().background(colors.backgroundSubtle)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            FlipTopBar(title = "Subscription", onBack = onBack)
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
                return@Column
            }

            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    if (state.access.isUnlimited) {
                        "${state.access.tier.displayName} plan is active"
                    } else if (state.access.remainingFreeDevices == 0) {
                        "You've used all $FREE_DEVICE_LIMIT free device flips"
                    } else {
                        "Upgrade whenever you're ready"
                    },
                    style = FlipTheme.typography.headingXl,
                    color = colors.textDefault,
                )
                Text(
                    if (state.access.isUnlimited) {
                        "You can keep adding devices. Cancelling only blocks future additions after the paid period ends."
                    } else {
                        "${state.access.lifetimeDevicesCreated.coerceAtMost(FREE_DEVICE_LIMIT)} of $FREE_DEVICE_LIMIT free device flips used. Existing devices and sales always remain accessible."
                    },
                    style = FlipTheme.typography.bodyM,
                    color = colors.textWeaker,
                )

                plans.forEach { (tier, plan) ->
                    PlanCard(
                        plan = plan,
                        highlighted = tier == displayedTier,
                        actionText = when {
                            shouldManage || tier == SubscriptionTier.FREE -> null
                            tier == SubscriptionTier.SOLO -> "Choose Solo — $10/month"
                            else -> null
                        },
                        actionLoading = state.actionLoading && state.checkoutTier == tier,
                        actionsEnabled = !state.actionLoading,
                        primaryAction = tier == SubscriptionTier.SOLO,
                        onAction = { vm.startCheckout(tier) },
                    )
                }

                UiErrorEffect(state.error)

                if (shouldManage) {
                    PrimaryButton(
                        text = "Manage subscription",
                        onClick = vm::manageSubscription,
                        loading = state.actionLoading,
                    )
                }
                SecondaryButton(
                    text = "Refresh subscription status",
                    onClick = vm::refresh,
                    enabled = !state.actionLoading,
                )
                if (state.watchingReturn) {
                    Text(
                        "Updating subscription status…",
                        style = FlipTheme.typography.caption,
                        color = colors.textWeaker,
                    )
                }
                Text(
                    "Checkout opens in a secure browser window. Return here and your subscription status will update automatically.",
                    style = FlipTheme.typography.caption,
                    color = colors.textWeakest,
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    highlighted: Boolean,
    actionText: String?,
    actionLoading: Boolean,
    actionsEnabled: Boolean,
    primaryAction: Boolean,
    onAction: () -> Unit,
) {
    val colors = FlipTheme.colors
    val borderColor = if (highlighted) colors.primary else colors.borderDefault
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(colors.backgroundDefault)
            .border(
                BorderStroke(if (highlighted) 2.dp else 1.dp, borderColor),
                RoundedCornerShape(Radius.card),
            )
            .padding(18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(plan.name, style = FlipTheme.typography.headingM, color = colors.textDefault)
            Text(
                plan.price,
                style = FlipTheme.typography.headingM,
                color = if (highlighted) colors.primary else colors.textDefault,
            )
        }
        Spacer(Modifier.height(12.dp))
        plan.features.forEach { feature ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.success),
                )
                Text(
                    feature,
                    style = FlipTheme.typography.bodyM,
                    color = colors.textWeaker,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
        actionText?.let { text ->
            Spacer(Modifier.height(14.dp))
            if (primaryAction) {
                PrimaryButton(
                    text = text,
                    onClick = onAction,
                    enabled = actionsEnabled,
                    loading = actionLoading,
                )
            } else {
                SecondaryButton(
                    text = text,
                    onClick = onAction,
                    enabled = actionsEnabled,
                )
            }
        }
    }
}

private val SubscriptionTier.displayName: String
    get() = when (this) {
        SubscriptionTier.FREE -> "Free"
        SubscriptionTier.SOLO -> "Solo"
        SubscriptionTier.PARTNER -> "Partner"
    }
