import Foundation

/// Minimal JSON client for TamaadeAPI (URLSession + JSONSerialization, no dependencies).
/// Every call throws `APIError` with a user-facing message. Same endpoints and rules as the
/// Android app's `TamaadeApi` object.
final class TamaadeAPI {
    static let shared: TamaadeAPI = {
        let api = TamaadeAPI(baseURL: AppConfig.current.apiBase)
        // Unit tests never read or write the saved catalog.
        if !TamaadeApp.isRunningTests { api.responseCacheDir = defaultResponseCacheDir() }
        return api
    }()

    /// Server root ending in "/".
    let baseURL: String
    private let session: URLSession

    init(baseURL: String, session: URLSession? = nil) {
        self.baseURL = AppConfig.normalizedBase(baseURL) ?? baseURL
        if let session {
            self.session = session
        } else {
            let config = URLSessionConfiguration.default
            // The hosted backend (Render) can take a while to wake up, so be generous.
            config.timeoutIntervalForRequest = 60
            config.timeoutIntervalForResource = 90
            config.requestCachePolicy = .reloadIgnoringLocalCacheData
            self.session = URLSession(configuration: config)
        }
    }

    // MARK: - Auth

    func login(email: String, password: String) async throws -> AuthResult {
        let json = try await request("POST", "api/user/login/", body: ["email": email, "password": password])
        return try parseAuthResult(json, fallbackEmail: email)
    }

    /// Creates the account and signs it in straight away. At least one of `email` and
    /// `phoneNumber` is required; empty optional fields are left out of the body.
    func register(firstName: String, lastName: String, password: String,
                  email: String?, phoneNumber: String?) async throws -> AuthResult {
        var body: JSONObject = ["first_name": firstName, "last_name": lastName, "password": password]
        if let email, !email.isBlank { body["email"] = email }
        if let phoneNumber, !phoneNumber.isBlank { body["phone_number"] = phoneNumber }
        let json = try await request("POST", "api/user/register/", body: body)
        return try parseAuthResult(json, fallbackEmail: email ?? "", fallbackPhone: phoneNumber ?? "")
    }

    /// Sends a sign-in SMS code to a Ghana mobile number (0XXXXXXXXX or +233XXXXXXXXX).
    func requestOtp(phoneNumber: String) async throws -> OtpRequestResult {
        let json = try await request("POST", "api/user/otp/request/",
                                     body: ["phone_number": phoneNumber, "purpose": "login"])
        return parseOtpRequest(json, phoneNumber: phoneNumber)
    }

    /// Verifies a sign-in SMS code; the response signs the user in.
    func verifyOtp(phoneNumber: String, code: String) async throws -> AuthResult {
        let json = try await request("POST", "api/user/otp/verify/",
                                     body: ["phone_number": phoneNumber, "code": code, "purpose": "login"])
        return try parseAuthResult(json, fallbackPhone: phoneNumber)
    }

    /// Trades the refresh token for a new access token. A 401 means the refresh token is dead too.
    func refreshTokens(refreshToken: String) async throws -> RefreshedTokens {
        let json = try await request("POST", "api/user/token/refresh/", body: ["refresh": refreshToken])
        guard let access = json.string("access") else {
            throw APIError(message: APIError.sessionExpiredMessage, status: 401)
        }
        return RefreshedTokens(accessToken: access, refreshToken: json.string("refresh"))
    }

    /// Forgotten password: texts a reset code to the account's Ghana mobile number.
    func requestPasswordReset(phoneNumber: String) async throws -> OtpRequestResult {
        let json = try await request("POST", "api/user/password/reset/otp/request/",
                                     body: ["phone_number": phoneNumber])
        return parseOtpRequest(json, phoneNumber: phoneNumber)
    }

    /// Forgotten password: checks the SMS code, sets `newPassword` and signs the user in.
    func confirmPasswordReset(phoneNumber: String, code: String, newPassword: String) async throws -> AuthResult {
        let json = try await request("POST", "api/user/password/reset/otp/confirm/",
                                     body: ["phone_number": phoneNumber, "code": code, "new_password": newPassword])
        return try parseAuthResult(json, fallbackPhone: phoneNumber)
    }

    /// PATCH api/user/ - updates names and contact details and returns the updated user.
    /// An empty `email` or `phoneNumber` removes it (at least one must remain).
    func updateProfile(token: String, firstName: String, lastName: String,
                       email: String, phoneNumber: String) async throws -> UserSession {
        let json = try await request("PATCH", "api/user/", body: [
            "first_name": firstName,
            "last_name": lastName,
            "email": email,
            "phone_number": phoneNumber,
        ], token: token)
        // The endpoint returns the user object itself; also accept a {"user": {...}} wrapper.
        return Self.parseUser(json.object("user") ?? json)
    }

