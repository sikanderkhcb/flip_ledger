package com.blackink.app.presentation.screens.adddevice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.blackink.app.domain.model.AcquisitionSource
import com.blackink.app.domain.model.DeviceCategory
import com.blackink.app.domain.model.DeviceCondition
import com.blackink.app.domain.model.LockStatus
import com.blackink.app.domain.util.Money
import com.blackink.app.presentation.components.ChipGroup
import com.blackink.app.presentation.components.DateField
import com.blackink.app.presentation.components.FieldLabel
import com.blackink.app.presentation.components.FlipCard
import com.blackink.app.presentation.components.FlipTextField
import com.blackink.app.presentation.components.FlipTopBar
import com.blackink.app.presentation.components.IconBlob
import com.blackink.app.presentation.components.LinkButton
import com.blackink.app.presentation.components.PrimaryButton
import com.blackink.app.presentation.components.ScreenScaffold
import com.blackink.app.presentation.components.WizardHeader
import com.blackink.app.presentation.components.categoryIcon
import com.blackink.app.presentation.components.UiErrorEffect
import com.blackink.app.presentation.theme.FlipTheme

/** 09 · Step 1 — category + model name. */
@Composable
fun AddDevice1Screen(vm: AddDeviceViewModel, onContinue: () -> Unit, onBack: () -> Unit) {
    val d by vm.draft.collectAsState()
    val errors by vm.fieldErrors.collectAsState()
    WizardShell(1, "What are you adding?", null, onContinue = onContinue, onBack = onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DeviceCategory.entries.take(3).forEach { cat -> CategoryCard(cat, d.category == cat, { vm.setCategory(cat) }, Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DeviceCategory.entries.drop(3).forEach { cat -> CategoryCard(cat, d.category == cat, { vm.setCategory(cat) }, Modifier.weight(1f)) }
            Spacer(Modifier.weight(1f))
        }
        FieldError(errors["category"])
        Spacer(Modifier.height(20.dp))
        FlipTextField(
            d.model,
            vm::setModel,
            "Model name",
            placeholder = "e.g. iPhone 15 Pro 256GB",
            error = errors["model"],
        )
    }
}

/** 10 · Step 2 — purchase price, date, source. */
@Composable
fun AddDevice2Screen(vm: AddDeviceViewModel, onContinue: () -> Unit, onBack: () -> Unit) {
    val d by vm.draft.collectAsState()
    val errors by vm.fieldErrors.collectAsState()
    WizardShell(2, "What did it cost you?", "This becomes the starting point for true profit.", onContinue = onContinue, onBack = onBack) {
        FlipTextField(
            d.price,
            vm::setPrice,
            "Purchase price",
            placeholder = "0",
            keyboardType = KeyboardType.Decimal,
            currencyPrefix = true,
            error = errors["price"],
        )
        Spacer(Modifier.height(16.dp))
        DateField(
            d.date,
            vm::setDate,
            "Purchase date",
            error = errors["date"],
        )
        Spacer(Modifier.height(20.dp))
        FieldLabel("Source")
        ChipGroup(AcquisitionSource.entries, d.source, { it.label }, vm::setSource)
        FieldError(errors["source"])
    }
}

/** 11 · Step 3 — condition, identifier, storage, lock (all optional). */
@Composable
fun AddDevice3Screen(vm: AddDeviceViewModel, onContinue: () -> Unit, onBack: () -> Unit) {
    val d by vm.draft.collectAsState()
    val errors by vm.fieldErrors.collectAsState()
    WizardShell(3, "Tell us about the device", "Optional but helpful for records and disputes.", onContinue = onContinue, onBack = onBack) {
        FieldLabel("Condition")
        ChipGroup(DeviceCondition.entries, d.condition, { it.label }, vm::setCondition)
        Spacer(Modifier.height(20.dp))
        FlipTextField(
            d.identifierLast4,
            vm::setIdentifier,
            "Serial / IMEI",
            placeholder = "Last 4 digits are enough",
            keyboardType = KeyboardType.Number,
            error = errors["identifier"],
        )
        Spacer(Modifier.height(16.dp))
        FlipTextField(
            d.storage,
            vm::setStorage,
            "Storage",
            placeholder = "256GB",
            error = errors["storage"],
        )
        Spacer(Modifier.height(20.dp))
        FieldLabel("Lock status")
        ChipGroup(listOf(LockStatus.UNLOCKED, LockStatus.LOCKED), d.lock, { it.label }, vm::setLock)
    }
}

@Composable
private fun FieldError(message: String?) {
    message?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, style = FlipTheme.typography.caption, color = FlipTheme.colors.error)
    }
}

