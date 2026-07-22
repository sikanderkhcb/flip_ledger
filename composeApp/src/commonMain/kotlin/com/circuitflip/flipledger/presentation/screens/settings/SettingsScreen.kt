package com.circuitflip.flipledger.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.domain.model.WorkspaceType
import com.circuitflip.flipledger.presentation.components.FlipCard
import com.circuitflip.flipledger.presentation.components.IconBlob
import com.circuitflip.flipledger.presentation.components.LinkButton
import com.circuitflip.flipledger.presentation.components.SectionLabel
import com.circuitflip.flipledger.presentation.rememberViewModel
import com.circuitflip.flipledger.presentation.theme.FlipTheme

/** 24 · Settings — profile header, workspace/partner/export rows, theme toggle, sign out. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenSettlement: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSubscription: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val vm = rememberViewModel<SettingsViewModel>()
    val state by vm.state.collectAsState()
    val colors = FlipTheme.colors
    var showSignOutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }

    Box(Modifier.fillMaxSize().background(colors.backgroundSubtle)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("More", style = FlipTheme.typography.headingXl, color = colors.textDefault)
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.padding(20.dp, 0.dp, 20.dp, 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Profile header
                FlipCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBlob(color = colors.accentLilac, size = 52.dp) {
                            Text(state.profile.ownerName.ifBlank { state.profile.businessName }.take(1), style = FlipTheme.typography.headingM, color = colors.primary)
                        }
                        Spacer(Modifier.size(14.dp))
                        Column {
                            Text(state.profile.ownerName.ifBlank { state.profile.businessName }, style = FlipTheme.typography.headingM, color = colors.textDefault)
                            Text(state.profile.businessName, style = FlipTheme.typography.bodyS, color = colors.textWeaker)
                        }
                    }
                }

                SectionLabel("WORKSPACE")
                FlipCard(padding = 0.dp) {
                    SettingsRow("Workspace", colors.accentLoyaltyBlue, onClick = {})
                    Divider()
                    if (state.profile.workspaceType == WorkspaceType.PARTNER) {
                        SettingsRow("Partner settlement", colors.accentLilac, onClick = onOpenSettlement)
                        Divider()
                    }
                    SettingsRow("Export & reports", colors.accentAmberLight, onClick = onOpenReports)
                    Divider()
                    SettingsRow("Subscription", colors.accentCream, onClick = onOpenSubscription)
                }

                SectionLabel("PREFERENCES")
                FlipCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Dark theme", style = FlipTheme.typography.headingS, color = colors.textDefault)
                            Text(if (state.isDark) "rp-new-dark" else "loft (light)", style = FlipTheme.typography.bodyS, color = colors.textWeaker)
                        }
                        Switch(
                            checked = state.isDark,
                            onCheckedChange = { vm.toggleTheme() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.backgroundDefault,
                                checkedTrackColor = colors.primary,
                            ),
                        )
                    }
                }

                SectionLabel("ACCOUNT")
                FlipCard(padding = 0.dp) {
                    SettingsRow("Help", colors.backgroundMuted, onClick = {})
                    Divider()
                    SettingsRow("Privacy", colors.backgroundMuted, onClick = {})
                }

                Spacer(Modifier.height(4.dp))
                LinkButton(text = "Sign out", onClick = { showSignOutConfirm = true }, modifier = Modifier.fillMaxWidth())
            }
        }

        if (showSignOutConfirm) {
            AlertDialog(
                onDismissRequest = { showSignOutConfirm = false },
                title = { Text("Sign out?", style = FlipTheme.typography.headingM, color = colors.textDefault) },
                text = {
                    Text(
                        "You'll need to sign in again to access your inventory and profit.",
                        style = FlipTheme.typography.bodyM,
                        color = colors.textWeaker,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showSignOutConfirm = false
                        vm.signOut()
                    }) { Text("Sign out", color = colors.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutConfirm = false }) {
                        Text("Cancel", color = colors.textDefault)
                    }
                },
                containerColor = colors.backgroundDefault,
            )
        }
    }
}

@Composable
private fun SettingsRow(label: String, iconColor: Color, onClick: () -> Unit) {
    val colors = FlipTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBlob(color = iconColor, size = 32.dp)
        Spacer(Modifier.size(12.dp))
        Text(label, style = FlipTheme.typography.bodyL, color = colors.textDefault, modifier = Modifier.weight(1f))
        Text("›", style = FlipTheme.typography.headingS, color = colors.textWeakest)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(start = 60.dp).height(1.dp).background(FlipTheme.colors.borderDefault))
}
