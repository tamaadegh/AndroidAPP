import Foundation

/// Storage for everything that must survive an app restart: the signed-in session (tokens in
/// the Keychain), the basket, an in-flight Hubtel checkout and user settings (UserDefaults).
final class SessionStore {
    private let defaults: UserDefaults
    private let keychain: Keychain
    private let api: TamaadeAPI

    private enum Key {
        static let access = "access_token"
        static let refresh = "refresh_token"
        static let user = "user"
        static let pendingRef = "pending_checkout_ref"
        static let music = "background_music_enabled"
        static let cart = "cart"
        static let installed = "installed"
    }

    init(defaults: UserDefaults = .standard,
         keychain: Keychain = Keychain(service: "com.tamaade.ecommerce.session"),
         api: TamaadeAPI = .shared) {
        self.defaults = defaults
        self.keychain = keychain
        self.api = api
        // Keychain items outlive an uninstall; a fresh install must not inherit an old session.
        if !defaults.bool(forKey: Key.installed) {
            keychain.set(nil, for: Key.access)
            keychain.set(nil, for: Key.refresh)
            defaults.set(true, forKey: Key.installed)
        }
    }

    var accessToken: String? { keychain.string(Key.access) }

    var refreshToken: String? { keychain.string(Key.refresh) }

    /// Stores a refreshed access token (and the new refresh token when the server rotated it).
    func updateTokens(accessToken: String, refreshToken: String?) {
        keychain.set(accessToken, for: Key.access)
        if let refreshToken { keychain.set(refreshToken, for: Key.refresh) }
    }

    func loadUser() -> UserSession? {
        guard accessToken != nil,
              let data = defaults.data(forKey: Key.user),
              let json = JSON.parse(data) as? JSONObject else { return nil }
        let email = json.string("email") ?? ""
        let phone = json.string("phone_number") ?? ""
        if email.isEmpty && phone.isEmpty { return nil }
        return UserSession(
            email: email,
            firstName: json.string("first_name") ?? "",
            lastName: json.string("last_name") ?? "",
            id: json.int("id"),
            phoneNumber: phone
        )
    }

    func saveSession(accessToken: String, refreshToken: String?, user: UserSession) {
        let json: JSONObject = [
            "email": user.email,
            "first_name": user.firstName,
            "last_name": user.lastName,
            "id": user.id ?? NSNull(),
            "phone_number": user.phoneNumber,
        ]
        keychain.set(accessToken, for: Key.access)
        keychain.set(refreshToken, for: Key.refresh)
        defaults.set(JSON.data(json), forKey: Key.user)
    }

    /// Replaces the stored user (e.g. after editing details), keeping the current tokens.
    func saveUser(_ user: UserSession) {
        guard let token = accessToken else { return }
        saveSession(accessToken: token, refreshToken: refreshToken, user: user)
    }

    func clearSession() {
        keychain.set(nil, for: Key.access)
        keychain.set(nil, for: Key.refresh)
        defaults.removeObject(forKey: Key.user)
        defaults.removeObject(forKey: Key.pendingRef)
    }

    /// client_reference of a Hubtel checkout opened but not yet confirmed.
    var pendingCheckoutRef: String? {
        get { defaults.string(forKey: Key.pendingRef) }
        set {
            if let newValue { defaults.set(newValue, forKey: Key.pendingRef) }
            else { defaults.removeObject(forKey: Key.pendingRef) }
        }
    }

    var musicEnabled: Bool {
        get { defaults.object(forKey: Key.music) as? Bool ?? true }
        set { defaults.set(newValue, forKey: Key.music) }
    }

    /// Basket with a snapshot of each product (API field names), so it shows offline;
    /// it is re-synced with the live catalog once products load.
    func saveCart(_ lines: [CartLine]) {
        let array: [JSONObject] = lines.map {
            ["product": TamaadeAPI.productJSON($0.product), "qty": $0.quantity]
        }
        defaults.set(try? JSONSerialization.data(withJSONObject: array), forKey: Key.cart)
    }

    func loadCart() -> [CartLine] {
        guard let data = defaults.data(forKey: Key.cart) else { return [] }
        return JSON.objects(JSON.parse(data)).compactMap { item in
            guard let raw = item.object("product"), let product = api.parseProduct(raw) else { return nil }
            let qty = item.int("qty", default: 0)
            return qty > 0 ? CartLine(product: product, quantity: qty) : nil
        }
    }
}
