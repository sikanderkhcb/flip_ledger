import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
                .onOpenURL { url in
                    guard url.scheme == "flipledger", url.host == "subscription" else { return }
                    CheckoutBrowser_iosKt.dismissCheckoutBrowser()
                }
        }
    }
}