/** 12 · Step 4 — review all fields, then Add Device. */
@Composable
fun AddDevice4Screen(vm: AddDeviceViewModel, onEdit: () -> Unit, onBack: () -> Unit) {
    val d by vm.draft.collectAsState()
    val submitting by vm.submitting.collectAsState()
    val error by vm.error.collectAsState()
    val rows = listOf(
        "Category" to (d.category?.label ?: "—"),
        "Model" to d.model.ifBlank { "—" },
        "Purchase price" to (d.price.takeIf { it.isNotBlank() }?.let { Money.format(Money.parseToCents(it)) } ?: "—"),
        "Date" to d.date,
        "Condition" to (d.condition?.label ?: "—"),
        "Identifier" to (d.identifierLast4.takeIf { it.isNotBlank() }?.let { "●●●●$it" } ?: "Not provided"),
        "Source" to (d.source?.label ?: "—"),
    )
    WizardShell(4, "Review your device", null, continueLabel = "Add Device", loading = submitting, onContinue = vm::submit, onBack = onBack) {
        FlipCard {
            rows.forEachIndexed { i, (label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = FlipTheme.typography.bodyM, color = FlipTheme.colors.textWeaker)
                    Text(value, style = FlipTheme.typography.headingS, color = FlipTheme.colors.textDefault)
                }
                if (i < rows.lastIndex) androidx.compose.material3.HorizontalDivider(color = FlipTheme.colors.borderDefault)
            }
        }
        Spacer(Modifier.height(8.dp))
        LinkButton("Edit", onEdit, Modifier.fillMaxWidth())
        UiErrorEffect(error)
    }
}

@Composable
private fun CategoryCard(category: DeviceCategory, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val blob = when (category) {
        DeviceCategory.PHONE -> FlipTheme.colors.accentLilac
        DeviceCategory.LAPTOP -> FlipTheme.colors.accentLoyaltyBlue
        DeviceCategory.TABLET -> FlipTheme.colors.accentAmberLight
        DeviceCategory.GAMING -> FlipTheme.colors.accentPink
        DeviceCategory.ACCESSORY -> FlipTheme.colors.accentCream
    }
    FlipCard(modifier = modifier, padding = 12.dp, selected = selected, onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            IconBlob(color = blob, size = 40.dp) { Icon(categoryIcon(category), contentDescription = null, tint = FlipTheme.colors.textDefault) }
            Spacer(Modifier.height(8.dp))
            Text(
                category.label,
                style = FlipTheme.typography.caption,
                color = if (selected) FlipTheme.colors.primary else FlipTheme.colors.textWeaker,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/** Shared 4-step wizard shell. */
@Composable
private fun WizardShell(
    step: Int,
    title: String,
    subtitle: String?,
    continueLabel: String = "Continue",
    continueEnabled: Boolean = true,
    loading: Boolean = false,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    body: @Composable () -> Unit,
) {
    ScreenScaffold {
        FlipTopBar(title = "Add device", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
            WizardHeader(step, 4, title, subtitle)
            Spacer(Modifier.height(24.dp))
            body()
        }
        Column(Modifier.padding(24.dp)) {
            PrimaryButton(continueLabel, onContinue, enabled = continueEnabled, loading = loading)
        }
    }
}
