package com.circuitflip.flipledger.presentation.screens.reports

import androidx.compose.runtime.Composable

/**
 * Returns a callback that shares/exports the given CSV [content] via the platform share sheet.
 * [fileName] is used as the suggested name/subject.
 */
@Composable
expect fun rememberCsvExporter(): (fileName: String, content: String) -> Unit
