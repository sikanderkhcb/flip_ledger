import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // MainViewControllerKt is generated from MainViewController.kt in composeApp/iosMain.
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var isShowingSplash = true

    var body: some View {
        ZStack {
            ComposeView()
                .ignoresSafeArea(.keyboard) // Compose handles the keyboard inset itself

            if isShowingSplash {
                StartupSplashView()
                    .transition(.opacity)
                    .zIndex(1)
                    .allowsHitTesting(false)
            }
        }
        .task {
            try? await Task.sleep(nanoseconds: 1_100_000_000)
            guard !Task.isCancelled else { return }

            if reduceMotion {
                isShowingSplash = false
            } else {
                withAnimation(.easeOut(duration: 0.18)) {
                    isShowingSplash = false
                }
            }
        }
    }
}

private struct StartupSplashView: View {
    var body: some View {
        ZStack {
            Color("LaunchBackground")
                .ignoresSafeArea()

            VStack(spacing: 0) {
                Image("LaunchLogoV2")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 104, height: 104)
                    .accessibilityHidden(true)

                Text("FlipLedger")
                    .font(.system(size: 36, weight: .semibold, design: .serif))
                    .foregroundStyle(Color(red: 40 / 255, green: 40 / 255, blue: 41 / 255))
                    .padding(.top, 24)

                Text("Inventory and profit, clearly tracked.")
                    .font(.system(size: 14))
                    .foregroundStyle(Color(red: 74 / 255, green: 74 / 255, blue: 75 / 255))
                    .multilineTextAlignment(.center)
                    .padding(.top, 8)
            }
            .padding(32)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("FlipLedger. Inventory and profit, clearly tracked.")
    }
}
