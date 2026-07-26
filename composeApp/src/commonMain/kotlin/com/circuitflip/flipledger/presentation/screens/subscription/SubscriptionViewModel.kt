package com.circuitflip.flipledger.presentation.screens.subscription

import com.circuitflip.flipledger.core.DataResult
import com.circuitflip.flipledger.core.onFailure
import com.circuitflip.flipledger.core.onSuccess
import com.circuitflip.flipledger.domain.model.SubscriptionAccess
import com.circuitflip.flipledger.domain.model.SubscriptionTier
import com.circuitflip.flipledger.domain.repository.SubscriptionRepository
import com.circuitflip.flipledger.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SubscriptionUiState(
    val access: SubscriptionAccess = SubscriptionAccess(),
    val loading: Boolean = true,
    val actionLoading: Boolean = false,
    val checkingDeviceLimit: Boolean = false,
    val checkoutTier: SubscriptionTier? = null,
    val externalUrl: String? = null,
    val error: String? = null,
)

class SubscriptionViewModel(
    private val repository: SubscriptionRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(SubscriptionUiState())
    val state = _state.asStateFlow()

    init {
        repository.observeAccess()
            .onEach { access ->
                _state.value = _state.value.copy(access = access, loading = false)
            }
            .launchIn(scope)
    }

    fun refresh() {
        if (_state.value.actionLoading) return
        scope.launch {
            _state.value = _state.value.copy(actionLoading = true, error = null)
            repository.refresh()
                .onFailure { error ->
                    _state.value = _state.value.copy(error = error.userMessage())
                }
            _state.value = _state.value.copy(actionLoading = false, loading = false)
        }
    }

    fun startCheckout(tier: SubscriptionTier) = openExternal(
        action = { repository.createCheckoutSession(tier) },
        checkoutTier = tier,
    )

    fun manageSubscription() = openExternal(repository::createPortalSession)

    fun consumeExternalUrl() {
        _state.value = _state.value.copy(externalUrl = null)
    }

    fun requestAddDevice(
        onAllowed: () -> Unit,
        onLimitReached: () -> Unit,
    ) {
        if (_state.value.checkingDeviceLimit) return
        scope.launch {
            _state.value = _state.value.copy(checkingDeviceLimit = true)
            val latest = when (val result = repository.refresh()) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> _state.value.access
            }
            _state.value = _state.value.copy(checkingDeviceLimit = false)
            if (latest.canAddDevice) onAllowed() else onLimitReached()
        }
    }

    private fun openExternal(
        action: suspend () -> DataResult<String>,
        checkoutTier: SubscriptionTier? = null,
    ) {
        if (_state.value.actionLoading) return
        scope.launch {
            _state.value = _state.value.copy(
                actionLoading = true,
                checkoutTier = checkoutTier,
                error = null,
            )
            action()
                .onSuccess { url ->
                    _state.value = _state.value.copy(externalUrl = url)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(error = error.userMessage())
                }
            _state.value = _state.value.copy(
                actionLoading = false,
                checkoutTier = null,
            )
        }
    }
}
