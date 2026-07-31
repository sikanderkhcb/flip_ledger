package com.blackink.app.presentation.screens.invoice

import com.blackink.app.domain.model.BusinessProfile
import com.blackink.app.domain.repository.ProfileRepository
import com.blackink.app.presentation.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Supplies the seller's business profile (name + owner) for the invoice's PAY TO section. */
class InvoiceViewModel(profileRepository: ProfileRepository) : BaseViewModel() {
    val profile: StateFlow<BusinessProfile> =
        profileRepository.observeProfile()
            .stateIn(scope, SharingStarted.Eagerly, BusinessProfile())
}
