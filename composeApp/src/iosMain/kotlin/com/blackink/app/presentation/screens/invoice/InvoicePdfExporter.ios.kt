package com.blackink.app.presentation.screens.invoice

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsBeginPDFContextToFile
import platform.UIKit.UIGraphicsBeginPDFPageWithInfo
import platform.UIKit.UIGraphicsEndPDFContext
import platform.UIKit.UIRectFill
import platform.UIKit.drawAtPoint
import platform.UIKit.sizeWithAttributes

private const val PAGE_W = 595.0
private const val PAGE_H = 842.0
private const val LEFT = 48.0
private const val LABEL_COL = 150.0

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberInvoicePdfExporter(): (InvoiceDocument) -> Unit = { doc ->
    val path = NSTemporaryDirectory() + "invoice-${doc.invoiceNumber}.pdf"
    val bounds = CGRectMake(0.0, 0.0, PAGE_W, PAGE_H)
    val right = PAGE_W - LEFT

    val dark = UIColor.colorWithRed(0x28 / 255.0, 0x28 / 255.0, 0x29 / 255.0, alpha = 1.0)
    val gray = UIColor.colorWithRed(0x8a / 255.0, 0x8a / 255.0, 0x99 / 255.0, alpha = 1.0)
    val rule = UIColor.colorWithRed(0xd8 / 255.0, 0xd8 / 255.0, 0xdd / 255.0, alpha = 1.0)
    val ruleLight = UIColor.colorWithRed(0xec / 255.0, 0xec / 255.0, 0xf1 / 255.0, alpha = 1.0)

    fun attrs(size: Double, color: UIColor, bold: Boolean): Map<Any?, *> = mapOf<Any?, Any?>(
        NSFontAttributeName to if (bold) UIFont.boldSystemFontOfSize(size) else UIFont.systemFontOfSize(size),
        NSForegroundColorAttributeName to color,
    )

    fun draw(text: String, x: Double, y: Double, size: Double, color: UIColor, bold: Boolean = false) {
        (text as NSString).drawAtPoint(CGPointMake(x, y), attrs(size, color, bold))
    }

    fun drawRight(text: String, xRight: Double, y: Double, size: Double, color: UIColor, bold: Boolean = false) {
        val a = attrs(size, color, bold)
        val width = (text as NSString).sizeWithAttributes(a).useContents { width }
        (text as NSString).drawAtPoint(CGPointMake(xRight - width, y), a)
    }

    fun separator(y: Double, color: UIColor) {
        color.setFill()
        UIRectFill(CGRectMake(LEFT, y, right - LEFT, 1.0))
    }

    UIGraphicsBeginPDFContextToFile(path, bounds, null)
    UIGraphicsBeginPDFPageWithInfo(bounds, null)

    // Header (drawAtPoint places text by its top-left corner)
    drawRight("INVOICE", right, 44.0, 34.0, dark, bold = true)
    drawRight("#${doc.invoiceNumber}", right, 88.0, 11.0, gray)

    var y = 150.0
    draw("BILLED TO:", LEFT, y, 11.0, dark, bold = true)
    draw(doc.billedToName, LEFT + LABEL_COL, y, 12.0, dark)
    y += 20.0
    doc.billedToDetails.forEach { detail ->
        draw(detail, LEFT + LABEL_COL, y, 11.0, gray)
        y += 16.0
    }

    y += 12.0
    draw("PAY TO:", LEFT, y, 11.0, dark, bold = true)
    draw(doc.businessName.ifBlank { "—" }, LEFT + LABEL_COL, y, 12.0, dark)
    if (doc.ownerName.isNotBlank()) {
        y += 16.0
        draw(doc.ownerName, LEFT + LABEL_COL, y, 11.0, gray)
    }
    y += 20.0
    draw("Date:", LEFT, y, 11.0, dark, bold = true)
    draw(doc.date, LEFT + LABEL_COL, y, 11.0, gray)

    y += 44.0
    draw("DESCRIPTION", LEFT, y, 11.0, dark, bold = true)
    drawRight("AMOUNT", right, y, 11.0, dark, bold = true)
    y += 20.0
    separator(y, rule)
    y += 12.0

    doc.items.forEach { item ->
        draw(item.description, LEFT, y, 12.0, dark)
        drawRight(item.amount, right, y, 12.0, dark)
        if (item.meta.isNotBlank()) {
            y += 16.0
            draw(item.meta, LEFT, y, 10.0, gray)
        }
        y += 20.0
        separator(y, ruleLight)
        y += 12.0
    }

    y += 6.0
    draw("Sub-Total", right - 160.0, y, 11.0, gray)
    drawRight(doc.subTotal, right, y, 11.0, dark)
    y += 30.0
    draw("TOTAL", LEFT, y, 16.0, dark, bold = true)
    drawRight(doc.total, right, y, 16.0, dark, bold = true)

    draw(doc.footerNote, LEFT, PAGE_H - 76.0, 10.0, gray)
    draw("Thank you for your business.", LEFT, PAGE_H - 58.0, 10.0, gray)

    UIGraphicsEndPDFContext()

    val controller = UIActivityViewController(
        activityItems = listOf(NSURL.fileURLWithPath(path)),
        applicationActivities = null,
    )
    UIApplication.sharedApplication.keyWindow?.rootViewController
        ?.presentViewController(controller, animated = true, completion = null)
}
