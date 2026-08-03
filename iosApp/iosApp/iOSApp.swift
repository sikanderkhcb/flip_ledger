import SwiftUI
import ComposeApp
import FirebaseCore
import UserNotifications

@main
struct iOSApp: App {
    // Retained for the app's lifetime: it's the notification-center delegate (held only weakly
    // there) and the shared Notifier (held strongly by Kotlin), so this keeps both alive.
    private let notifier = IosNotifier()

    init() {
        // Start Firebase, then install the Firebase-backed telemetry into the shared holder
        // before any Compose/Kotlin code (which logs via `Track`) runs.
        FirebaseApp.configure()
        TelemetryProvider.shared.instance = IosTelemetry()

        // Local device notifications (e.g. the sign-up welcome) are posted through this facade.
        UNUserNotificationCenter.current().delegate = notifier
        NotifierProvider.shared.instance = notifier

        // Native App Store rating prompt, driven by ReviewGate from shared code.
        ReviewPrompterProvider.shared.instance = IosReviewPrompter()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
                .onOpenURL { url in
                    // Stripe checkout return — dismiss the in-app browser and refresh.
                    guard url.scheme == "blackink" else { return }
                    if url.host == "subscription" {
                        CheckoutBrowser_iosKt.dismissCheckoutBrowser()
                    } else if url.host == "password-reset" {
                        AuthRecoveryKt.markPasswordResetRequested(url: url.absoluteString)
                    }
                }
        }
    }
}