    /// Permanently deletes the signed-in account (App Store account-deletion requirement).
    func deleteAccount(token: String, password: String) async throws -> String {
        let json = try await request("POST", "api/user/delete-account/", body: ["password": password], token: token)
        return json.string("detail") ?? "Your account has been deleted."
    }

    // MARK: - Hubtel checkout

    func startHubtelCheckout(token: String, lines: [CartLine]) async throws -> HubtelCheckout {
        let items: [JSONObject] = lines.map {
            ["product": $0.product.id, "name": $0.product.name, "quantity": $0.quantity]
        }
        // "ios" makes the backend send Hubtel's return through its app-return page,
        // which redirects to tamaade://checkout/result.
        let json = try await request("POST", "api/user/payments/hubtel/checkout/",
                                     body: ["platform": "ios", "items": items], token: token)
        let directUrl = json.string("checkout_direct_url")
        guard let url = json.string("checkout_url") ?? directUrl,
              let reference = json.string("client_reference") else {
            throw APIError(message: "Hubtel checkout could not be started. Please try again.", status: 200)
        }
        return HubtelCheckout(
            checkoutUrl: url,
            checkoutDirectUrl: directUrl,
            checkoutId: json.string("checkout_id"),
            clientReference: reference,
            orderId: json.int("order_id"),
            amount: json.string("amount")
        )
    }

    func hubtelStatus(token: String, reference: String) async throws -> PaymentStatus {
        let json = try await request("GET", "api/user/payments/hubtel/status/?ref=" + Self.queryEncode(reference),
                                     token: token)
        return PaymentStatus(
            status: json.string("status") ?? "P",
            paid: json.bool("paid", default: false),
            orderId: json.int("order_id"),
            clientReference: json.string("client_reference") ?? reference
        )
    }

    // MARK: - Content

    func privacy() async throws -> PrivacyContent {
        let json = try await request("GET", "api/content/privacy/")
        return PrivacyContent(
            title: json.string("title") ?? "Privacy Policy",
            content: json.string("content") ?? "",
            updatedAt: json.string("updated_at")
        )
    }

    func backgroundMusic() async throws -> BackgroundMusicConfig {
        let json = try await request("GET", "api/content/background-music/")
        var track: MusicTrack?
        if let t = json.object("track"), let url = t.string("url") {
            let volume = Float(t.double("volume") ?? 1)
            track = MusicTrack(id: t.int("id"), title: t.string("title") ?? "", url: url,
                               volume: min(max(volume, 0), 1))
        }
        return BackgroundMusicConfig(enabled: json.bool("enabled", default: false), track: track)
    }

    // MARK: - Catalog (public, unpaginated JSON arrays)

    func products() async throws -> [Product] {
        try await requestList("api/products/").compactMap { self.parseProduct($0) }
    }

    func product(id: Int) async throws -> Product? {
        let json = try await request("GET", "api/products/\(id)/")
        return parseProduct(json)
    }

    func categories() async throws -> [ProductCategory] {
        try await requestList("api/products/categories/").compactMap { c in
            guard let id = c.int("id"), let name = c.string("name") else { return nil }
            return ProductCategory(id: id, name: name, icon: mediaURL(c.string("icon")))
        }
    }

    func banners() async throws -> [HeroBanner] {
        try await activeSorted(requestList("api/products/banners/")).compactMap { b in
            guard let image = mediaURL(b.string("image") ?? b.string("image_url")),
                  let id = b.int("id") else { return nil }
            return HeroBanner(id: id, title: b.string("title") ?? "", image: image, link: b.string("link"))
        }
    }

    func priceTiers() async throws -> [PriceTier] {
        let tiers = try await requestList("api/products/price-tiers/")
            .filter { $0.bool("is_active", default: true) }
            .sorted {
                let (o0, o1) = ($0.int("order", default: 0), $1.int("order", default: 0))
                if o0 != o1 { return o0 < o1 }
                return ($0.double("amount") ?? .nan) < ($1.double("amount") ?? .nan)
            }
        return tiers.compactMap { t in
            guard let amount = t.double("amount"), !amount.isNaN, let id = t.int("id") else { return nil }
            return PriceTier(id: id, amount: amount)
        }
    }

    func promos() async throws -> [StorefrontPromo] {
        try await activeSorted(requestList("api/products/promos/")).compactMap { p in
            guard let id = p.int("id") else { return nil }
            return StorefrontPromo(
                id: id,
                key: p.string("key") ?? "",
                title: p.string("title") ?? "",
                subtitle: p.string("subtitle") ?? "",
                highlight: p.string("highlight") ?? "",
                ctaLabel: p.string("cta_label") ?? "",
                link: p.string("link")
            )
        }
    }

