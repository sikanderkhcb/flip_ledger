package com.blackink.app.presentation.screens.invoice

import androidx.compose.runtime.Composable

/** One row of the invoice's line-item table. [meta] is a small sub-line (e.g. sale date / channel). */
data class InvoiceLineItem(
    val description: String,
    val meta: String,
    val amount: String,
)

/** Everything the PDF renderer needs — already formatted strings, so platforms just lay it out. */
data class InvoiceDocument(
    val businessName: String,
    val ownerName: String,
    val invoiceNumber: String,
    val date: String,
    val billedToName: String,
    val billedToDetails: List<String>,
    val items: List<InvoiceLineItem>,
    val subTotal: String,
    val total: String,
    val footerNote: String,
)

/**
 * Returns a callback that renders [InvoiceDocument] to a PDF matching the invoice template
 * (INVOICE title, BILLED TO / PAY TO, line-item table, sub-total/total, footer) and opens the
 * platform share sheet. Android draws with `PdfDocument`; iOS draws with UIKit's PDF context.
 */
@Composable
expect fun rememberInvoicePdfExporter(): (InvoiceDocument) -> Unit
