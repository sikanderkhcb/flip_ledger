package com.circuitflip.flipledger.presentation.screens.addcost

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.domain.model.CostType
import com.circuitflip.flipledger.domain.model.PaidBy
import com.circuitflip.flipledger.presentation.components.ChipGroup
import com.circuitflip.flipledger.presentation.components.FieldLabel
import com.circuitflip.flipledger.presentation.components.FlipTextField
import com.circuitflip.flipledger.presentation.components.FlipTopBar
import com.circuitflip.flipledger.presentation.components.PrimaryButton
import com.circuitflip.flipledger.presentation.components.ScreenScaffold

/** 15 · Add Cost — cost type, amount, paid by, date, optional note. */
@Composable
fun AddCostScreen(vm: AddCostViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    LaunchedEffect(Unit) { vm.start() }
    val d by vm.draft.collectAsState()
    val saved by vm.saved.collectAsState()
    val submitting by vm.submitting.collectAsState()
    LaunchedEffect(saved) { if (saved) onSaved() }

    ScreenScaffold {
        FlipTopBar(title = "Add cost", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            FieldLabel("Cost type")
            ChipGroup(CostType.entries, d.type, { it.label }, vm::setType)
            Spacer(Modifier.height(20.dp))
            FlipTextField(d.amount, vm::setAmount, "Amount", placeholder = "0", keyboardType = KeyboardType.Decimal, currencyPrefix = true)
            Spacer(Modifier.height(16.dp))
            FieldLabel("Paid by")
            ChipGroup(PaidBy.entries, d.paidBy, { it.label }, vm::setPaidBy)
            Spacer(Modifier.height(20.dp))
            FlipTextField(d.date, vm::setDate, "Date", placeholder = "Jul 12, 2026")
            Spacer(Modifier.height(16.dp))
            FlipTextField(d.note, vm::setNote, "Note (optional)", placeholder = "Screen replacement")
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.padding(20.dp)) { PrimaryButton("Save Cost", vm::save, loading = submitting) }
    }
}
