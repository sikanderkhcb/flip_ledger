package com.blackink.app.presentation.screens.invoice

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

private const val PAGE_W = 595
private const val PAGE_H = 842
private const val LEFT = 48f
private const val LABEL_COL = 150f

private val DARK = Color.rgb(0x28, 0x28, 0x29)
private val GRAY = Color.rgb(0x8a, 0x8a, 0x99)
private val RULE = Color.rgb(0xd8, 0xd8, 0xdd)
private val RULE_LIGHT = Color.rgb(0xec, 0xec, 0xf1)

private fun textPaint(
    size: Float,
    color: Int,
    bold: Boolean = false,
    right: Boolean = false,
    spacing: Float = 0f,
) = Paint().apply {
    isAntiAlias = true
    textSize = size
    this.color = color
    typeface = Typeface.create(Typeface.SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    letterSpacing = spacing
    if (right) textAlign = Paint.Align.RIGHT
}

private fun linePaint(color: Int) = Paint().apply {
    this.color = color
    strokeWidth = 1f
    isAntiAlias = true
}

@Composable
actual fun rememberInvoicePdfExporter(): (InvoiceDocument) -> Unit {
    val context = LocalContext.current
    return { doc -> shareInvoice(context, doc) }
}

private fun shareInvoice(context: Context, doc: InvoiceDocument) {
    val right = PAGE_W - LEFT
    val pdf = PdfDocument()
    val page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
    val c = page.canvas

    // Header
    c.drawText("INVOICE", right, 92f, textPaint(38f, DARK, bold = true, right = true, spacing = 0.12f))
    c.drawText("#${doc.invoiceNumber}", right, 116f, textPaint(11f, GRAY, right = true))

    var y = 178f
    // Billed to
    c.drawText("BILLED TO:", LEFT, y, textPaint(11f, DARK, bold = true, spacing = 0.06f))
    c.drawText(doc.billedToName, LEFT + LABEL_COL, y, textPaint(12f, DARK))
    y += 18f
    doc.billedToDetails.forEach { detail ->
        c.drawText(detail, LEFT + LABEL_COL, y, textPaint(11f, GRAY))
        y += 16f
    }

    y += 12f
    // Pay to
    c.drawText("PAY TO:", LEFT, y, textPaint(11f, DARK, bold = true, spacing = 0.06f))
    c.drawText(doc.businessName.ifBlank { "—" }, LEFT + LABEL_COL, y, textPaint(12f, DARK))
    if (doc.ownerName.isNotBlank()) {
        y += 16f
        c.drawText(doc.ownerName, LEFT + LABEL_COL, y, textPaint(11f, GRAY))
    }
    y += 12f
    c.drawText("Date", LEFT, y + 18f, textPaint(11f, DARK, bold = true, spacing = 0.06f))
    c.drawText(doc.date, LEFT + LABEL_COL, y + 18f, textPaint(11f, GRAY))

    // Table header
    y += 64f
    c.drawText("DESCRIPTION", LEFT, y, textPaint(11f, DARK, bold = true, spacing = 0.06f))
    c.drawText("AMOUNT", right, y, textPaint(11f, DARK, bold = true, right = true, spacing = 0.06f))
    y += 10f
    c.drawLine(LEFT, y, right, y, linePaint(RULE))
    y += 26f

    // Rows
    doc.items.forEach { item ->
        c.drawText(item.description, LEFT, y, textPaint(12f, DARK))
        c.drawText(item.amount, right, y, textPaint(12f, DARK, right = true))
        if (item.meta.isNotBlank()) {
            y += 15f
            c.drawText(item.meta, LEFT, y, textPaint(10f, GRAY))
        }
        y += 16f
        c.drawLine(LEFT, y, right, y, linePaint(RULE_LIGHT))
        y += 26f
    }

    // Totals
    c.drawText("Sub-Total", right - 130f, y, textPaint(11f, GRAY, right = true))
    c.drawText(doc.subTotal, right, y, textPaint(11f, DARK, right = true))
    y += 34f
    c.drawText("TOTAL", LEFT, y, textPaint(16f, DARK, bold = true, spacing = 0.04f))
    c.drawText(doc.total, right, y, textPaint(16f, DARK, bold = true, right = true))

    // Footer
    c.drawText(doc.footerNote, LEFT, (PAGE_H - 70).toFloat(), textPaint(10f, GRAY))
    c.drawText("Thank you for your business.", LEFT, (PAGE_H - 52).toFloat(), textPaint(10f, GRAY))

    pdf.finishPage(page)

    val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
    val file = File(dir, "invoice-${doc.invoiceNumber}.pdf")
    file.outputStream().use { pdf.writeTo(it) }
    pdf.close()

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Invoice #${doc.invoiceNumber}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(send, "Share invoice").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
    )
}
