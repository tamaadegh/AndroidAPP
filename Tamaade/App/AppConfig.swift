import Foundation

/// Build-time configuration - the iOS counterpart of Android's `BuildConfig.API_BASE_URL`.
/// The value comes from the `API_BASE_URL` Info.plist key, filled in by `Config/*.xcconfig`.
struct AppConfig {
    /// Server root, always ending in `/` (e.g. `https://tamaadeapi-7it5.onrender.com/`).
    let apiBase: String

    static let current = AppConfig(bundle: .main)

    init(apiBase: String) {
        self.apiBase = apiBase
    }

    init(bundle: Bundle) {
        let raw = bundle.object(forInfoDictionaryKey: "API_BASE_URL") as? String
        guard let base = AppConfig.normalizedBase(raw) else {
            fatalError("API_BASE_URL missing or invalid in Info.plist - check Config/*.xcconfig")
        }
        self.apiBase = base
    }

    /// Adds the trailing slash that relative paths (`api/products/`) need.
    static func normalizedBase(_ raw: String?) -> String? {
        guard var value = raw?.trimmingCharacters(in: .whitespacesAndNewlines),
              !value.isEmpty, !value.contains("$(") else { return nil }
        while value.hasSuffix("/") { value.removeLast() }
        value += "/"
        guard let url = URL(string: value),
              let scheme = url.scheme, scheme == "http" || scheme == "https",
              url.host != nil else { return nil }
        return value
    }
}
