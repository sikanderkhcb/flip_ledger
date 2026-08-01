package com.blackink.app.presentation.screens.sale

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blackink.app.domain.model.Sale
import com.blackink.app.domain.util.Money
import com.blackink.app.domain.util.toPercentLabel
import com.blackink.app.presentation.components.FlipCard
import com.blackink.app.presentation.components.PrimaryButton
import com.blackink.app.presentation.components.SecondaryButton
import com.blackink.app.presentation.theme.FlipTheme

/** 19 · Sale Complete — celebratory result + margin/days/invested/channel + follow-ups. */
@Composable
fun SaleCompleteScreen(sale: Sale?, onViewSales: () -> Unit, onAddAnother: () -> Unit, onDashboard: () -> Unit, onInvoice: () -> Unit) {
    val colors = FlipTheme.colors
    Box(Modifier.fillMaxSize().background(colors.backgroundSubtle), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Sale recorded", style = FlipTheme.typography.displayM, color = colors.textDefault)
            Spacer(Modifier.height(8.dp))
            Text(Money.formatSigned(sale?.netProfitCents ?: 0), style = FlipTheme.typography.displayM, color = colors.success)
            Spacer(Modifier.height(24.dp))
            FlipCard(Modifier.fillMaxWidth()) {
                statRow("Margin", (sale?.margin ?: 0.0).toPercentLabel())
                statRow("Days held", "${sale?.daysHeld ?: 0} days")
                statRow("Total invested", Money.format(sale?.costCents ?: 0))
                statRow("Channel", sale?.channel?.label ?: "—")
            }
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Go to dashboard", onDashboard)
            Spacer(Modifier.height(10.dp))
            SecondaryButton("View sales", onViewSales)
            Spacer(Modifier.height(10.dp))
            SecondaryButton("View invoice", onInvoice)
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Add another sale", onAddAnother)
        }
    }
}

@Composable
private fun statRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = FlipTheme.typography.bodyM, color = FlipTheme.colors.textWeaker, modifier = Modifier)
        Spacer(Modifier.size(24.dp))
        Text(value, style = FlipTheme.typography.headingS, color = FlipTheme.colors.textDefault)
    }
}