    func merchTiles(placement: String) async throws -> [MerchTile] {
        try await activeSorted(requestList("api/products/merch-tiles/?placement=\(placement)")).compactMap { t in
            guard let id = t.int("id") else { return nil }
            return MerchTile(
                id: id,
                placement: t.string("placement") ?? placement,
                title: t.string("title") ?? "",
                image: mediaURL(t.string("image") ?? t.string("image_url")),
                link: t.string("link"),
                badge: t.string("badge") ?? "",
                highlight: t.bool("highlight", default: false)
            )
        }
    }

    func homeSections(location: String) async throws -> [HomeSection] {
        try await activeSorted(requestList("api/products/home-sections/?location=\(location)")).compactMap { h in
            guard let id = h.int("id") else { return nil }
            return HomeSection(
                id: id,
                key: h.string("key") ?? "",
                location: h.string("location") ?? location,
                title: h.string("title") ?? "",
                subtitle: h.string("subtitle") ?? "",
                ctaLabel: h.string("cta_label") ?? "",
                link: h.string("link"),
                productSource: h.string("product_source") ?? "featured"
            )
        }
    }

    /// Parses the api/products/ shape (also used for the basket saved on the device).
    func parseProduct(_ p: JSONObject) -> Product? {
        guard let id = p.int("id"), let name = p.string("name") else { return nil }
        let gallery = JSON.objects(p["images"])
            .enumerated()
            .sorted { a, b in
                let (pa, pb) = (a.element.bool("is_primary", default: false), b.element.bool("is_primary", default: false))
                if pa != pb { return pa }
                let (oa, ob) = (a.element.int("order", default: 0), b.element.int("order", default: 0))
                if oa != ob { return oa < ob }
                return a.offset < b.offset
            }
            .compactMap { mediaURL($0.element.string("url")) }
        return Product(
            id: id,
            name: name,
            desc: p.string("desc") ?? "",
            category: p.string("category") ?? "",
            seller: p.string("seller") ?? "",
            image: gallery.first ?? mediaURL(p.string("image")),
            images: gallery,
            price: p.string("price") ?? "0",
            compareAtPrice: p.string("compare_at_price"),
            discountPercent: p.int("discount_percent"),
            promoLabel: (p.string("promo_label") ?? "").trimmingCharacters(in: .whitespacesAndNewlines),
            brand: p.string("brand") ?? "",
            isNew: p.bool("is_new", default: false),
            isExpress: p.bool("is_express", default: false),
            saleEndsAt: p.string("sale_ends_at"),
            quantity: p.int("quantity", default: 0),
            createdAt: p.string("created_at")
        )
    }

    /// Same field names as the API, so `parseProduct` can read it back.
    static func productJSON(_ product: Product) -> JSONObject {
        let images: [JSONObject] = product.images.enumerated().map { i, url in
            ["url": url, "is_primary": i == 0, "order": i]
        }
        return [
            "id": product.id,
            "name": product.name,
            "desc": product.desc,
            "category": product.category,
            "seller": product.seller,
            "image": product.image ?? NSNull(),
            "images": images,
            "price": product.price,
            "compare_at_price": product.compareAtPrice ?? NSNull(),
            "discount_percent": product.discountPercent ?? NSNull(),
            "promo_label": product.promoLabel,
            "brand": product.brand,
            "is_new": product.isNew,
            "is_express": product.isExpress,
            "sale_ends_at": product.saleEndsAt ?? NSNull(),
            "quantity": product.quantity,
            "created_at": product.createdAt ?? NSNull(),
        ]
    }

    /// API-relative media paths become absolute URLs (same as TamaadeWeb resolveMediaUrl).
    func mediaURL(_ url: String?) -> String? {
        guard let url, !url.isBlank else { return nil }
        if url.hasPrefix("http://") || url.hasPrefix("https://") { return url }
        let root = baseURL.hasSuffix("/") ? String(baseURL.dropLast()) : baseURL
        return root + (url.hasPrefix("/") ? url : "/" + url)
    }

    // MARK: - Parsing helpers

    private func parseOtpRequest(_ json: JSONObject, phoneNumber: String) -> OtpRequestResult {
        OtpRequestResult(
            phoneNumber: json.string("phone_number") ?? phoneNumber,
            expiresIn: json.int("expires_in") ?? 600,
            resendIn: json.int("resend_in") ?? 60,
            message: json.string("detail") ?? "Code sent."
        )
    }

