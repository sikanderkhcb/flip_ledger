package com.blackink.app.android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.blackink.app.core.notifications.Notifier

/**
 * Android-side [Notifier] backed by NotificationManagerCompat. Posts *local* notifications to the
 * system tray. The POST_NOTIFICATIONS runtime permission (Android 13+) is requested up front by
 * [MainActivity]; here we simply skip posting if it hasn't been granted (posting would silently
 * no-op anyway).
 */
class AndroidNotifier(private val context: Context) : Notifier {

    init {
        // A channel is required on Android 8+ before any notification can be posted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Account and activity updates" }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    override fun show(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(WELCOME_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "blackink_general"
        private const val WELCOME_NOTIFICATION_ID = 1001
    }
}
