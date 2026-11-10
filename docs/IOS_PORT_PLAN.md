# iOS port of the Tamaade Android app

Source: `leye-mobile` @ 7d00130 (Compose app, ~6.7k lines of Kotlin). Target: SwiftUI, iOS 16+.
Every screen and rule of the Android app is ported; nothing was added on the backend.

## File mapping

| Android (Kotlin) | iOS (Swift) |
|------------------|-------------|
| `data/api/TamaadeApi.kt` | `Core/Network/TamaadeAPI.swift`, `APIError.swift`, `JSON.swift` |
| `data/model/Models.kt` | `Core/Models/Models.swift` |
| `data/SessionStore.kt` (SharedPreferences) | `Core/Session/SessionStore.swift` (Keychain + UserDefaults) |
| `data/StoreViewModel.kt` | `Core/Store/StoreModel.swift` |
| `data/CatalogViewModel.kt` | `Core/Store/CatalogModel.swift` |
| `data/BackgroundMusicPlayer.kt` (MediaPlayer) | `Core/Audio/BackgroundMusicPlayer.swift` (AVQueuePlayer + AVPlayerLooper) |
| `ui/navigation/StoreLink.kt`, `Routes.kt` | `Core/Store/StoreLink.swift`, `App/Router.swift` |
| `ui/util/PriceFormat.kt`, `ui/auth/PhoneAuth.kt` (helpers) | `Core/Store/Formatting.swift` |
| `MainActivity.kt`, `TamaadeApp.kt` | `App/TamaadeApp.swift`, `App/RootView.swift` |
| `ui/theme/*` | `UI/Theme.swift` |
| `ui/components/*` | `UI/Components/*` |
| `ui/home`, `categories`, `deals`, `product`, `cart`, `profile`, `privacy`, `auth` | `Features/*` (same names) |

## Decisions

- **Navigation**: one `NavigationStack` per tab (Home, Category, Deals, Cart, Me), like the
  Compose bottom bar with saved state. Login / Sign up / Forgot password open as a sheet
  instead of full-screen routes; "Continue as guest" and the close button dismiss it.
- **Hubtel checkout**: Android opens the browser and gets `tamaade://checkout/result` back
  through an intent filter. iOS opens the page in `ASWebAuthenticationSession` with the
  `tamaade` callback scheme, which catches the backend's redirect. If the sheet is closed
  without it, the pending payment is checked once (same as Android's `onResume`). The
  `tamaade` URL scheme is also registered, and Safari is used as a fallback.
- **Tokens** move from SharedPreferences to the Keychain; a fresh install clears any
  Keychain items left from a previous install.
- **Background music** uses the `.ambient` audio session: it mixes with other audio,
  obeys the silent switch and stops in the background.
- **Snackbars** become a toast above the tab bar.
- **Debug API**: Android debug builds use the emulator's local server; iOS Debug uses the
  Render API by default (local server via `Config/Local.xcconfig`).
- Light appearance only, like the Android theme.

## Still to do on a Mac

1. Set the development team, build, fix any compiler errors (the port was written without Xcode).
2. Run the unit tests and the manual checklist in `README.md` against the Render API.
3. App Store Connect: create the app (`com.tamaade.ecommerce`), privacy labels (name, e-mail,
   phone, purchase history), screenshots, then archive and upload.
