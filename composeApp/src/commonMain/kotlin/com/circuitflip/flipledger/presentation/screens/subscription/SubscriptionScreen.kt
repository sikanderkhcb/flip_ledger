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
import com.circuitflip.flipledger.domain.model.FREE_DEVICE_LIMIT
import com.circuitflip.flipledger.domain.model.SubscriptionStatus
import com.circuitflip.flipledger.presentation.components.FlipTopBar
import com.circuitflip.flipledger.presentation.components.PrimaryButton
import com.circuitflip.flipledger.presentation.components.SecondaryButton
import com.circuitflip.flipledger.presentation.theme.FlipTheme
import com.circuitflip.flipledger.presentation.theme.Radius

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
                        "Unlimited devices are active"
                    } else if (state.access.remainingFreeDevices == 0) {
                        "You've used all 10 free device flips"
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

                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.card))
                        .background(colors.backgroundDefault)
                        .border(
                            BorderStroke(2.dp, colors.primary),
                            RoundedCornerShape(Radius.card),
                        )
                        .padding(20.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Unlimited",
                            style = FlipTheme.typography.headingM,
                            color = colors.textDefault,
                        )
                        Text(
                            "$10/month",
                            style = FlipTheme.typography.headingM,
                            color = colors.primary,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    listOf(
                        "Unlimited lifetime device records",
                        "Record sales for every existing device",
                        "Full profit tracking and CSV export",
                        "Cancel anytime",
                    ).forEach { feature ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(7.dp)
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
                }

                state.error?.let { message ->
                    Text(
                        message,
                        style = FlipTheme.typography.bodyM,
                        color = colors.error,
                    )
                }

                val shouldManage = state.access.status in setOf(
                    SubscriptionStatus.ACTIVE,
                    SubscriptionStatus.TRIALING,
                    SubscriptionStatus.PAST_DUE,
                    SubscriptionStatus.UNPAID,
                    SubscriptionStatus.PAUSED,
                )
                PrimaryButton(
                    text = if (shouldManage) {
                        "Manage subscription"
                    } else {
                        "Upgrade for $10/month"
                    },
                    onClick = if (shouldManage) {
                        vm::manageSubscription
                    } else {
                        vm::startCheckout
                    },
                    loading = state.actionLoading,
                )
                SecondaryButton(
                    text = "Refresh subscription status",
                    onClick = vm::refresh,
                    enabled = !state.actionLoading,
                )
                Text(
                    "Checkout opens in your browser. Return here and refresh after payment.",
                    style = FlipTheme.typography.caption,
                    color = colors.textWeakest,
                )
            }
        }
    }
}
