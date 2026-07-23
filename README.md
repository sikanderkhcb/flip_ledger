# FlipLedger

FlipLedger is a Kotlin Multiplatform resale ledger for Android and iOS. It tracks devices,
additional costs, completed sales, profit, inventory aging, reports, and partner splits.

## Current architecture

- Shared Kotlin and Compose Multiplatform UI in `composeApp`
- Android host in `androidApp`
- SwiftUI iOS host in `iosApp`
- Supabase Auth and PostgREST for remote persistence
- Koin for dependency injection
- `multiplatform-settings` for the theme preference
- `kotlin.test` and `kotlinx-coroutines-test` for shared tests

The app is online-backed. It keeps an in-memory cache for responsive navigation, but it does
not currently provide durable offline storage or background synchronization.

## Prerequisites

- JDK 17 or newer (a JRE alone is not enough because Gradle needs `javac`)
- Android Studio with Android SDK 34 for Android
- Xcode 15 or newer for iOS
- A Supabase project for authenticated data storage

## Backend setup

The versioned database definition is in:

```text
supabase/migrations/20260723000000_initial_schema.sql
```

It creates the tables, ownership indexes, Row Level Security policies, new-user profile
trigger, and the `complete_sale` database function. `complete_sale` records the sale and
removes the device in one PostgreSQL transaction.

Review the migration against the target project before applying it:

```bash
supabase db push
```

The client currently reads its project URL and publishable/anon key from
`SupabaseConfig.kt`. Before distributing the app, move environment-specific values into
per-build configuration so development and production cannot be mixed accidentally.

## Build and test

On Apple Silicon with the Homebrew JDK used for this repository:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
```

```bash
./gradlew :composeApp:allTests
./gradlew :androidApp:assembleDebug
```

For iOS, open `iosApp/iosApp.xcodeproj`. Its build phase calls:

```bash
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

## Data rules

- Monetary values are stored as integer cents.
- User-entered business dates are stored as ISO-8601 dates (`YYYY-MM-DD`).
- Monthly dashboard, sales, settlement, and report totals use the recorded sale date.
- Supabase RLS (Row Level Security) limits every row to its authenticated owner.
- Sale completion is atomic (all-or-nothing) through the `complete_sale` database function.
- In-memory user data is cleared whenever the session becomes unauthenticated.

## Intentionally unavailable features

- Apple sign-in is hidden on iOS until the native AuthenticationServices flow and Apple
  entitlement are configured.
- Subscription plans are a preview; the purchase button is disabled until StoreKit and
  Google Play Billing are implemented.
- Settlement payment history is not yet persisted, so the app does not invent previous
  payments or subtract placeholder amounts.
