package com.circuitflip.flipledger.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import org.koin.compose.getKoin

/**
 * Obtains a [BaseViewModel] from Koin, remembers it for the composition, and cancels its
 * scope when it leaves. This replaces AndroidX's viewModel() without pulling in the
 * lifecycle-viewmodel multiplatform artifact.
 */
@Composable
inline fun <reified T : BaseViewModel> rememberViewModel(key: Any? = null): T {
    val koin = getKoin()
    val vm = remember(key) { koin.get<T>() }
    DisposableEffect(vm) { onDispose { vm.clear() } }
    return vm
}
