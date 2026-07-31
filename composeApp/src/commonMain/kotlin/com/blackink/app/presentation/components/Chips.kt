package com.blackink.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.blackink.app.presentation.theme.FlipTheme

/**
 * Selectable pill chip used throughout forms (source, condition, channel, cost type, etc.).
 * Selected state mirrors the reference: primary fill + inverse text; unselected is a
 * bordered surface chip.
 */
@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FlipTheme.colors
    Text(
        text = label,
        style = FlipTheme.typography.bodyM,
        color = if (selected) colors.textInverse else colors.textWeaker,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) colors.primary else colors.backgroundDefault)
            .border(
                BorderStroke(1.dp, if (selected) colors.primary else colors.borderDefault),
                RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/** Wrapping row of selectable chips bound to an enum-like option list. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipGroup(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            SelectableChip(
                label = label(option),
                selected = option == selected,
                onClick = { onSelect(option) },
            )
        }
    }
}
