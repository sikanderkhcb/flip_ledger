package com.blackink.app.presentation.screens.reports

import androidx.compose.runtime.Composable
import platform.Foundation.NSString
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@Composable
actual fun rememberCsvExporter(): (fileName: String, content: String) -> Unit = { _, content ->
    val controller = UIActivityViewController(
        activityItems = listOf(content as NSString),
        applicationActivities = null,
    )
    UIApplication.sharedApplication.keyWindow?.rootViewController
        ?.presentViewController(controller, animated = true, completion = null)
}
