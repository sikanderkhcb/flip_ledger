package com.circuitflip.flipledger.presentation

import com.circuitflip.flipledger.domain.model.CostDraft
import com.circuitflip.flipledger.domain.model.DeviceDraft
import com.circuitflip.flipledger.domain.model.Sale
import com.circuitflip.flipledger.domain.model.SaleDraft
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Cross-screen state that spans multi-step flows (which device is selected, the in-progress
 * device/cost/sale drafts, the last-added device id, and the completed-sale snapshot).
 *
 * In the reference design all of this lived in one component's state; here it's a single
 * injected holder so wizard screens stay decoupled while sharing progress. Registered as a
 * Koin singleton.
 */
class WizardStore {
    val deviceDraft = MutableStateFlow(DeviceDraft())
    val costDraft = MutableStateFlow(CostDraft())
    val saleDraft = MutableStateFlow(SaleDraft())

    var selectedDeviceId: String? = null
    var lastAddedDeviceId: String? = null
    var lastSale: Sale? = null

    fun resetDeviceDraft() { deviceDraft.value = DeviceDraft() }
    fun resetCostDraft() { costDraft.value = CostDraft() }
    fun resetSaleDraft() { saleDraft.value = SaleDraft() }

    fun clearSession() {
        resetDeviceDraft()
        resetCostDraft()
        resetSaleDraft()
        selectedDeviceId = null
        lastAddedDeviceId = null
        lastSale = null
    }

    inline fun updateDevice(block: (DeviceDraft) -> DeviceDraft) { deviceDraft.value = block(deviceDraft.value) }
    inline fun updateCost(block: (CostDraft) -> CostDraft) { costDraft.value = block(costDraft.value) }
    inline fun updateSale(block: (SaleDraft) -> SaleDraft) { saleDraft.value = block(saleDraft.value) }
}
