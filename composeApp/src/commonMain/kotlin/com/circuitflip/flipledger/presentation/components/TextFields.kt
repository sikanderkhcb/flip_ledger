package com.circuitflip.flipledger.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.presentation.theme.FlipTheme
import com.circuitflip.flipledger.presentation.theme.Radius
import com.circuitflip.flipledger.presentation.theme.Spacing

/** Small uppercase field label used above every input (e.g. "EMAIL", "PURCHASE PRICE"). */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = FlipTheme.typography.caption,
        color = FlipTheme.colors.textWeakest,
        modifier = modifier.padding(bottom = Spacing.x150),
    )
}

/**
 * Labeled text input matching the design: uppercase label, 8px-radius bordered box,
 * optional leading currency symbol, error text below.
 */
@Composable
fun FlipTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    currencyPrefix: Boolean = false,
    error: String? = null,
) {
    val colors = FlipTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(label)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.backgroundDefault, RoundedCornerShape(Radius.input))
                .border(
                    1.dp,
                    if (error != null) colors.error else colors.borderDefault,
                    RoundedCornerShape(Radius.input),
                )
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            if (currencyPrefix) {
                Text("$", style = FlipTheme.typography.bodyL, color = colors.textWeakest, modifier = Modifier.padding(end = 6.dp))
            }
            Box(Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = FlipTheme.typography.bodyL, color = colors.textWeakest)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.merge(FlipTheme.typography.bodyL).copy(color = colors.textDefault),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                )
            }
        }
        if (error != null) {
            Text(error, style = FlipTheme.typography.caption, color = colors.error, modifier = Modifier.padding(top = Spacing.x100))
        }
    }
}
