# FlipLedger

FlipLedger is a Kotlin Multiplatform resale ledger for Android and iOS. It tracks devices,
additional costs, completed sales, profit, inventory aging, reports, and partner splits.

## Current architecture

- Shared Kotlin and Compose Multiplatform UI in `composeApp`
- Android host in `androidApp`
- SwiftUI iOS host in `iosApp`
- Supabase Auth, PostgREST, and Edge Functions for remote persistence and Stripe billing
- Koin for dependency injection
- `multiplatform-settings` for the theme preference
- `kotlin.test` and `kotlinx-coroutines-test` for shared tests

The app is online-backed. It keeps an in-memory cache for responsive navigation, but it does
not currently provide durable offline storage or background synchronization.

## Prerequisites

- JDK 17 or newer (a JRE alone is not enough because Gradle needs `javac`)
- Android Studio with Android SDK 34 for Android
- Xcode 15 or newer for iOS
- A Supabase project for authenticated data storage and Edge Functions
- A Stripe account with a recurring USD $10/month Price

## Backend setup

The versioned database definitions are in:

```text
supabase/migrations/
```

They create the application tables, ownership indexes, Row Level Security policies,
subscription state, the lifetime 10-device free allowance, and transactional database
functions. The server blocks device 11 unless Stripe reports an active subscription. Sales
for existing devices are never blocked.

Review the migration against the target project before applying it:

```bash
supabase db push
```

## Stripe subscription setup

The mobile app uses Stripe Checkout for a recurring $10/month unlimited-device subscription.
Stripe API calls run only in Supabase Edge Functions; no Stripe secret is embedded in the app.

1. In Stripe test mode, create a Product named `FlipLedger Unlimited` and a recurring Price:
   USD `$10.00`, billed monthly.
2. Create a restricted API key (`rk_...`) rather than a broad secret key. Grant only the
   Customer, Checkout Session, Billing Portal Session, Subscription, Price, and Product
   permissions required by the functions.
3. Copy `supabase/functions/.env.example` to an ignored local environment file and supply:
   `STRIPE_API_KEY`, `STRIPE_PRICE_ID`, and `STRIPE_WEBHOOK_SECRET`.
4. Store production values in Supabase project secrets:

```bash
supabase secrets set --env-file supabase/functions/.env.production
```

5. Apply the database migration and deploy the functions:

```bash
supabase db push
supabase functions deploy create-checkout-session
supabase functions deploy create-portal-session
supabase functions deploy stripe-webhook
supabase functions deploy stripe-return
```

6. In Stripe Workbench, create a webhook endpoint pointing to:

```text
https://YOUR_PROJECT_REF.supabase.co/functions/v1/stripe-webhook
```

Subscribe it to:

```text
checkout.session.completed
customer.subscription.created
customer.subscription.updated
customer.subscription.deleted
```

7. Enable and configure Stripe Customer Portal so users can update payment details or cancel.

The functions use Stripe API version `2026-06-24.dahlia`. The webhook verifies Stripe's
signature before changing subscription access.

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
- Free accounts can create 10 device records over their lifetime. Selling or deleting a
  device does not restore a free slot.
- Active Stripe subscribers can create unlimited devices.
- A cancelled or expired subscriber may view and sell every existing device but cannot add
  another device after the paid period ends.
- Sale completion is atomic (all-or-nothing) through the `complete_sale` database function.
- In-memory user data is cleared whenever the session becomes unauthenticated.

## Intentionally unavailable features

- Apple sign-in is hidden on iOS until the native AuthenticationServices flow and Apple
  entitlement are configured.
- Browser-based Stripe Checkout is implemented. StoreKit and Google Play Billing still need
  to replace or complement it before distributing digital subscriptions in storefronts or
  regions that require the platform billing system.
- Settlement payment history is not yet persisted, so the app does not invent previous
  payments or subtract placeholder amounts.
