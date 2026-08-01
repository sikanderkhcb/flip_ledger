import Foundation
import UserNotifications
import ComposeApp

/// iOS-side implementation of the shared Kotlin `Notifier` protocol, backed by
/// UNUserNotificationCenter. Posts *local* notifications (no APNs/push server needed).
/// Installed into `NotifierProvider` from `iOSApp.init()`, which also sets it as the
/// notification-center delegate so banners appear while the app is in the foreground.
class IosNotifier: NSObject, Notifier, UNUserNotificationCenterDelegate {

    func show(title: String, body: String) {
        let center = UNUserNotificationCenter.current()
        // Requesting when already determined just returns the prior decision — safe to call here.
        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
            guard granted else { return }

            let content = UNMutableNotificationContent()
            content.title = title
            content.body = body
            content.sound = .default

            // A tiny delay makes delivery reliable right after the sign-up call returns.
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
            let request = UNNotificationRequest(
                identifier: UUID().uuidString,
                content: content,
                trigger: trigger
            )
            center.add(request, withCompletionHandler: nil)
        }
    }

    /// Present the banner even when the app is foregrounded (it will be, right after sign-up).
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }
}
