# FlipLedger — Kotlin Multiplatform (Android + iOS)

**Know what you own. Know what you earned.**

FlipLedger is a resale-business tracker for people who buy used devices, invest in repairs,
and resell them. It records each device's true cost basis (purchase + every logged cost),
computes real net profit and margin on sale, ages inventory, and — in partner mode — settles
profit splits. This repository is a production-shaped **Compose Multiplatform** app that shares
one codebase across Android and iOS, reverse-engineered from the FlipLedger design prototype
and built on Clean Architecture + MVVM.

---

## 1. Tech stack

| Concern | Choice |
|---|---|
| Language / build | Kotlin 2.0.21, Gradle 8.9, AGP 8.5.2 |
| Shared UI | Compose Multiplatform 1.7 (Android + iOS) |
| Architecture | Clean Architecture (domain / data / presentation) + MVVM |
| DI | Koin 4 |
| Networking | Ktor 3 (OkHttp on Android, Darwin on iOS) |
| Local storage | SQLDelight 2 (Android driver + native driver) |
| Preferences | multiplatform-settings |
| Async | kotlinx.coroutines + Flow |
| Serialization | kotlinx.serialization |
| Logging | Napier |
| Testing | kotlin.test, kotlinx-coroutines-test, Turbine |

---

## 2. Project layout

```
FlipLedger/
├─ composeApp/                     ← shared KMP module (all business logic + UI)
│  └─ src/
│     ├─ commonMain/kotlin/com/circuitflip/flipledger/
│     │  ├─ core/                  Result type, AppError, Logger
│     │  ├─ domain/                model · util · repository (interfaces) · usecase
│     │  ├─ data/                  local (SQLDelight) · remote (Ktor) · repository impls · seed
│     │  ├─ di/                    Koin modules (core, domain, presentation, platform expect)
│     │  ├─ presentation/
│     │  │  ├─ theme/              design tokens (colors, type, dimens) + FlipTheme
│     │  │  ├─ components/         reusable UI (buttons, chips, cards, pills, top bar…)
│     │  │  ├─ navigation/         Route (sealed) + Navigator (stack)
│     │  │  └─ screens/            one package per screen, each with a ViewModel + composable
│     │  └─ App.kt                 root composable + AppNavHost (wires all 24 screens)
│     ├─ commonMain/sqldelight/    Device.sq · Cost.sq · Sale.sq
│     ├─ androidMain/              actual drivers (SQLDelight/Ktor) + androidContext module
│     ├─ iosMain/                  actual drivers + MainViewController() bridge
│     └─ commonTest/               Money, ProfitCalculator, AppViewModel (Turbine) tests
├─ androidApp/                     ← thin Android host (Application + MainActivity)
├─ iosApp/                         ← thin iOS host (SwiftUI wrapper around Compose)
├─ gradle/ + gradlew + settings.gradle.kts + build.gradle.kts
```

The dependency rule points inward: `presentation → domain ← data`. The domain layer knows
nothing about Compose, Ktor, SQLDelight, or platforms; it exposes repository *interfaces* and
use cases, and the data layer implements them.

---

## 3. How to build & run

### Prerequisites
- Android Studio (Ladybug or newer) with the Kotlin Multiplatform plugin
- JDK 17
- For iOS: a Mac with Xcode 15+

### Android
1. Open the project root in Android Studio and let it sync.
2. Select the `androidApp` run configuration and run on an emulator or device (minSdk 24).

Or from the command line:
```bash
./gradlew :androidApp:installDebug
```

### iOS
The `iosApp/` folder contains the SwiftUI host. The shared module builds into a framework
named `ComposeApp`.
1. Open `iosApp/iosApp.xcodeproj` in Xcode *(create it by pointing Xcode at the `iosApp`
   sources if not present — see note below)*.
2. Add a "Run Script" build phase that runs `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`,
   and link the `ComposeApp` framework.
3. Build & run on a simulator.

> **iOS project note.** To keep the deliverable text-only, `iosApp/` ships the Swift sources
> (`iOSApp.swift`, `ContentView.swift`, `Info.plist`) but not a binary `.xcodeproj`. Generate
> the Xcode project once (New → App, then add these files and the Gradle framework phase), or
> use the KMP wizard's standard `iosApp` template and drop these files in. `ContentView.swift`
> already calls `MainViewControllerKt.MainViewController()`.

### Tests
```bash
./gradlew :composeApp:allTests        # or :composeApp:testDebugUnitTest for JVM/Android
```

---

## 4. Fonts

The design uses **P22 Mackinac Pro** (display serif) and **Inter** (sans). Font binaries are
not redistributed here, so `flipTypography()` falls back to the platform serif/sans. To match
the design pixel-for-pixel, drop the font files into
`composeApp/src/commonMain/composeResources/font/` and reference them from `FlipType.kt` via
`FontFamily(Font(Res.font.mackinac_pro))`. Everything else (sizes, weights, line heights, letter
spacing) is already encoded to spec.

