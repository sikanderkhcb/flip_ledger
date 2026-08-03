package com.blackink.app.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.blackink.app.App
import com.blackink.app.presentation.markPasswordResetRequested

/** Single-activity host. All UI lives in the shared [App] composable. */
class MainActivity : ComponentActivity() {

    // Registered at construction (before STARTED) as required by the Activity Result API. We don't
    // act on the result: if the user declines, notifications simply stay silent.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Theme the Android 12+ system splash (dark ink + white diamond) via Theme.BlackInk.Starting
        // and let it hand off on the first frame to the branded in-app splash. Same brand mark on
        // both, so the two read as one continuous splash.
        installSplashScreen()

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleAuthIntent(intent)
        requestNotificationPermissionIfNeeded()
        setContent { App() }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: android.content.Intent?) {
        if (intent?.data?.scheme == "blackink" && intent.data?.host == "password-reset") {
            markPasswordResetRequested(intent.data.toString())
        }
    }

    /** Android 13+ requires a runtime grant to post notifications; ask once on launch. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
