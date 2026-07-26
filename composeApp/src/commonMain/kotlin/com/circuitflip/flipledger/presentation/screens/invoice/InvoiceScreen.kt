package com.circuitflip.flipledger.presentation.screens.invoice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.domain.model.Sale
import com.circuitflip.flipledger.domain.util.Money
import com.circuitflip.flipledger.presentation.components.FlipCard
import com.circuitflip.flipledger.presentation.components.FlipTopBar
import com.circuitflip.flipledger.presentation.components.PrimaryButton
import com.circuitflip.flipledger.presentation.components.ScreenScaffold
import com.circuitflip.flipledger.presentation.screens.reports.rememberCsvExporter
import com.circuitflip.flipledger.presentation.theme.FlipTheme

@Composable
fun InvoiceScreen(sale: Sale, onBack: () -> Unit) {
    val colors = FlipTheme.colors
    val export = rememberCsvExporter()
    val invoiceText = invoiceText(sale)
    ScreenScaffold {
        FlipTopBar(title = "Invoice", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("INVOICE", style = FlipTheme.typography.caption, color = colors.primary)
            Text("FlipLedger", style = FlipTheme.typography.displayM, color = colors.textDefault)
            Text("Invoice ${sale.id.takeLast(8)} · ${sale.soldDate}", style = FlipTheme.typography.bodyM, color = colors.textWeaker)
            Spacer(Modifier.padding(8.dp))
            FlipCard(Modifier.fillMaxWidth()) {
                Text("BILL TO", style = FlipTheme.typography.caption, color = colors.textWeakest)
                Text(sale.customerName.ifBlank { "Customer not provided" }, style = FlipTheme.typography.headingM, color = colors.textDefault)
                sale.customerEmail.takeIf { it.isNotBlank() }?.let { Text(it, style = FlipTheme.typography.bodyM, color = colors.textWeaker) }
                sale.customerPhone.takeIf { it.isNotBlank() }?.let { Text(it, style = FlipTheme.typography.bodyM, color = colors.textWeaker) }
                sale.customerAddress.takeIf { it.isNotBlank() }?.let { Text(it, style = FlipTheme.typography.bodyM, color = colors.textWeaker) }
            }
            Spacer(Modifier.padding(8.dp))
            FlipCard(Modifier.fillMaxWidth()) {
                invoiceRow("Device", sale.model)
                invoiceRow("Sale price", Money.format(sale.revenueCents))
                invoiceRow("Fees", Money.format(sale.feesCents))
                invoiceRow("Total", Money.format(sale.revenueCents))
            }
            Spacer(Modifier.padding(8.dp))
        }
        Column(Modifier.padding(20.dp)) {
            PrimaryButton("Share invoice", { export("invoice-${sale.id}.txt", invoiceText) })
        }
    }
}

@Composable
private fun invoiceRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = FlipTheme.typography.bodyM, color = FlipTheme.colors.textWeaker)
        Text(value, style = FlipTheme.typography.headingS, color = FlipTheme.colors.textDefault)
    }
}

private fun invoiceText(sale: Sale): String = buildString {
    appendLine("════════════════════════════════")
    appendLine("           FLIPLEDGER")
    appendLine("              INVOICE")
    appendLine("════════════════════════════════")
    appendLine("Invoice #  ${sale.id.takeLast(8)}")
    appendLine("Date       ${sale.soldDate}")
    appendLine()
    appendLine("BILL TO")
    appendLine(sale.customerName.ifBlank { "Customer not provided" })
    if (sale.customerEmail.isNotBlank()) appendLine(sale.customerEmail)
    if (sale.customerPhone.isNotBlank()) appendLine(sale.customerPhone)
    if (sale.customerAddress.isNotBlank()) appendLine(sale.customerAddress)
    appendLine()
    appendLine("ITEM")
    appendLine("${sale.model}  ${Money.format(sale.revenueCents)}")
    appendLine("Fees       ${Money.format(sale.feesCents)}")
    appendLine("────────────────────────────────")
    appendLine("TOTAL      ${Money.format(sale.revenueCents)}")
    appendLine("════════════════════════════════")
}
