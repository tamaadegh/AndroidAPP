# Tamaade - iOS

Native SwiftUI app for Tamaade, online shopping in Ghana. This branch (`appleversion1`)
holds only the iOS app; the Android app lives on `leye-mobile` and stays the reference.
iOS 16+, iPhone only, portrait.

## Run

Open `Tamaade.xcodeproj`, pick an iPhone simulator, Run (⌘R).

Before the first run on a device or an archive: select the **Tamaade** target >
Signing & Capabilities and choose the Apple developer team (`DEVELOPMENT_TEAM` is left
empty in the project). The bundle id is `com.tamaade.ecommerce`, like on Android.

Debug and Release both use the **live API on Render**
(`https://api.tamaade.com/`). The server must run the TamaadeAPI branch
`leye` (OTP, Hubtel, delete-account, token refresh, SMS password reset).
To use a backend running on the Mac, copy `Config/Local.xcconfig.example` to
`Config/Local.xcconfig` (gitignored) and set `API_BASE_URL`.

## Tests

```sh
xcodebuild -project Tamaade.xcodeproj -scheme Tamaade \
  -destination 'platform=iOS Simulator,name=iPhone 16' test
```

Unit tests cover the pure logic (links, phone numbers, prices, product parsing, error
messages, privacy text) and never touch the network; the app skips its startup API calls
while it hosts the tests.

## What to check by hand

- Sign in with an SMS code (real Hubtel SMS), with e-mail + password, sign up, forgot password.
- Basket survives a relaunch and is re-synced with the catalog (removed items, new prices).
- Checkout: Hubtel opens in an in-app Safari sheet; after paying, the backend's return page
  redirects to `tamaade://checkout/result` and the basket shows "Payment successful".
  Closing the sheet without paying leaves "I've paid — check" / "Dismiss".
- Background music (Profile > Settings) plays only while the app is open and respects
  the silent switch.
- Profile > Delete account.

## Layout

| Folder | Contents |
|--------|----------|
| `Tamaade/App` | App entry, root view (tabs, splash, toasts, sign-in sheet), router, Hubtel sheet |
| `Tamaade/Core/Network` | API client, lenient JSON helpers, error messages |
| `Tamaade/Core/Models` | Products, catalog content, session, checkout state |
| `Tamaade/Core/Session` | Keychain tokens, basket and settings storage |
| `Tamaade/Core/Store` | `StoreModel` (basket, auth, checkout), `CatalogModel`, link resolver, formatting |
| `Tamaade/Core/Audio` | Background music |
| `Tamaade/Features/*` | Home, Categories, Deals, Product, Cart, Profile, Auth, Privacy |
| `Tamaade/UI` | Colours, fonts, shared components |
| `TamaadeTests` | Unit tests |
| `Config` | xcconfigs (API URL) and Info.plist |

Kotlin -> Swift mapping and decisions: `docs/IOS_PORT_PLAN.md`.