    private func parseAuthResult(_ json: JSONObject, fallbackEmail: String = "",
                                 fallbackPhone: String = "") throws -> AuthResult {
        guard let access = json.string("access") ?? json.string("access_token") ?? json.string("key") else {
            throw APIError(message: "Sign in failed. Please try again.", status: 200)
        }
        return AuthResult(
            accessToken: access,
            // OTP/register/reset send "refresh"; the e-mail login (dj-rest-auth) sends "refresh_token".
            refreshToken: json.string("refresh") ?? json.string("refresh_token"),
            user: Self.parseUser(json.object("user"), fallbackEmail: fallbackEmail, fallbackPhone: fallbackPhone)
        )
    }

    static func parseUser(_ user: JSONObject?, fallbackEmail: String = "", fallbackPhone: String = "") -> UserSession {
        UserSession(
            email: user?.string("email") ?? fallbackEmail,
            firstName: user?.string("first_name") ?? "",
            lastName: user?.string("last_name") ?? "",
            id: user?.int("pk") ?? user?.int("id"),
            phoneNumber: user?.string("phone_number") ?? fallbackPhone
        )
    }

    private func activeSorted(_ items: [JSONObject]) -> [JSONObject] {
        items.enumerated()
            .filter { $0.element.bool("is_active", default: true) }
            .sorted {
                let (a, b) = ($0.element.int("order", default: 0), $1.element.int("order", default: 0))
                return a != b ? a < b : $0.offset < $1.offset
            }
            .map { $0.element }
    }

    /// Percent-encodes a query value (keeps only unreserved characters).
    static func queryEncode(_ value: String) -> String {
        var allowed = CharacterSet.alphanumerics
        allowed.insert(charactersIn: "-._~")
        return value.addingPercentEncoding(withAllowedCharacters: allowed) ?? value
    }

    // MARK: - Transport

    private func request(_ method: String, _ path: String, body: JSONObject? = nil,
                         token: String? = nil) async throws -> JSONObject {
        (try await send(method, path, body: body, token: token) as? JSONObject) ?? [:]
    }

    /// For list endpoints; also accepts a DRF paginated {"results": [...]} body.
    /// Every successful catalog list is saved on the phone; inside `fromCache` the saved copy is
    /// read instead of the network, so the store opens instantly on the next launch.
    private func requestList(_ path: String) async throws -> [JSONObject] {
        let cacheFile = responseCacheDir?.appendingPathComponent(Self.cacheName(path))
        if Self.cacheOnly {
            guard let cacheFile, let data = try? Data(contentsOf: cacheFile) else {
                throw APIError(message: "No saved copy yet", status: 0)
            }
            return Self.toArray(JSON.parse(data))
        }
        let json = try await send("GET", path, body: nil, token: nil)
        let array = Self.toArray(json)
        if let cacheFile, json != nil, let data = try? JSONSerialization.data(withJSONObject: array) {
            // Atomic write: a crash never leaves half a file behind.
            try? data.write(to: cacheFile, options: .atomic)
        }
        return array
    }

    private static func toArray(_ json: Any?) -> [JSONObject] {
        if let array = json as? [Any] { return JSON.objects(array) }
        if let object = json as? JSONObject { return JSON.objects(object["results"]) }
        return []
    }

    // MARK: - Saved catalog

    /// Where catalog responses are saved (Application Support, which iOS doesn't purge).
    var responseCacheDir: URL?

    @TaskLocal private static var cacheOnly = false

    /// Runs catalog calls against the copies saved on the phone (no network).
    static func fromCache<T>(_ block: () async throws -> T) async rethrows -> T {
        try await $cacheOnly.withValue(true) { try await block() }
    }

    static func defaultResponseCacheDir() -> URL? {
        guard let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
        else { return nil }
        let dir = base.appendingPathComponent("catalog_cache", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    static func cacheName(_ path: String) -> String {
        path.replacingOccurrences(of: "[^A-Za-z0-9]+", with: "_", options: .regularExpression)
            .trimmingCharacters(in: CharacterSet(charactersIn: "_")) + ".json"
    }

    private func send(_ method: String, _ path: String, body: JSONObject?, token: String?) async throws -> Any? {
        guard let url = URL(string: baseURL + path) else { throw APIError.offline }
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let token { request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }
        if let body {
            request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
            request.httpBody = JSON.data(body)
        }

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            throw APIError.from(error)
        }
        let status = (response as? HTTPURLResponse)?.statusCode ?? 0
        let json = JSON.parse(data)
        guard (200...299).contains(status) else {
            let error = json as? JSONObject
            let header = (response as? HTTPURLResponse)?.value(forHTTPHeaderField: "Retry-After")
            throw APIError(
                message: APIErrorParser.message(body: json, status: status),
                status: status,
                code: error?.string("code"),
                retryAfter: error?.int("retry_after") ?? header.flatMap { Int($0.trimmingCharacters(in: .whitespaces)) },
                fieldErrors: APIErrorParser.fieldErrors(error?.object("errors"))
            )
        }
        return json
    }
}
