import Foundation
import StoreKit
import UIKit
import ComposeApp

/// iOS-side implementation of the shared Kotlin `ReviewPrompter` protocol, backed by StoreKit's
/// `SKStoreReviewController`. It shows Apple's own in-place rating overlay; iOS rate-limits it
/// (a few times per year per user), so a no-show is expected and not an error.
/// Installed into `ReviewPrompterProvider` from `iOSApp.init()`.
class IosReviewPrompter: ReviewPrompter {
    func requestReview() {
        // StoreKit requires this on the main thread and needs the active window scene.
        DispatchQueue.main.async {
            guard let scene = UIApplication.shared.connectedScenes
                .first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene
            else { return }
            SKStoreReviewController.requestReview(in: scene)
        }
    }
}
