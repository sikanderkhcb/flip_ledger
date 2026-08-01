import Foundation
import ComposeApp
import FirebaseAnalytics
import FirebaseCrashlytics

/// iOS-side implementation of the shared Kotlin `Telemetry` interface, backed by Firebase
/// Analytics + Crashlytics. Installed into `TelemetryProvider` from `iOSApp.init()`.
class IosTelemetry: Telemetry {
    func logEvent(name: String, params: [String: String]) {
        Analytics.logEvent(name, parameters: params as [String: Any])
    }

    func recordError(throwable: KotlinThrowable, fatal: Bool) {
        let error = NSError(
            domain: "KotlinError",
            code: 0,
            userInfo: [
                NSLocalizedDescriptionKey: throwable.message ?? "Kotlin error",
                "kotlin_type": String(describing: type(of: throwable)),
                "fatal": fatal,
            ]
        )
        Crashlytics.crashlytics().record(error: error)
    }

    func log(message: String) {
        Crashlytics.crashlytics().log(message)
    }

    func setUser(id: String?) {
        Analytics.setUserID(id)
        Crashlytics.crashlytics().setUserID(id ?? "")
    }
}
