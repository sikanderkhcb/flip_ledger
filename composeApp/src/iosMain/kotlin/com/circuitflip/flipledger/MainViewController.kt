package com.circuitflip.flipledger

import androidx.compose.ui.window.ComposeUIViewController
import com.circuitflip.flipledger.di.initKoin

private val koin by lazy { initKoin() }

/**
 * Bridge called from Swift (see iosApp/ContentView.swift). Ensures Koin is started exactly
 * once, then returns a UIViewController rendering the shared [App].
 */
fun MainViewController() = ComposeUIViewController {
    koin // touch to trigger one-time init
    App()
}
