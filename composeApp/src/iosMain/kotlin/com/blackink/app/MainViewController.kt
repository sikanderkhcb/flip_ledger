package com.blackink.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.window.ComposeUIViewController
import com.blackink.app.di.initKoin
import com.blackink.app.platform.createIosUriHandler

private val koin by lazy { initKoin() }

/**
 * Bridge called from Swift (see iosApp/ContentView.swift). Ensures Koin is started exactly
 * once, then returns a UIViewController rendering the shared [App].
 */
fun MainViewController() = ComposeUIViewController {
    CompositionLocalProvider(
        LocalUriHandler provides remember { createIosUriHandler() },
    ) {
        koin // touch to trigger one-time init
        App()
    }
}