---

## 5. Design system

Two themes are captured exactly from the prototype: **loft** (light, default) and
**rp-new-dark** (dark), toggled at runtime from Settings via `ThemeRepository`. All tokens live
in `presentation/theme/`:
- `FlipColors.kt` — every semantic color (primary, backgrounds, text tiers, borders, success /
  warning / error / info, the three status-pill palettes, and the six accent hues).
- `FlipType.kt` — the display/heading/body/caption scale.
- `Dimens.kt` — the 4px spacing scale and radii (input 8, button 12, card 16, pill 999).

`FlipTheme.colors` / `FlipTheme.typography` expose them through CompositionLocals, and
`FlipLedgerTheme` maps them onto a Material 3 scheme so Material components inherit the palette.

---

## 6. The 24 screens (all implemented)

Splash · Welcome · Sign In / Sign Up · Setup 1–3 · Dashboard · Inventory · Add Device 1–4 ·
Device Added · Device Detail · Add Cost · Sale 1–3 · Sale Complete · Sales History ·
Partner Settlement · Reports · Subscription · Settings.

Navigation is a lightweight stack `Navigator` (push / replace / back / popTo / resetTo),
modeled on the prototype's own `goTo`/`back`, and dispatched by a single exhaustive `when`
over the sealed `Route` in `AppNavHost` (in `App.kt`). Multi-step flows (Setup, Add Device,
Sale) share one ViewModel instance and a single injected `WizardStore` so in-progress drafts
survive step-to-step navigation.

---

## 7. Business logic (single source of truth)

All monetary values are stored as **whole cents (`Long`)** to avoid floating-point drift, and
formatted to whole dollars for display (matching the mockup) via `Money`. `ProfitCalculator`
centralizes the rules:
- `invested = purchase + Σ costs`
- `net profit = revenue − invested − fees`, `margin = net / revenue` (guards ÷0)
- month net profit, average margin, inventory value, aging count (>30 days)
- expected-profit estimate = 32% of invested (as in the reference)
- partner settlement: your share = total × split%, owed = partner's accrued share

These are covered by unit tests in `commonTest`.

---

## 8. Data & offline

SQLDelight is the offline-first source of truth; repositories expose reactive `Flow`s so the
UI updates automatically. On first launch a `DatabaseSeeder` loads the prototype's sample
inventory (7 devices) and sales (5) so the app is explorable immediately. Networking is wired
through Ktor with a `FakeAuthApi` bound in DI, so the onboarding/auth flow works end-to-end
without a backend; swap in the real `AuthApi` implementation and a base URL to go live.

---

## 9. Assumptions made

- **Auth is stubbed.** The design shows sign-in/sign-up/"Continue with Apple" but no backend.
  A `FakeAuthApi` returns success and flips the session flag, so flows are demonstrable. Real
  endpoints plug into `AuthApi`/`AuthRepositoryImpl`.
- **Dates are display strings** (e.g. "Jul 12, 2026"), exactly as the prototype modeled them,
  rather than real `LocalDate`s — kept to preserve the design's copy and avoid inventing a
  date-picker spec. `kotlinx-datetime` is on the classpath if you want to formalize this.
- **Whole-dollar display, cent-precision storage** — the mockup rounds to dollars; storage
  keeps cents for correctness.
- **CSV export / "Continue with Apple" / plan purchase** are represented in the UI (as the
  design does) but are non-functional stubs — no export pipeline or StoreKit/Play Billing is
  specified by the source.
- **Custom stack navigator** was chosen over a library to mirror the reference's exact
  push/back model and avoid coupling to a specific nav library's lifecycle.

## 10. Push notifications

The source design contains **no** push-notification surface, so none is implemented — adding
an unrequested background service would be scope creep. If required later, the integration
points are: FCM on Android (a `FirebaseMessagingService` in `androidApp` + token registration
through a new repository) and APNs on iOS (register in `iOSApp.swift`, bridge the token into
the shared layer). The Settings screen already has a "Notifications" row to host preferences.

---

## 11. What is fully implemented vs. simplified

**Fully implemented:** the complete architecture (domain/data/presentation), the two-theme
design system to spec, all domain models + use cases, the SQLDelight schema with mappers and
seeding, Koin DI with platform `expect/actual` drivers, the reactive repository layer, all 24
screen composables and their ViewModels, the stack navigator wiring the whole flow, error
handling via a `DataResult` type + `AppError`, offline-first caching, and unit tests for the
money/profit logic and a ViewModel.

**Simplified / stubbed (documented above):** real authentication backend, CSV export, in-app
purchases, and push notifications — each because the source design specifies the surface but
not a backend contract. These are isolated behind interfaces so they can be implemented without
touching the UI or domain layers.

> This project could not be compiled in the authoring environment (no Kotlin/Android/Xcode
> toolchain was available), so treat a first Android Studio sync as the compile step. The code
> is written to the versions pinned in `gradle/libs.versions.toml`.
