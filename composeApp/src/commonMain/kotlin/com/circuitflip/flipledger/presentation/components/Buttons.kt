package com.circuitflip.flipledger.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.presentation.theme.FlipTheme
import com.circuitflip.flipledger.presentation.theme.Radius

/** Full-width filled primary CTA. Matches the design's 12px-radius solid button. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(Radius.button),
        colors = ButtonDefaults.buttonColors(
            containerColor = FlipTheme.colors.primary,
            contentColor = FlipTheme.colors.textInverse,
            disabledContainerColor = FlipTheme.colors.borderStrong,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(strokeWidth = 2.dp, color = FlipTheme.colors.textInverse, modifier = Modifier.size(20.dp))
        } else {
            Text(text, style = FlipTheme.typography.headingS)
        }
    }
}

/** Outlined / secondary action. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(Radius.button),
        border = BorderStroke(1.dp, FlipTheme.colors.borderDefault),
    ) {
        Text(text, style = FlipTheme.typography.headingS, color = FlipTheme.colors.textDefault)
    }
}

/** Low-emphasis text link (e.g. "Back", "Sign up"). */
@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text, style = FlipTheme.typography.bodyM, color = FlipTheme.colors.textWeaker)
    }
}
