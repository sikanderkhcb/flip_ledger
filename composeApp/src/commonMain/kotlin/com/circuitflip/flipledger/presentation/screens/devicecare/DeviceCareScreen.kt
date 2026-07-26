package com.circuitflip.flipledger.presentation.screens.devicecare

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.domain.model.DeviceStatus
import com.circuitflip.flipledger.presentation.components.FlipTextField
import com.circuitflip.flipledger.presentation.components.FlipTopBar
import com.circuitflip.flipledger.presentation.components.PrimaryButton
import com.circuitflip.flipledger.presentation.components.ScreenScaffold
import com.circuitflip.flipledger.presentation.components.SecondaryButton
import com.circuitflip.flipledger.presentation.components.StatusPill
import com.circuitflip.flipledger.presentation.rememberViewModel
import com.circuitflip.flipledger.presentation.theme.FlipTheme

@Composable
fun DeviceCareScreen(deviceId: String, onBack: () -> Unit) {
    val vm = rememberViewModel<DeviceCareViewModel>(key = deviceId)
    LaunchedEffect(deviceId) { vm.load(deviceId) }
    val state by vm.state.collectAsState()
    val device = state.device ?: return
    val d = state.draft
    val colors = FlipTheme.colors

    ScreenScaffold {
        FlipTopBar(title = "Repair & warranty", onBack = onBack, trailing = { StatusPill(device.status) })
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Text("Repair details", style = FlipTheme.typography.headingM, color = colors.textDefault)
            Spacer(Modifier.padding(4.dp))
            FlipTextField(d.repairIssue, { value -> vm.update { draft -> draft.copy(repairIssue = value) } }, "Problem", placeholder = "Cracked screen, battery issue…")
            Spacer(Modifier.padding(6.dp))
            FlipTextField(d.repairProvider, { value -> vm.update { draft -> draft.copy(repairProvider = value) } }, "Repair provider", placeholder = "Technician or shop")
            Spacer(Modifier.padding(6.dp))
            FlipTextField(d.repairStartedOn, { value -> vm.update { draft -> draft.copy(repairStartedOn = value) } }, "Repair started", placeholder = "YYYY-MM-DD")
            Spacer(Modifier.padding(6.dp))
            FlipTextField(d.repairCompletedOn, { value -> vm.update { draft -> draft.copy(repairCompletedOn = value) } }, "Repair completed", placeholder = "YYYY-MM-DD")

            Spacer(Modifier.padding(8.dp))
            Text("Warranty", style = FlipTheme.typography.headingM, color = colors.textDefault)
            Spacer(Modifier.padding(4.dp))
            FlipTextField(d.warrantyProvider, { value -> vm.update { draft -> draft.copy(warrantyProvider = value) } }, "Warranty provider", placeholder = "Apple, Samsung, store warranty…")
            Spacer(Modifier.padding(6.dp))
            FlipTextField(d.warrantyExpiresOn, { value -> vm.update { draft -> draft.copy(warrantyExpiresOn = value) } }, "Warranty expires", placeholder = "YYYY-MM-DD")
            if (device.warrantyDaysRemaining != null) {
                val days = device.warrantyDaysRemaining ?: 0
                Text(
                    if (days == 0) "Warranty expires today" else if (days > 0) "Warranty active for $days more days" else "Warranty expired",
                    style = FlipTheme.typography.caption,
                    color = if (days >= 0) colors.success else colors.textWeaker,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            state.error?.let { Text(it, style = FlipTheme.typography.bodyS, color = colors.error, modifier = Modifier.padding(top = 12.dp)) }
            if (state.saved) Text("Saved", style = FlipTheme.typography.bodyS, color = colors.success, modifier = Modifier.padding(top = 12.dp))
            Spacer(Modifier.padding(12.dp))
        }
        Column(Modifier.padding(20.dp)) {
            PrimaryButton("Save repair & warranty", { vm.save(deviceId) }, loading = state.saving)
            Spacer(Modifier.padding(4.dp))
            SecondaryButton(
                if (device.status == DeviceStatus.REPAIR) "Mark repair complete" else "Mark as in repair",
                onClick = { vm.toggleRepairStatus(deviceId) },
                enabled = !state.saving,
            )
        }
    }
}
