package com.blackink.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blackink.app.domain.util.Dates
import com.blackink.app.presentation.theme.FlipTheme
import com.blackink.app.presentation.theme.Radius
import com.blackink.app.presentation.theme.Spacing
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private fun friendly(date: LocalDate): String = "${MONTHS[date.monthNumber - 1]} ${date.dayOfMonth}, ${date.year}"

/**
 * A labeled, tappable date field matching [FlipTextField]'s look. Tapping opens a Material3
 * date picker; [value] is an ISO `yyyy-MM-dd` string and [onValueChange] receives the same.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    val colors = FlipTheme.colors
    var showPicker by remember { mutableStateOf(false) }
    val parsed = Dates.parseIso(value)

    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(label)
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.backgroundDefault, RoundedCornerShape(Radius.input))
                .border(
                    1.dp,
                    if (error != null) colors.error else colors.borderDefault,
                    RoundedCornerShape(Radius.input),
                )
                .clickable { showPicker = true }
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                parsed?.let(::friendly) ?: "Select a date",
                style = FlipTheme.typography.bodyL,
                color = if (parsed != null) colors.textDefault else colors.textWeakest,
            )
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = colors.textWeakest)
        }
        if (error != null) {
            Text(error, style = FlipTheme.typography.caption, color = colors.error, modifier = Modifier.padding(top = Spacing.x100))
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = parsed?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date
                        onValueChange(date.toString())
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
