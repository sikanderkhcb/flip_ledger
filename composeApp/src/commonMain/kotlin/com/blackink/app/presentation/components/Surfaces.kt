package com.blackink.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blackink.app.presentation.theme.FlipTheme
import com.blackink.app.presentation.theme.Radius
import com.blackink.app.presentation.theme.Spacing

/** White surface card with subtle border — the base container for most content. */
@Composable
fun FlipCard(
    modifier: Modifier = Modifier,
    padding: Dp = Spacing.x400,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = FlipTheme.colors
    val borderColor = if (selected) colors.primary else colors.borderDefault
    val borderWidth = if (selected) 2.dp else 1.dp
    var base = modifier
        .clip(RoundedCornerShape(Radius.card))
        .background(colors.backgroundDefault)
        .border(BorderStroke(borderWidth, borderColor), RoundedCornerShape(Radius.card))
    if (onClick != null) base = base.clickable(onClick = onClick)
    Column(modifier = base.padding(padding), content = content)
}

/** Uppercase section header (e.g. "Recent sales", "CURRENCY"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = FlipTheme.typography.caption.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(0.4f, androidx.compose.ui.unit.TextUnitType.Sp)),
        color = FlipTheme.colors.textWeakest,
        modifier = modifier,
    )
}

/** Rounded colored square holding a small icon/glyph — used on category cards, settings rows. */
@Composable
fun IconBlob(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    content: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(Radius.sm)).background(color),
        contentAlignment = Alignment.Center,
    ) { content() }
}
