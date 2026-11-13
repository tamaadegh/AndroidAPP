# Tamaade - iOS (branch `appleversion1`)

This branch holds **only the native iOS app** (SwiftUI). The Android app lives on
`leye-mobile` and is the reference implementation: when porting or fixing behaviour,
read the Kotlin there (`git show leye-mobile:app/src/main/java/com/tamaade/ecommerce/...`).
Same backend (TamaadeAPI branch `leye`), same endpoints - the iOS app never needs a
backend change. The only difference on the wire: Hubtel checkout sends `"platform": "ios"`.

Kotlin -> Swift mapping and decisions: `docs/IOS_PORT_PLAN.md`.
Open `Tamaade.xcodeproj`; see `README.md` for running and tests.

## The app talks to production

Debug and Release both use `https://api.tamaade.com/`:
- Sign-in SMS codes are real, paid Hubtel SMS - never request an OTP from automated tests.
- Hubtel checkouts take real money - never start one from automated tests.
Unit tests stay offline; `TamaadeApp.isRunningTests` keeps the host app from calling the API.

## Never commit secrets

- No API keys, tokens or passwords in Swift, plists or xcconfigs.
- `Config/Local.xcconfig`, signing certificates, provisioning profiles and App Store
  Connect keys (`*.p8`) are gitignored and must stay that way.
- Run `git status` and look at what is staged before every commit.

## Rules carried over from Android

- Tokens live in the Keychain (`SessionStore`); a 401 triggers one single-flight refresh
  (`api/user/token/refresh/`) and one retry, then the user is signed out. Network errors
  during refresh must not sign the user out.
- The basket is stored with a product snapshot and re-synced with the catalog after every
  products load.
- Payment success is only ever trusted from `api/user/payments/hubtel/status/`, never from
  the `result=` of the deep link.
- Error text comes from the server (`APIErrorParser`), with per-field messages under fields.

## iOS 16

Minimum iOS is 16.0. Models are `ObservableObject` (not `@Observable`), navigation is
`NavigationStack`. Swift language mode 5.
